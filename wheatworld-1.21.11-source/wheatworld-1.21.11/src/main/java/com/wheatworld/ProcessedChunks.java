package com.wheatworld;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Remembers which chunks were already converted (stored in the world save folder),
 * so player edits are never overwritten on later loads.
 */
public final class ProcessedChunks {
    private final LongOpenHashSet done = new LongOpenHashSet();
    private Path file;
    private boolean dirty;

    public synchronized void load(Path path) {
        this.file = path;
        done.clear();
        dirty = false;
        if (!Files.exists(path)) return;
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int n = in.readInt();
            for (int i = 0; i < n; i++) done.add(in.readLong());
        } catch (IOException e) {
            WheatWorld.LOGGER.error("Could not read processed chunk list, starting from what was read", e);
        }
    }

    public synchronized boolean contains(long chunkKey) {
        return done.contains(chunkKey);
    }

    public synchronized void add(long chunkKey) {
        if (done.add(chunkKey)) dirty = true;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized void save() {
        if (file == null || !dirty) return;
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(done.size());
                for (long l : done) out.writeLong(l);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            dirty = false;
        } catch (IOException e) {
            WheatWorld.LOGGER.error("Could not save processed chunk list", e);
        }
    }
}
