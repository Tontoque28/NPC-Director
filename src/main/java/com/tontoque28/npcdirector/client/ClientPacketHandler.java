package com.tontoque28.npcdirector.client;

import java.util.List;

import com.tontoque28.npcdirector.network.OpenAdminPayload;

import net.minecraft.client.Minecraft;

public class ClientPacketHandler {

	public static void open(String text, List<String> optionTexts) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new DialogueScreen(text, optionTexts)));
	}

	public static void openAdmin(OpenAdminPayload payload) {
		Minecraft minecraft = Minecraft.getMinecraft();
		minecraft.addScheduledTask(() -> minecraft.displayGuiScreen(new AdminScreen(payload)));
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
