package com.tontoque28.npcdirector.client;

import com.tontoque28.npcdirector.network.NetworkingInit;

public class ClientNet {

	public static void send(Object payload) {
		NetworkingInit.sendToServer(payload);
	}
}
