package com.tontoque28.npcdirector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tontoque28.npcdirector.client.NpcDirectorClient;
import com.tontoque28.npcdirector.command.DevCommands;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(NpcDirector.MOD_ID)
public class NpcDirector {
	public static final String MOD_ID = "npcdirector";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public NpcDirector(FMLJavaModLoadingContext context) {
		IEventBus modBus = context.getModEventBus();
		ModEntities.ENTITY_TYPES.register(modBus);
		modBus.addListener(ModEntities::registerAttributes);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onAddReloadListeners);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onRegisterCommands);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerTick);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerStopping);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onPlayerLoggedOut);
		NetworkingInit.init();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			NpcDirectorClient.register(modBus);
		}
		LOGGER.info("NPC Director initialized");
	}

	private static void onAddReloadListeners(AddReloadListenerEvent event) {
		event.addListener(DialogueManager.listener());
		event.addListener(WaypointPathManager.listener());
	}

	private static int tickCounter = 0;

	private static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
		if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
			return;
		}
		if (++tickCounter % 5 == 0) {
			com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler.tick(event.getServer());
		}
	}

	private static void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
		com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler.clearAll();
	}

	private static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
			com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler.close(player);
		}
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		DevCommands.register(event.getDispatcher());
	}
}
