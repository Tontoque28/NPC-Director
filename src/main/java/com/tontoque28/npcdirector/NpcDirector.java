package com.tontoque28.npcdirector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tontoque28.npcdirector.command.DevCommands;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class NpcDirector implements ModInitializer {
	public static final String MOD_ID = "npcdirector";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static int tickCounter = 0;

	@Override
	public void onInitialize() {
		ModEntities.register();
		DialogueManager.register();
		WaypointPathManager.register();
		NetworkingInit.registerCommon();
		NetworkingInit.registerServer();
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				DevCommands.register(dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (++tickCounter % 5 == 0) {
				DialogueInteractionHandler.tick(server);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> DialogueInteractionHandler.clearAll());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> DialogueInteractionHandler.close(handler.player));
		LOGGER.info("NPC Director initialized");
	}
}
