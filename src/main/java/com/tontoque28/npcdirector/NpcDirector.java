package com.tontoque28.npcdirector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tontoque28.npcdirector.client.NpcDirectorClient;
import com.tontoque28.npcdirector.command.DevCommands;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NpcDirector.MOD_ID)
public class NpcDirector {
	public static final String MOD_ID = "npcdirector";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public NpcDirector() {
		IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
		ModEntities.ENTITY_TYPES.register(modBus);
		modBus.addListener(ModEntities::registerAttributes);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onAddReloadListeners);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onRegisterCommands);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerTick);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onServerStopping);
		MinecraftForge.EVENT_BUS.addListener(NpcDirector::onPlayerLoggedOut);
		NetworkingInit.init();
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NpcDirectorClient.register(modBus));
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
			com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler.tick(net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer());
		}
	}

	private static void onServerStopping(net.minecraftforge.fml.event.server.FMLServerStoppingEvent event) {
		com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler.clearAll();
	}

	private static void onPlayerLoggedOut(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getPlayer() instanceof net.minecraft.entity.player.ServerPlayerEntity) {
			com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler.close((net.minecraft.entity.player.ServerPlayerEntity) event.getPlayer());
		}
	}

	private static void onRegisterCommands(RegisterCommandsEvent event) {
		DevCommands.register(event.getDispatcher());
	}
}
