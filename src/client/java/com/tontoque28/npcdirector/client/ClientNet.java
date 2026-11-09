package com.tontoque28.npcdirector.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public class ClientNet {

	public static void send(CustomPacketPayload payload) {
		ClientPlayNetworking.send(payload);
	}
}
