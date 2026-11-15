package com.tontoque28.npcdirector.entity;

import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import com.tontoque28.npcdirector.admin.AdminService;
import com.tontoque28.npcdirector.data.NpcProfile;
import com.tontoque28.npcdirector.data.QuestRelevance;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueNode;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.skin.CustomSkinService;
import com.tontoque28.npcdirector.skin.SkinResolver;
import com.tontoque28.npcdirector.util.DebugLog;
import com.tontoque28.npcdirector.util.Validate;
import com.tontoque28.npcdirector.waypoint.NpcPatrolGoal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class QuestNpcEntity extends PathfinderMob {

	public static final long GREET_COOLDOWN_TICKS = 400L;
	public static final EquipmentSlot[] EQUIP_SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	public static final String[] EQUIP_NAMES = {"mainhand", "offhand", "head", "chest", "legs", "feet"};

	private static final EntityDataAccessor<String> SKIN_NAME = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> SKIN_TEXTURE = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> SKIN_UUID = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> SKIN_PNG_HASH = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> ARM_MODEL = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SITTING = SynchedEntityData.defineId(QuestNpcEntity.class, EntityDataSerializers.BOOLEAN);

	private NpcProfile profile = new NpcProfile();
	private int patrolIndex = 0;
	private int patrolIdleTicks = -1;
	private int skinToken = 0;
	private boolean seatLock = false;
	private boolean skinChecked = false;
	private final Map<UUID, Long> greetedAt = new HashMap<>();
	private final Set<UUID> inGreetRange = new HashSet<>();

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
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(SKIN_NAME, "");
		this.entityData.define(SKIN_TEXTURE, "");
		this.entityData.define(SKIN_UUID, "");
		this.entityData.define(SKIN_PNG_HASH, "");
		this.entityData.define(SCALE, 1.0F);
		this.entityData.define(ARM_MODEL, 0);
		this.entityData.define(SITTING, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new NpcLookGoal(this));
		this.goalSelector.addGoal(1, new NpcPatrolGoal(this));
		this.goalSelector.addGoal(2, new NpcStrollGoal(this));
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

	public String getSyncedSkinName() {
		return this.entityData.get(SKIN_NAME);
	}

	public String getSyncedSkinTexture() {
		return this.entityData.get(SKIN_TEXTURE);
	}

	public String getSyncedSkinUuid() {
		return this.entityData.get(SKIN_UUID);
	}

	public String getSyncedSkinPngHash() {
		return this.entityData.get(SKIN_PNG_HASH);
	}

	public int getSyncedArmModel() {
		return this.entityData.get(ARM_MODEL);
	}

	public boolean isSyncedSitting() {
		return this.entityData.get(SITTING);
	}

	public float getSyncedScale() {
		return this.entityData.get(SCALE);
	}

	@Override
	public float getScale() {
		return this.entityData.get(SCALE);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (SCALE.equals(key)) {
			this.refreshDimensions();
		}
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (!this.level().isClientSide()) {
			if (player instanceof ServerPlayer serverPlayer) {
				if (serverPlayer.isShiftKeyDown() && AdminService.canEdit(serverPlayer)) {
					AdminService.openFor(serverPlayer, this);
					return InteractionResult.SUCCESS;
				}
				if (!profile.isEnabled()) {
					return InteractionResult.SUCCESS;
				}
				DialogueNode node = DialogueInteractionHandler.open(serverPlayer, this);
				if (node != null) {
					playOpenSound();
				}
				NetworkingInit.sendNodeOrClose(serverPlayer, node);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.CONSUME;
	}

	private void playOpenSound() {
		String id = profile.getSoundId();
		if (id == null || id.isEmpty() || profile.getSoundVolume() <= 0.0F) {
			return;
		}
		SoundEvent event = Validate.soundEvent(id);
		if (event == null) {
			return;
		}
		this.level().playSound(null, this.getX(), this.getY(), this.getZ(), event, SoundSource.NEUTRAL, profile.getSoundVolume(), profile.getSoundPitch());
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!profile.isTargetableByHostiles() && source.getEntity() instanceof Enemy) {
			return false;
		}
		return super.hurt(source, amount);
	}

	@Override
	public boolean isInvulnerableTo(DamageSource source) {
		if (profile.isInvulnerable() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}
		return super.isInvulnerableTo(source);
	}

	@Override
	public boolean isPushable() {
		return !profile.isSitting() && super.isPushable();
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && !skinChecked) {
			skinChecked = true;
			if (!profile.getSkinName().isEmpty() && profile.getSkinTexture().isEmpty()) {
				resolveSkin(null);
			}
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!this.level().isClientSide() && profile.isSitting() && this.tickCount % 4 == 0 && !this.isPassenger()) {
			snapToSeat();
		}
		if (!this.level().isClientSide() && this.tickCount % 10 == 0) {
			greetTick();
		}
	}

	private void greetTick() {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		String greeting = profile.getGreeting();
		if (greeting.isEmpty() || !profile.isEnabled()) {
			inGreetRange.clear();
			greetedAt.clear();
			return;
		}
		double range = profile.getGreetRange();
		double rangeSquared = range * range;
		long now = serverLevel.getGameTime();
		Set<UUID> current = new HashSet<>();
		for (ServerPlayer player : serverLevel.players()) {
			if (player.isSpectator() || !player.isAlive() || player.distanceToSqr(this) > rangeSquared) {
				continue;
			}
			UUID id = player.getUUID();
			current.add(id);
			if (!inGreetRange.contains(id)) {
				Long last = greetedAt.get(id);
				if (last == null || now - last >= GREET_COOLDOWN_TICKS) {
					greetedAt.put(id, now);
					player.sendSystemMessage(greetingComponent(greeting));
					DebugLog.action("saludo a " + player.getName().getString() + " npc=" + this.getId());
					OutputService.line("[NPC " + this.getId() + "] saludo a " + player.getName().getString());
				}
			}
		}
		inGreetRange.clear();
		inGreetRange.addAll(current);
		greetedAt.values().removeIf(time -> now - time > GREET_COOLDOWN_TICKS);
	}

	private Component greetingComponent(String greeting) {
		String name = profile.getDisplayName().isEmpty() ? "NPC" : profile.getDisplayName();
		MutableComponent prefix = Component.literal("<" + name + "> ").withStyle(Validate.format(profile.getNameColor()));
		return prefix.append(Component.literal(greeting));
	}

	public NpcProfile getProfile() {
		return profile;
	}

	public void setProfile(NpcProfile profile) {
		this.profile = profile;
		this.setPersistenceRequired();
		refreshName();
		syncFromProfile();
	}

	public void syncFromProfile() {
		this.entityData.set(SKIN_NAME, profile.getSkinName());
		this.entityData.set(SKIN_TEXTURE, profile.getSkinTexture());
		this.entityData.set(SKIN_UUID, profile.getSkinUuid());
		this.entityData.set(SKIN_PNG_HASH, profile.getSkinPngHash());
		this.entityData.set(SCALE, profile.getScale());
		this.entityData.set(ARM_MODEL, profile.getArmModel());
		boolean sitting = profile.isSitting();
		this.entityData.set(SITTING, sitting);
		if (sitting) {
			this.seatLock = true;
			this.setNoGravity(true);
		} else if (this.seatLock) {
			this.seatLock = false;
			this.setNoGravity(false);
		}
		if ((sitting || !profile.isEnabled()) && !this.level().isClientSide()) {
			this.getNavigation().stop();
		}
		this.refreshDimensions();
	}

	public void resolveSkin(Consumer<Component> notify) {
		if (!(this.level() instanceof ServerLevel serverLevel)) {
			return;
		}
		String requested = profile.getSkinName();
		if (requested.isEmpty()) {
			return;
		}
		int token = ++skinToken;
		SkinResolver.resolve(serverLevel.getServer(), requested, result -> {
			if (this.isRemoved() || token != skinToken || !requested.equals(profile.getSkinName())) {
				return;
			}
			if (result.status() == SkinResolver.Status.OK) {
				profile.setSkinTexture(result.texture());
				profile.setSkinUuid(result.uuid());
				if (Validate.validSkinName(result.name())) {
					profile.setSkinName(result.name());
				}
				syncFromProfile();
				DebugLog.action("skin resuelta " + requested + " npc=" + this.getId());
				OutputService.line("[NPC " + this.getId() + "] skin resuelta " + requested);
				if (notify != null) {
					notify.accept(Component.translatableWithFallback("npcdirector.skin.applied", "[NPCDirector] Skin of %s applied", profile.getSkinName()));
				}
				return;
			}
			profile.setSkinTexture("");
			profile.setSkinUuid("");
			syncFromProfile();
			String key = switch (result.status()) {
				case NOT_FOUND -> "npcdirector.skin.not_found";
				case NO_TEXTURE -> "npcdirector.skin.no_texture";
				case RATE_LIMITED -> "npcdirector.skin.rate_limited";
				case NETWORK -> "npcdirector.skin.network";
				default -> "npcdirector.skin.error";
			};
			DebugLog.warn("skin " + requested + " fallo " + result.status() + " " + result.detail());
			OutputService.line("[NPC " + this.getId() + "] skin " + requested + " fallo " + result.status());
			if (notify != null) {
				notify.accept(Component.translatable(key, requested, result.detail()));
			}
		});
	}

	public void clearSkin() {
		skinToken++;
		profile.setSkinName("");
		profile.setSkinTexture("");
		profile.setSkinUuid("");
		profile.setSkinPng("");
		syncFromProfile();
	}

	public void setCustomSkin(byte[] png) {
		skinToken++;
		profile.setSkinName("");
		profile.setSkinTexture("");
		profile.setSkinUuid("");
		profile.setSkinPng(Base64.getEncoder().encodeToString(png));
		syncFromProfile();
		CustomSkinService.sendToTrackers(this);
	}

	private void snapToSeat() {
		double surface = SeatHelper.surface(this.level(), this.getX(), this.getY(), this.getZ());
		if (Double.isNaN(surface)) {
			this.setNoGravity(false);
			return;
		}
		this.setNoGravity(true);
		if (Math.abs(this.getY() - surface) > 0.002D) {
			this.setPos(this.getX(), surface, this.getZ());
			this.setDeltaMovement(0.0D, 0.0D, 0.0D);
		}
	}

	public String equipId(EquipmentSlot slot) {
		ItemStack stack = this.getItemBySlot(slot);
		if (stack.isEmpty()) {
			return "";
		}
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	public void setEquip(EquipmentSlot slot, ItemStack stack) {
		this.setItemSlot(slot, stack);
		this.setDropChance(slot, 0.0F);
	}

	public void refreshName() {
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
		MutableComponent component = Component.literal(prefix + (name.isEmpty() ? "NPC" : name)).withStyle(Validate.format(profile.getNameColor()));
		this.setCustomName(component);
		this.setCustomNameVisible(profile.isNameVisible());
	}

	@Override
	public void addAdditionalSaveData(CompoundTag output) {
		super.addAdditionalSaveData(output);
		CompoundTag profileTag = new CompoundTag();
		profile.write(profileTag);
		output.put("NpcProfile", profileTag);
		output.putInt("PatrolIndex", patrolIndex);
		output.putInt("PatrolIdleTicks", patrolIdleTicks);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag input) {
		super.readAdditionalSaveData(input);
		if (input.contains("NpcProfile", Tag.TAG_COMPOUND)) {
			this.profile = NpcProfile.read(input.getCompound("NpcProfile"));
			this.setPersistenceRequired();
			if (!profile.getDisplayName().isEmpty() || profile.getQuestRelevance() != QuestRelevance.NONE) {
				refreshName();
			}
			syncFromProfile();
		}
		this.patrolIndex = input.contains("PatrolIndex") ? input.getInt("PatrolIndex") : 0;
		this.patrolIdleTicks = input.contains("PatrolIdleTicks") ? input.getInt("PatrolIdleTicks") : -1;
	}

	private long editHoldUntil;

	public void holdForEdit() {
		this.editHoldUntil = this.level().getGameTime() + 1200L;
		this.getNavigation().stop();
	}

	public void releaseEditHold() {
		this.editHoldUntil = 0L;
	}

	public boolean isHeldForEdit() {
		return this.level().getGameTime() < this.editHoldUntil;
	}

	private static class NpcLookGoal extends LookAtPlayerGoal {

		private final QuestNpcEntity npc;

		NpcLookGoal(QuestNpcEntity npc) {
			super(npc, Player.class, 8.0F);
			this.npc = npc;
		}

		@Override
		public boolean canUse() {
			return npc.getProfile().isLookAtPlayer() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return npc.getProfile().isLookAtPlayer() && super.canContinueToUse();
		}
	}

	private static class NpcStrollGoal extends WaterAvoidingRandomStrollGoal {

		private final QuestNpcEntity npc;

		NpcStrollGoal(QuestNpcEntity npc) {
			super(npc, 0.6);
			this.npc = npc;
		}

		@Override
		public boolean canUse() {
			return npc.getProfile().isEnabled() && !npc.isHeldForEdit() && !npc.getProfile().isStationary() && !npc.getProfile().isSitting() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return npc.getProfile().isEnabled() && !npc.isHeldForEdit() && !npc.getProfile().isStationary() && !npc.getProfile().isSitting() && super.canContinueToUse();
		}
	}
}
