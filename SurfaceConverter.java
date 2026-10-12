package com.wheatworld;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

/** Turns the top of every column in a chunk into wet farmland + mature wheat, keeping vanilla terrain shape. */
public final class SurfaceConverter {
    private static final BlockState FARMLAND = Blocks.FARMLAND.getDefaultState()
            .with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE);
    private static final BlockState WHEAT = Blocks.WHEAT.getDefaultState().with(Properties.AGE_7, 7);
    private static final int FLAGS = Block.NOTIFY_LISTENERS; // clients get synced, no neighbor update cascade

    private SurfaceConverter() {}

    public static void convert(ServerWorld world, WorldChunk chunk, int chunkX, int chunkZ) {
        Heightmap surface = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        int minY = world.getBottomY();
        int maxY = minY + world.getHeight(); // exclusive
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = baseX + lx;
                int z = baseZ + lz;
                int y = Math.min(surface.get(lx, lz), maxY) - 1;
                boolean underwater = false;
                boolean found = false;

                for (; y >= minY; y--) {
                    pos.set(x, y, z);
                    BlockState s = world.getBlockState(pos);
                    if (s.isAir()) continue;
                    if (s.isOf(Blocks.WATER) || s.isOf(Blocks.BUBBLE_COLUMN)) {
                        underwater = true;
                        continue;
                    }
                    if (!s.isOf(Blocks.LAVA) && isClutter(world, pos, s)) {
                        // Remove trees, plants, snow layers, kelp... keep water where the block was waterlogged.
                        if (s.getFluidState().isOf(Fluids.WATER)) underwater = true;
                        world.setBlockState(pos, s.getFluidState().getBlockState(), FLAGS);
                        continue;
                    }
                    found = true;
                    break;
                }
                if (!found) continue;

                pos.set(x, y, z);
                world.setBlockState(pos, FARMLAND, FLAGS);
                // Wheat cannot exist inside water, so seabeds get farmland only.
                if (!underwater && y + 1 < maxY) {
                    pos.set(x, y + 1, z);
                    world.setBlockState(pos, WHEAT, FLAGS);
                }
            }
        }
    }

    /** Things that sit on top of the ground and should be cleared rather than turned into farmland. */
    private static boolean isClutter(ServerWorld world, BlockPos pos, BlockState s) {
        return s.isIn(BlockTags.LEAVES)
                || s.isIn(BlockTags.LOGS)
                || s.isReplaceable()
                || s.getCollisionShape(world, pos).isEmpty() // flowers, grass, vines, kelp, sugar cane, torches...
                || s.isOf(Blocks.SNOW)
                || s.isOf(Blocks.CACTUS)
                || s.isOf(Blocks.PUMPKIN)
                || s.isOf(Blocks.MELON)
                || s.isOf(Blocks.MUSHROOM_STEM)
                || s.isOf(Blocks.RED_MUSHROOM_BLOCK)
                || s.isOf(Blocks.BROWN_MUSHROOM_BLOCK)
                || s.isOf(Blocks.MANGROVE_ROOTS)
                || s.isOf(Blocks.MOSS_CARPET)
                || s.isOf(Blocks.BEE_NEST);
    }
}
