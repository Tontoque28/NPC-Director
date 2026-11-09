package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.waypoint.NpcPatrolGoal;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class QuestNpcEntity extends PathfinderMob {

	private NpcProfile profile = new NpcProfile();
	private int patrolIndex = 0;
	private int patrolIdleTicks = -1;

	public QuestNpcEntity(EntityType<? extends PathfinderMob> entityType, Level world) {
		super(entityType, world);
		this.setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(1, new NpcPatrolGoal(this));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
	}

	public int getPatrolIndex() {
		return patrolIndex;
	}

	public void setPatrolIndex(int patrolIndex) {
		this.patrolIndex = patrolIndex;
	}

	public int getPatrolIdleTicks() {
		return patrolIdleTicks;
	}

	public void setPatrolIdleTicks(int patrolIdleTicks) {
		this.patrolIdleTicks = patrolIdleTicks;
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.level().isClientSide()) {
			if (player instanceof ServerPlayer serverPlayer) {
				if (serverPlayer.isShiftKeyDown() && AdminService.isOperator(serverPlayer)) {
					AdminService.openFor(serverPlayer, this);
					return InteractionResult.SUCCESS;
				}
				DialogueNode node = DialogueInteractionHandler.open(serverPlayer, this);
				NetworkingInit.sendNodeOrClose(serverPlayer, node);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (!profile.isTargetableByHostiles() && source.getEntity() instanceof Enemy) {
			return false;
		}
		return super.hurtServer(level, source, amount);
	}

	public NpcProfile getProfile() {
		return profile;
	}

	public void setProfile(NpcProfile profile) {
		this.profile = profile;
		this.setPersistenceRequired();
		refreshName();
	}

	private void refreshName() {
		String prefix = switch (profile.getQuestRelevance()) {
			case MAIN -> "★ ";
			case INDIRECT -> "◆ ";
			default -> "";
		};
		String name = profile.getDisplayName();
		if (name.isEmpty() && prefix.isEmpty()) {
			this.setCustomName(null);
			this.setCustomNameVisible(false);
			return;
		}
		this.setCustomName(Component.literal(prefix + (name.isEmpty() ? "NPC" : name)));
		this.setCustomNameVisible(true);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		profile.write(output.child("NpcProfile"));
		output.putInt("PatrolIndex", patrolIndex);
		output.putInt("PatrolIdleTicks", patrolIdleTicks);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.child("NpcProfile").ifPresent(child -> {
			this.profile = NpcProfile.read(child);
			this.setPersistenceRequired();
			if (!profile.getDisplayName().isEmpty() || profile.getQuestRelevance() != QuestRelevance.NONE) {
				refreshName();
			}
		});
		this.patrolIndex = input.getIntOr("PatrolIndex", 0);
		this.patrolIdleTicks = input.getIntOr("PatrolIdleTicks", -1);
	}
}
