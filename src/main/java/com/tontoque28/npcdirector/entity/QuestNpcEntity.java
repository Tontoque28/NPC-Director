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
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class QuestNpcEntity extends EntityCreature {

	private static final DataParameter<Boolean> SEATED = EntityDataManager.createKey(QuestNpcEntity.class, DataSerializers.BOOLEAN);
	private static final DataParameter<String> SKIN_HASH = EntityDataManager.createKey(QuestNpcEntity.class, DataSerializers.STRING);

	private NpcProfile profile = new NpcProfile();
	private int patrolIndex = 0;
	private int patrolIdleTicks = -1;
	private boolean seatApplied = false;

	public QuestNpcEntity(World world) {
		super(world);
		this.setSize(0.6F, 1.95F);
		this.enablePersistence();
	}

	@Override
	protected void entityInit() {
		super.entityInit();
		this.dataManager.register(SEATED, Boolean.FALSE);
		this.dataManager.register(SKIN_HASH, "");
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
		final QuestNpcEntity self = this;
		this.tasks.addTask(0, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F) {
			@Override
			public boolean shouldExecute() {
				return self.profile.isLookAtPlayer() && !self.profile.isSeated() && super.shouldExecute();
			}
		});
		this.tasks.addTask(1, new NpcPatrolGoal(this));
		this.tasks.addTask(2, new EntityAIWanderAvoidWater(this, 0.6) {
			@Override
			public boolean shouldExecute() {
				return !self.profile.isStationary() && !self.profile.isSeated() && super.shouldExecute();
			}
		});
		this.tasks.addTask(3, new EntityAILookIdle(this) {
			@Override
			public boolean shouldExecute() {
				return !self.profile.isSeated() && super.shouldExecute();
			}
		});
	}

	public boolean isSeatedSynced() {
		return this.dataManager.get(SEATED);
	}

	public String getSkinHashSynced() {
		return this.dataManager.get(SKIN_HASH);
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
				if (serverPlayer.isSneaking() && AdminService.canEdit(serverPlayer)) {
					AdminService.openFor(serverPlayer, this);
					return true;
				}
				DialogueNode node = DialogueInteractionHandler.open(serverPlayer, this);
				NetworkingInit.sendNodeOrClose(serverPlayer, node, profile.getDisplayName());
			}
		}
		return true;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (!profile.isTargetableByHostiles() && source.getTrueSource() instanceof IMob) {
			return false;
		}
		if (profile.isInvulnerable() && source != DamageSource.OUT_OF_WORLD) {
			return false;
		}
		return super.attackEntityFrom(source, amount);
	}

	@Override
	public boolean canBePushed() {
		return !profile.isSeated() && super.canBePushed();
	}

	@Override
	public void onLivingUpdate() {
		super.onLivingUpdate();
		if (this.world.isRemote) {
			return;
		}
		if (profile.isSeated()) {
			this.setNoGravity(true);
			this.seatApplied = true;
			this.motionX = 0.0;
			this.motionZ = 0.0;
			this.motionY = 0.0;
			this.getNavigator().clearPath();
			if (this.ticksExisted % 4 == 0) {
				double surface = SeatHelper.surface(this.world, this.posX, this.posY, this.posZ);
				if (Double.isNaN(surface)) {
					this.setNoGravity(false);
				} else if (Math.abs(surface - this.posY) > 1.0E-4) {
					this.setPosition(this.posX, surface, this.posZ);
				}
			}
		} else if (seatApplied) {
			seatApplied = false;
			this.setNoGravity(false);
		}
	}

	public NpcProfile getProfile() {
		return profile;
	}

	public void setProfile(NpcProfile profile) {
		this.profile = profile;
		this.enablePersistence();
		syncProfileData();
		refreshName();
	}

	public void syncProfileData() {
		this.dataManager.set(SEATED, profile.isSeated());
		this.dataManager.set(SKIN_HASH, profile.getSkinPngHash());
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
			syncProfileData();
		}
		this.patrolIndex = input.hasKey("PatrolIndex") ? input.getInteger("PatrolIndex") : 0;
		this.patrolIdleTicks = input.hasKey("PatrolIdleTicks") ? input.getInteger("PatrolIdleTicks") : -1;
	}
}
