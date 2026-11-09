package com.tontoque28.npcdirector.waypoint;

import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.quest.QuestManager;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

public class NpcPatrolGoal extends EntityAIBase {

	private final QuestNpcEntity npc;

	public NpcPatrolGoal(QuestNpcEntity npc) {
		this.npc = npc;
		this.setMutexBits(1);
	}

	private WaypointPath resolvePath() {
		String pathId = npc.getProfile().getAssignedPathId();
		if (pathId == null || pathId.isEmpty()) {
			return null;
		}
		return WaypointPathManager.get(pathId);
	}

	@Override
	public boolean shouldExecute() {
		WaypointPath path = resolvePath();
		return path != null && !path.getPoints().isEmpty();
	}

	@Override
	public boolean shouldContinueExecuting() {
		return shouldExecute();
	}

	@Override
	public void updateTask() {
		WaypointPath path = resolvePath();
		if (path == null || path.getPoints().isEmpty()) {
			return;
		}

		int index = Math.min(npc.getPatrolIndex(), path.getPoints().size() - 1);
		Waypoint waypoint = path.getPoints().get(index);

		int idleTicksRemaining = npc.getPatrolIdleTicks();
		if (idleTicksRemaining > 0) {
			idleTicksRemaining--;
			npc.setPatrolIdleTicks(idleTicksRemaining);
			if (idleTicksRemaining == 0) {
				applyTrigger(waypoint.hasDepartTrigger(), waypoint.getOnDepartQuestId(), waypoint.getOnDepartState());
				npc.setPatrolIndex(path.nextIndex(index));
				npc.setPatrolIdleTicks(-1);
			}
			return;
		}

		BlockPos target = waypoint.getPosition();
		double distanceSquared = npc.getDistanceSq(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);

		if (distanceSquared > 2.25) {
			if (npc.getNavigator().noPath()) {
				npc.getNavigator().tryMoveToXYZ(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.6);
			}
		} else {
			npc.getNavigator().clearPath();
			applyTrigger(waypoint.hasArriveTrigger(), waypoint.getOnArriveQuestId(), waypoint.getOnArriveState());
			npc.setPatrolIdleTicks(Math.max(waypoint.getWaitTicks(), 1));
		}
	}

	private void applyTrigger(boolean hasTrigger, String questId, String state) {
		if (!hasTrigger) {
			return;
		}
		World world = npc.world;
		if (world instanceof WorldServer) {
			QuestManager.get((WorldServer) world).setGlobalState(questId, state);
		}
	}
}
