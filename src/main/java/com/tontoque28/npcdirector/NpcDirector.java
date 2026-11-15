package com.tontoque28.npcdirector;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.tontoque28.npcdirector.command.DevCommands;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.output.OutputService;
import com.tontoque28.npcdirector.skin.CustomSkinService;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;
import com.tontoque28.npcdirector.world.WorldContent;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class NpcDirector implements ModInitializer {
	public static final String MOD_ID = "npcdirector";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	private static int tickCounter = 0;

	@Override
	public void onInitialize() {
		ModEntities.register();
		DialogueManager.register();
		WaypointPathManager.register();
		NetworkingInit.registerCommon();
		NetworkingInit.registerServer();
		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
				DevCommands.register(dispatcher));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			OutputService.flush(server);
			if (++tickCounter % 5 == 0) {
				DialogueInteractionHandler.tick(server);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(server -> WorldContent.get(server).publish());
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			WorldContent.unpublish();
			OutputService.clearAll();
			CustomSkinService.clearAll();
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> DialogueInteractionHandler.clearAll());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			DialogueInteractionHandler.close(handler.player);
			OutputService.remove(handler.player.getUUID());
			CustomSkinService.forget(handler.player.getUUID());
		});
		LOGGER.info("NPC Director initialized");
	}
}
