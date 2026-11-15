package com.tontoque28.npcdirector.client;

import java.util.List;

import com.tontoque28.npcdirector.network.OpenAdminPayload;

import net.minecraft.client.Minecraft;

public class ClientPacketHandler {

	public static void open(String text, List<String> optionTexts, String npcName) {
		Minecraft.getInstance().setScreen(new DialogueScreen(text, optionTexts, npcName));
	}

	public static void openAdmin(OpenAdminPayload payload) {
		Minecraft.getInstance().setScreen(new AdminScreen(payload));
	}

	public static void panelMessage(net.minecraft.util.text.ITextComponent message) {
		if (Minecraft.getInstance().screen instanceof AdminScreen) {
			((AdminScreen) Minecraft.getInstance().screen).showServerMessage(message);
		}
	}

	public static void skinSync(String hash, byte[] png) {
		CustomSkins.receive(hash, png);
	}

	public static void output(List<String> lines) {
		AdminScreen.pushOutput(lines);
	}

	public static void close() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof DialogueScreen) {
			minecraft.setScreen(null);
		}
	}
}
