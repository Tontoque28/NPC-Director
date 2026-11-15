package com.tontoque28.npcdirector.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SeatHelper {

	public static final double STEP_UP = 0.55D;
	public static final double SEARCH_DOWN = 1.5D;
	public static final double SIT_DROP = 0.569D;

	private SeatHelper() {
	}

	public static double surface(World world, double x, double y, double z) {
		int blockX = (int) Math.floor(x);
		int blockZ = (int) Math.floor(z);
		double best = Double.NaN;
		int top = (int) Math.floor(y + STEP_UP);
		int bottom = (int) Math.floor(y - SEARCH_DOWN);
		for (int by = top; by >= bottom; by--) {
			BlockPos pos = new BlockPos(blockX, by, blockZ);
			IBlockState state = world.getBlockState(pos);
			List<AxisAlignedBB> boxes = new ArrayList<>();
			state.addCollisionBoxToList(world, pos, new AxisAlignedBB(pos), boxes, null, true);
			for (AxisAlignedBB box : boxes) {
				if (x >= box.minX - 1.0E-6D && x <= box.maxX + 1.0E-6D && z >= box.minZ - 1.0E-6D && z <= box.maxZ + 1.0E-6D) {
					double height = box.maxY;
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
