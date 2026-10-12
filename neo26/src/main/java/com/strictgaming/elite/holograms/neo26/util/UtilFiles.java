package com.strictgaming.elite.holograms.neo26.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Crash-safe reading and writing of the JSON stores.
 *
 * <p>Every hologram the server knows about lives in one of two small JSON files, so a write that
 * is interrupted part way (a crash, a kill, a power cut) must never leave a file that is
 * missing or half written, and a file that cannot be read must never be mistaken for one that
 * is empty. The second matters as much as the first: an unreadable store that loads as "no
 * holograms" is overwritten with nothing by the very next save.
 */
public final class UtilFiles {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private UtilFiles() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }

    /**
     * Replaces a file's contents so that at every instant it holds either the complete old
     * version or the complete new one.
     *
     * <p>The new content is written and flushed to disk in a temporary file first, then moved
     * over the original in one step. The previous version is kept alongside as {@code .bak},
     * which {@link #readJson} falls back to if the main file is ever found damaged.
     *
     * @param target  the file to replace
     * @param content the complete new contents
     * @throws IOException if the new contents could not be written at all, in which case the
     *                     original file is untouched
     */
    public static synchronized void writeAtomically(Path target, String content) throws IOException {
        Path parent = target.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temp = sibling(target, ".tmp");

        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            ByteBuffer buffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

            // Without this the rename can reach the disk before the data does, and a power cut
            // leaves a file of the right name full of zeroes.
            channel.force(true);
        }

        if (Files.exists(target)) {
            try {
                Files.copy(target, sibling(target, ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // A stale backup is not worth failing the save over.
                LOGGER.warn("Could not refresh the backup of {}: {}", target.getFileName(), e.getMessage());
            }
        }

        replace(temp, target);
    }

    /**
     * Moves the finished temporary file over the target.
     *
     * <p>Windows refuses to replace a file another process has open (an editor, a virus
     * scanner, a backup tool), which is usually momentary, so the move is retried briefly
     * before falling back to a copy.
     */
    private static void replace(Path temp, Path target) throws IOException {
        IOException lastFailure = null;

        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (IOException e) {
                lastFailure = e;

                try {
                    Thread.sleep(50L * (attempt + 1));
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Still locked. The temporary file holds the complete new content, so copying it across
        // is not atomic but cannot lose anything we meant to write.
        LOGGER.warn("Could not replace {} in one step ({}), copying instead",
                target.getFileName(), lastFailure == null ? "interrupted" : lastFailure.getMessage());
        Files.copy(temp, target, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(temp);
    }

    /**
     * Reads a JSON store, recovering from a damaged file rather than treating it as empty.
     *
     * <p>A missing, blank or unparseable main file falls back to the {@code .bak} kept by
     * {@link #writeAtomically}. An unparseable file is first copied aside as
     * {@code .corrupt-<timestamp>}, so whatever it held survives the next save. A file that
     * parses is always used as-is, including an empty list, which is what a deliberately
     * emptied store looks like.
     *
     * @param file the store to read
     * @param what a short description for the log, such as "holograms"
     * @return the parsed contents, or {@code null} when neither the file nor its backup holds any
     */
    public static JsonElement readJson(Path file, String what) {
        JsonElement primary = tryRead(file, what);

        if (primary != null) {
            return primary;
        }

        Path backup = sibling(file, ".bak");

        if (Files.exists(backup)) {
            JsonElement restored = tryRead(backup, what);

            if (restored != null) {
                LOGGER.warn("Restored {} from the backup {}", what, backup.getFileName());
                return restored;
            }
        }

        return null;
    }

    private static JsonElement tryRead(Path file, String what) {
        if (!Files.exists(file)) {
            return null;
        }

        String text;

        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Could not read {} from {}: {}", what, file.getFileName(), e.getMessage());
            return null;
        }

        if (text.isBlank()) {
            LOGGER.warn("{} is empty", file.getFileName());
            return null;
        }

        try {
            return JsonParser.parseString(text);
        } catch (JsonParseException | IllegalStateException e) {
            LOGGER.error("{} is damaged and could not be read ({})", file.getFileName(), e.getMessage());
            quarantine(file, what);
            return null;
        }
    }

    /**
     * Copies a file that could not be used aside under a timestamped name, so the next save
     * cannot destroy it and it can be repaired by hand.
     *
     * @param file the file to preserve
     * @param what a short description for the log
     */
    public static void quarantine(Path file, String what) {
        Path copy = sibling(file, ".corrupt-" + LocalDateTime.now().format(STAMP));

        try {
            Files.copy(file, copy, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.error("The unreadable {} file has been kept as {}, so nothing in it has been lost",
                    what, copy.getFileName());
        } catch (IOException e) {
            LOGGER.error("Could not keep a copy of the unreadable {} file: {}", what, e.getMessage());
        }
    }

    /**
     * Returns when a file was last changed, or {@code null} if it does not exist.
     *
     * <p>Used to tell whether a store was edited by hand since the mod last wrote it.
     *
     * @param file the file to inspect
     * @return its modification time, or null
     */
    public static FileTime lastModified(Path file) {
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private static Path sibling(Path file, String suffix) {
        return file.resolveSibling(file.getFileName().toString() + suffix);
    }
}
