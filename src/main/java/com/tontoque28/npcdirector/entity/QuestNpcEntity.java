package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.waypoint.NpcPatrolGoal;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

public class QuestNpcEntity extends CreatureEntity {

	private NpcProfile profile = new NpcProfile();
	private int patrolIndex = 0;
	private int patrolIdleTicks = -1;

	public QuestNpcEntity(EntityType<? extends CreatureEntity> entityType, World world) {
		super(entityType, world);
		this.setPersistenceRequired();
	}

	public static AttributeModifierMap.MutableAttribute createAttributes() {
		return CreatureEntity.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new LookAtGoal(this, PlayerEntity.class, 8.0F));
		this.goalSelector.addGoal(1, new NpcPatrolGoal(this));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomWalkingGoal(this, 0.6));
		this.goalSelector.addGoal(3, new LookRandomlyGoal(this));
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
	protected ActionResultType mobInteract(PlayerEntity player, Hand hand) {
		if (!this.level.isClientSide) {
			if (player instanceof ServerPlayerEntity) {
				ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
				if (serverPlayer.isShiftKeyDown() && serverPlayer.hasPermissions(2)) {
					AdminService.openFor(serverPlayer, this);
					return ActionResultType.SUCCESS;
				}
				DialogueNode node = DialogueInteractionHandler.open(serverPlayer, this);
				NetworkingInit.sendNodeOrClose(serverPlayer, node);
			}
			return ActionResultType.SUCCESS;
		}
		return ActionResultType.CONSUME;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!profile.isTargetableByHostiles() && source.getEntity() instanceof IMob) {
			return false;
		}
		return super.hurt(source, amount);
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
		String prefix = "";
		switch (profile.getQuestRelevance()) {
			case MAIN:
				prefix = "★ ";
				break;
			case INDIRECT:
				prefix = "◆ ";
				break;
			default:
				break;
		}
		String name = profile.getDisplayName();
		if (name.isEmpty() && prefix.isEmpty()) {
			this.setCustomName(null);
			this.setCustomNameVisible(false);
			return;
		}
		this.setCustomName(new StringTextComponent(prefix + (name.isEmpty() ? "NPC" : name)));
		this.setCustomNameVisible(true);
	}

	@Override
	public void addAdditionalSaveData(CompoundNBT output) {
		super.addAdditionalSaveData(output);
		CompoundNBT profileTag = new CompoundNBT();
		profile.write(profileTag);
		output.put("NpcProfile", profileTag);
		output.putInt("PatrolIndex", patrolIndex);
		output.putInt("PatrolIdleTicks", patrolIdleTicks);
	}

	@Override
	public void readAdditionalSaveData(CompoundNBT input) {
		super.readAdditionalSaveData(input);
		this.setPersistenceRequired();
		if (input.contains("NpcProfile", 10)) {
			this.profile = NpcProfile.read(input.getCompound("NpcProfile"));
			if (!profile.getDisplayName().isEmpty() || profile.getQuestRelevance() != QuestRelevance.NONE) {
				refreshName();
			}
		}
		this.patrolIndex = input.contains("PatrolIndex") ? input.getInt("PatrolIndex") : 0;
		this.patrolIdleTicks = input.contains("PatrolIdleTicks") ? input.getInt("PatrolIdleTicks") : -1;
	}
}
