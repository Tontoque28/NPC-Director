package com.tontoque28.npcdirector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tontoque28.npcdirector.client.NpcDirectorClient;
import com.tontoque28.npcdirector.command.DevCommands;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(NpcDirector.MOD_ID)
public class NpcDirector {
	public static final String MOD_ID = "npcdirector";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public NpcDirector(FMLJavaModLoadingContext context) {
		ModEntities.ENTITY_TYPES.register(context.getModBusGroup());
		EntityAttributeCreationEvent.BUS.addListener(ModEntities::registerAttributes);
		AddReloadListenerEvent.BUS.addListener(NpcDirector::onAddReloadListeners);
		RegisterCommandsEvent.BUS.addListener(NpcDirector::onRegisterCommands);
		TickEvent.ServerTickEvent.Post.BUS.addListener(NpcDirector::onServerTick);
		ServerStoppingEvent.BUS.addListener(NpcDirector::onServerStopping);
		PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(NpcDirector::onPlayerLoggedOut);
		NetworkingInit.init();
		if (FMLEnvironment.dist == Dist.CLIENT) {
			NpcDirectorClient.register();
		}
		LOGGER.info("NPC Director initialized");
	}

	private static void onAddReloadListeners(AddReloadListenerEvent event) {
		event.addListener(DialogueManager.listener());
		event.addListener(WaypointPathManager.listener());
	}

	private static int tickCounter = 0;

	private static void onServerTick(TickEvent.ServerTickEvent.Post event) {
		if (++tickCounter % 5 == 0) {
			DialogueInteractionHandler.tick(event.server());
		}
	}

	private static void onServerStopping(ServerStoppingEvent event) {
		DialogueInteractionHandler.clearAll();
	}

	private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			DialogueInteractionHandler.close(player);
		}
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		DevCommands.register(event.getDispatcher());
	}
}
