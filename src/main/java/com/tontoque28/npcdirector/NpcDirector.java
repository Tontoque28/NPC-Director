package com.tontoque28.npcdirector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tontoque28.npcdirector.client.NpcDirectorClient;
import com.tontoque28.npcdirector.command.DevCommands;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.entity.QuestNpcEntity;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.skin.CustomSkinService;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;
import com.tontoque28.npcdirector.world.WorldContent;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod(NpcDirector.MOD_ID)
public class NpcDirector {
	public static final String MOD_ID = "npcdirector";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private static int tickCounter = 0;

	public NpcDirector() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModEntities.ENTITY_TYPES.register(modBus);
		modBus.addListener(ModEntities::registerAttributes);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onAddReloadListeners);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onRegisterCommands);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerTick);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerStarted);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerStopping);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerStopped);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onPlayerLoggedOut);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onStartTracking);
		NetworkingInit.init();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcDirectorClient.register(modBus));
		LOGGER.info("NPC Director initialized");
	}

	private static void onAddReloadListeners(AddReloadListenerEvent event) {
		event.addListener(DialogueManager.listener());
		event.addListener(WaypointPathManager.listener());
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		DevCommands.register(event.getDispatcher());
	}

	private static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return;
		}
		OutputService.flush(server);
		if (++tickCounter % 5 == 0) {
			DialogueInteractionHandler.tick(server);
		}
	}

	private static void onServerStarted(FMLServerStartedEvent event) {
		WorldContent.get(event.getServer()).publish();
	}

	private static void onServerStopping(FMLServerStoppingEvent event) {
		DialogueInteractionHandler.clearAll();
	}

	private static void onServerStopped(FMLServerStoppedEvent event) {
		WorldContent.unpublish();
		OutputService.clearAll();
		CustomSkinService.clearAll();
	}

	private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getPlayer() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
			DialogueInteractionHandler.close(player);
			OutputService.remove(player.getUUID());
			CustomSkinService.forget(player.getUUID());
		}
	}

	private static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof QuestNpcEntity && event.getPlayer() instanceof ServerPlayerEntity) {
			CustomSkinService.sendTo((ServerPlayerEntity) event.getPlayer(), (QuestNpcEntity) event.getTarget());
		}
	}
}
