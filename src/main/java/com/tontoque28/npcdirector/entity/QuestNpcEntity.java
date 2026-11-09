package com.tontoque28.npcdirector.entity;

import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.waypoint.NpcPatrolGoal;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class QuestNpcEntity extends EntityCreature {

	private NpcProfile profile = new NpcProfile();
	private int patrolIndex = 0;
	private int patrolIdleTicks = -1;

	public QuestNpcEntity(World world) {
		super(world);
		this.setSize(0.6F, 1.95F);
		this.enablePersistence();
	}

	@Override
	protected void applyEntityAttributes() {
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25);
		this.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE).setBaseValue(16.0);
	}

	@Override
	protected void initEntityAI() {
		this.tasks.addTask(0, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
		this.tasks.addTask(1, new NpcPatrolGoal(this));
		this.tasks.addTask(2, new EntityAIWanderAvoidWater(this, 0.6));
		this.tasks.addTask(3, new EntityAILookIdle(this));
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
	protected boolean processInteract(EntityPlayer player, EnumHand hand) {
		if (!this.world.isRemote) {
			if (player instanceof EntityPlayerMP) {
				EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
				if (hand != EnumHand.MAIN_HAND) {
					return true;
				}
				if (serverPlayer.isSneaking() && serverPlayer.canUseCommand(2, "")) {
					AdminService.openFor(serverPlayer, this);
					return true;
				}
				DialogueNode node = DialogueInteractionHandler.open(serverPlayer, this);
				NetworkingInit.sendNodeOrClose(serverPlayer, node);
			}
		}
		return true;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (!profile.isTargetableByHostiles() && source.getTrueSource() instanceof IMob) {
			return false;
		}
		return super.attackEntityFrom(source, amount);
	}

	public NpcProfile getProfile() {
		return profile;
	}

	public void setProfile(NpcProfile profile) {
		this.profile = profile;
		this.enablePersistence();
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
			this.setCustomNameTag("");
			this.setAlwaysRenderNameTag(false);
			return;
		}
		this.setCustomNameTag(prefix + (name.isEmpty() ? "NPC" : name));
		this.setAlwaysRenderNameTag(true);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound output) {
		super.writeEntityToNBT(output);
		NBTTagCompound profileTag = new NBTTagCompound();
		profile.write(profileTag);
		output.setTag("NpcProfile", profileTag);
		output.setInteger("PatrolIndex", patrolIndex);
		output.setInteger("PatrolIdleTicks", patrolIdleTicks);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound input) {
		super.readEntityFromNBT(input);
		this.enablePersistence();
		if (input.hasKey("NpcProfile", 10)) {
			this.profile = NpcProfile.read(input.getCompoundTag("NpcProfile"));
			if (!profile.getDisplayName().isEmpty() || profile.getQuestRelevance() != QuestRelevance.NONE) {
				refreshName();
			}
		}
		this.patrolIndex = input.hasKey("PatrolIndex") ? input.getInteger("PatrolIndex") : 0;
		this.patrolIdleTicks = input.hasKey("PatrolIdleTicks") ? input.getInteger("PatrolIdleTicks") : -1;
	}
}
