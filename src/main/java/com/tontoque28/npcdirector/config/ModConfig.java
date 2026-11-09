package com.tontoque28.npcdirector.config;

public class ModConfig {

	private static boolean devMode = false;

	public static boolean isDevMode() {
		return devMode;
	}

	public static void setDevMode(boolean value) {
		devMode = value;
	}
}
