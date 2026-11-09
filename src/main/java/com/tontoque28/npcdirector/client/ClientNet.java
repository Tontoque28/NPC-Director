package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.network.NetworkingInit;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientNet {

	public static void send(Object message) {
		ClientPlayNetworking.send(NetworkingInit.idOf(message), NetworkingInit.encode(message));
	}
}
