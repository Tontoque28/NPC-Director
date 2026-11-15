package com.tontoque28.npcdirector.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SeatHelper {

	public static final double STEP_UP = 0.55D;
	public static final double SEARCH_DOWN = 1.5D;

	private SeatHelper() {
	}

	public static double surface(Level level, double x, double y, double z) {
		int blockX = (int) Math.floor(x);
		int blockZ = (int) Math.floor(z);
		double localX = x - blockX;
		double localZ = z - blockZ;
		double best = Double.NaN;
		int top = (int) Math.floor(y + STEP_UP);
		int bottom = (int) Math.floor(y - SEARCH_DOWN);
		for (int by = top; by >= bottom; by--) {
			BlockPos pos = new BlockPos(blockX, by, blockZ);
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(level, pos);
			if (shape.isEmpty()) {
				continue;
			}
			for (AABB box : shape.toAabbs()) {
				if (localX >= box.minX - 1.0E-6D && localX <= box.maxX + 1.0E-6D && localZ >= box.minZ - 1.0E-6D && localZ <= box.maxZ + 1.0E-6D) {
					double height = by + box.maxY;
					if (height <= y + STEP_UP && (Double.isNaN(best) || height > best)) {
						best = height;
					}
				}
			}
			if (!Double.isNaN(best)) {
				return best;
			}
		}
		return best;
	}
}
