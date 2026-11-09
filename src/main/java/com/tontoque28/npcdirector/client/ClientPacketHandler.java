package com.tontoque28.npcdirector.client;

import java.util.List;

import com.tontoque28.npcdirector.network.OpenAdminPayload;

import net.minecraft.client.Minecraft;

public class ClientPacketHandler {

	public static void open(String text, List<String> optionTexts) {
		Minecraft.getInstance().setScreen(new DialogueScreen(text, optionTexts));
	}

	public static void openAdmin(OpenAdminPayload payload) {
		Minecraft.getInstance().setScreen(new AdminScreen(payload));
	}

	public static void close() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof DialogueScreen) {
			minecraft.setScreen(null);
		}
	}
}
