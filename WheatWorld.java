package com.wheatworld;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WheatWorld implements ModInitializer {
    public static final String MOD_ID = "wheatworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final long BUDGET_NANOS = 5_000_000L; // max ~5ms of conversion work per tick
    private static final int AUTOSAVE_TICKS = 1200;

    private static final ProcessedChunks PROCESSED = new ProcessedChunks();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server ->
                PROCESSED.load(server.getSavePath(WorldSavePath.ROOT).resolve("wheatworld_processed.bin")));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> PROCESSED.save());
        ServerTickEvents.END_WORLD_TICK.register(WheatWorld::tick);
    }

    /**
     * Converts loaded-but-unconverted chunks around each player, nearest first. This covers freshly
     * generated chunks and chunks that already existed before the mod was installed.
     */
    private static void tick(ServerWorld world) {
        if (world.getRegistryKey() != World.OVERWORLD) return;

        if (world.getTime() % AUTOSAVE_TICKS == 0 && PROCESSED.isDirty()) {
            PROCESSED.save();
        }
        if (world.getPlayers().isEmpty()) return;

        int radius = world.getServer().getPlayerManager().getViewDistance();
        ServerChunkManager chunks = world.getChunkManager();
        long deadline = System.nanoTime() + BUDGET_NANOS;

        for (ServerPlayerEntity player : world.getPlayers()) {
            int pcx = player.getBlockX() >> 4;
            int pcz = player.getBlockZ() >> 4;
            for (int r = 0; r <= radius; r++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (r == 0 || Math.abs(dx) == r) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (visit(world, chunks, pcx + dx, pcz + dz, deadline)) return;
                        }
                    } else {
                        if (visit(world, chunks, pcx + dx, pcz - r, deadline)) return;
                        if (visit(world, chunks, pcx + dx, pcz + r, deadline)) return;
                    }
                }
            }
        }
    }

    /** @return true when the per-tick time budget is used up */
    private static boolean visit(ServerWorld world, ServerChunkManager chunks, int cx, int cz, long deadline) {
        long key = (long) cx & 0xFFFFFFFFL | ((long) cz & 0xFFFFFFFFL) << 32;
        if (PROCESSED.contains(key)) return false;
        WorldChunk chunk = chunks.getWorldChunk(cx, cz); // null if not fully loaded; never forces generation
        if (chunk == null) return false;
        SurfaceConverter.convert(world, chunk, cx, cz);
        PROCESSED.add(key);
        return System.nanoTime() > deadline;
    }
}
