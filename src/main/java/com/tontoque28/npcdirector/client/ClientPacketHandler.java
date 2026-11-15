package com.tontoque28.npcdirector.client;

import java.util.List;

import com.tontoque28.npcdirector.network.OpenAdminPayload;
import com.tontoque28.npcdirector.network.SkinSyncPayload;

import net.minecraft.client.Minecraft;

public class ClientPacketHandler {

	public static void open(String text, List<String> optionTexts, String npcName) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new DialogueScreen(text, optionTexts, npcName)));
	}

	public static void openAdmin(OpenAdminPayload payload) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new AdminScreen(payload)));
	}

	public static void skinSync(SkinSyncPayload payload) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> CustomSkins.register(payload.hash(), payload.png()));
	}

	public static void panelMessage(String key) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> {
			if (minecraft.currentScreen instanceof AdminScreen) {
				((AdminScreen) minecraft.currentScreen).showMessage(key);
			}
		});
	}

	public static void close() {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> {
			if (minecraft.currentScreen instanceof DialogueScreen) {
				minecraft.displayGuiScreen(null);
			}
		});
	}
}
