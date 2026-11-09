package com.tontoque28.npcdirector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.tontoque28.npcdirector.client.NpcDirectorClient;
import com.tontoque28.npcdirector.command.DevCommands;
import com.tontoque28.npcdirector.dialogue.DialogueManager;
import com.tontoque28.npcdirector.entity.ModEntities;
import com.tontoque28.npcdirector.network.NetworkingInit;
import com.tontoque28.npcdirector.util.DataLoader;
import com.tontoque28.npcdirector.waypoint.WaypointPathManager;

import net.minecraft.server.MinecraftServer;
import com.tontoque28.npcdirector.dialogue.DialogueInteractionHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = NpcDirector.MOD_ID, name = "NPC Director", version = "0.2.0", acceptedMinecraftVersions = "[1.12.2]")
public class NpcDirector {
	public static final String MOD_ID = "npcdirector";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@Mod.Instance(MOD_ID)
	public static NpcDirector instance;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		DataLoader.setModSource(event.getSourceFile());
		ModEntities.register();
		NetworkingInit.init();
		MinecraftForge.EVENT_BUS.register(this);
		if (event.getSide() == Side.CLIENT) {
			NpcDirectorClient.register();
		}
		LOGGER.info("NPC Director initialized");
	}

	@Mod.EventHandler
	public void serverStarting(FMLServerStartingEvent event) {
		reloadData(event.getServer());
		event.registerServerCommand(new DevCommands());
	}

	@Mod.EventHandler
	public void serverStopping(FMLServerStoppingEvent event) {
		DialogueInteractionHandler.clearAll();
	}

	private int tickCounter = 0;

	@SubscribeEvent
	public void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		if (++tickCounter % 5 == 0) {
			DialogueInteractionHandler.tick(FMLCommonHandler.instance().getMinecraftServerInstance());
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.player instanceof EntityPlayerMP) {
			DialogueInteractionHandler.close((EntityPlayerMP) event.player);
		}
	}

	public static void reloadData(MinecraftServer server) {
		DataLoader.setWorldDirectory(server.getWorld(0).getSaveHandler().getWorldDirectory());
		DialogueManager.reload();
		WaypointPathManager.reload();
	}
}
