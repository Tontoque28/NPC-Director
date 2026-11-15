package com.tontoque28.npcdirector.skin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.util.DebugLog;

import net.minecraft.server.MinecraftServer;

public final class SkinResolver {

	public enum Status {
		OK, NOT_FOUND, NO_TEXTURE, RATE_LIMITED, NETWORK, ERROR
	}

	public record Result(Status status, String name, String uuid, String texture, String detail) {
	}

	private record Entry(Result result, long expiresAt) {
	}

	private static final long OK_TTL = 30L * 60L * 1000L;
	private static final long NOT_FOUND_TTL = 5L * 60L * 1000L;
	private static final long ERROR_TTL = 20L * 1000L;
	private static final long RATE_LIMIT_TTL = 60L * 1000L;
	private static final int ATTEMPTS = 3;
	private static final long RETRY_DELAY = 1500L;
	private static final Map<String, Entry> CACHE = new ConcurrentHashMap<>();
	private static final int MAX_QUEUE = 16;
	private static final int MAX_TEXTURE = 16384;
	private static final Map<String, List<Consumer<Result>>> PENDING = new HashMap<>();
	private static final ExecutorService EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(MAX_QUEUE), runnable -> {
		Thread thread = new Thread(runnable, "NPCDirector-skin");
		thread.setDaemon(true);
		return thread;
	});

	private SkinResolver() {
	}

	public static Result cached(String name) {
		Entry entry = CACHE.get(key(name));
		if (entry == null || entry.expiresAt() < System.currentTimeMillis()) {
			return null;
		}
		return entry.result();
	}

	public static void clearCache() {
		CACHE.clear();
	}

	public static void resolve(MinecraftServer server, String name, Consumer<Result> callback) {
		Result known = cached(name);
		if (known != null) {
			server.execute(() -> callback.accept(known));
			return;
		}
		String key = key(name);
		synchronized (PENDING) {
			List<Consumer<Result>> waiting = PENDING.get(key);
			if (waiting != null) {
				waiting.add(callback);
				return;
			}
			List<Consumer<Result>> created = new ArrayList<>();
			created.add(callback);
			PENDING.put(key, created);
		}
		var repository = server.getProfileRepository();
		var sessions = server.getSessionService();
		try {
			EXECUTOR.execute(() -> {
				Result result = cached(name);
				if (result == null) {
					try {
						result = lookup(repository, sessions, name);
					} catch (Throwable throwable) {
						result = new Result(Status.ERROR, name, "", "", String.valueOf(throwable));
					}
					long ttl = switch (result.status()) {
						case OK, NO_TEXTURE -> OK_TTL;
						case NOT_FOUND -> NOT_FOUND_TTL;
						case RATE_LIMITED -> RATE_LIMIT_TTL;
						default -> ERROR_TTL;
					};
					CACHE.put(key, new Entry(result, System.currentTimeMillis() + ttl));
				}
				finish(server, key, result);
			});
		} catch (RejectedExecutionException exception) {
			finish(server, key, new Result(Status.ERROR, name, "", "", "cola de skins llena"));
		}
	}

	private static void finish(MinecraftServer server, String key, Result result) {
		List<Consumer<Result>> waiting;
		synchronized (PENDING) {
			waiting = PENDING.remove(key);
		}
		if (waiting == null) {
			return;
		}
		for (Consumer<Result> callback : waiting) {
			server.execute(() -> callback.accept(result));
		}
	}

	private static Result lookup(com.mojang.authlib.GameProfileRepository repository, com.mojang.authlib.minecraft.MinecraftSessionService sessions, String name) {
		Result first = lookupAuthlib(repository, sessions, name);
		if (first.status() == Status.OK || first.status() == Status.NOT_FOUND || first.status() == Status.NO_TEXTURE) {
			return first;
		}
		DebugLog.warn("skin " + name + " via authlib " + first.status() + " (" + first.detail() + "), probando la API directa");
		Result second = lookupDirect(name);
		if (second.status() == Status.OK || second.status() == Status.NOT_FOUND || second.status() == Status.NO_TEXTURE) {
			return second;
		}
		NpcDirector.LOGGER.warn("[NPCDirector] skin {} no resuelta: authlib={} directa={}", name, first.detail(), second.detail());
		return second.status() == Status.ERROR && first.status() != Status.ERROR ? first : second;
	}

	private static Status classify(Throwable throwable) {
		for (Throwable current = throwable; current != null; current = current.getCause() == current ? null : current.getCause()) {
			String type = current.getClass().getName();
			String message = String.valueOf(current.getMessage()).toLowerCase(Locale.ROOT);
			if (type.contains("TooManyRequests") || message.contains("429") || message.contains("too many") || message.contains("rate limit")) {
				return Status.RATE_LIMITED;
			}
			if (current instanceof java.net.UnknownHostException || current instanceof java.net.ConnectException
					|| current instanceof java.net.SocketTimeoutException || current instanceof java.net.NoRouteToHostException
					|| current instanceof javax.net.ssl.SSLException || current instanceof java.nio.channels.UnresolvedAddressException
					|| type.contains("HttpTimeout") || type.contains("HttpConnectTimeout")) {
				return Status.NETWORK;
			}
		}
		return Status.ERROR;
	}

	private static String describe(Throwable throwable) {
		Throwable root = throwable;
		while (root.getCause() != null && root.getCause() != root) {
			root = root.getCause();
		}
		String message = root.getMessage() == null ? "" : root.getMessage().replaceAll("[\\r\\n\\t]+", " ").trim();
		String text = root.getClass().getSimpleName() + (message.isEmpty() ? "" : ": " + message);
		return text.length() > 90 ? text.substring(0, 90) : text;
	}

	private static Result lookupAuthlib(com.mojang.authlib.GameProfileRepository repository, com.mojang.authlib.minecraft.MinecraftSessionService sessions, String name) {
		String detail = "";
		Status failed = Status.ERROR;
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			GameProfile[] found = new GameProfile[1];
			Exception[] failure = new Exception[1];
			try {
				repository.findProfilesByNames(new String[] {name}, new ProfileLookupCallback() {
					@Override
					public void onProfileLookupSucceeded(GameProfile profile) {
						found[0] = profile;
					}

					@Override
					public void onProfileLookupFailed(String profileName, Exception exception) {
						failure[0] = exception;
					}
				});
			} catch (Throwable throwable) {
				failure[0] = throwable instanceof Exception exception ? exception : new RuntimeException(throwable);
			}
			if (found[0] != null) {
				GameProfile full;
				try {
					var fetched = sessions.fetchProfile(found[0].getId(), false);
					full = fetched == null ? null : fetched.profile();
					if (full == null) {
						detail = "sin perfil completo";
						failed = Status.ERROR;
					}
				} catch (Throwable throwable) {
					detail = describe(throwable);
					failed = classify(throwable);
					full = null;
				}
				if (full != null) {
					String texture = texture(full);
					if (texture.length() > MAX_TEXTURE) {
						return new Result(Status.ERROR, name, "", "", "textura demasiado grande");
					}
					if (texture.isEmpty()) {
						return new Result(Status.NO_TEXTURE, name, full.getId() == null ? "" : full.getId().toString(), "", "sin propiedad textures");
					}
					return new Result(Status.OK, full.getName() == null ? name : full.getName(), full.getId() == null ? "" : full.getId().toString(), texture, "");
				}
			} else if (failure[0] instanceof ProfileNotFoundException) {
				return new Result(Status.NOT_FOUND, name, "", "", "no existe");
			} else if (failure[0] != null) {
				detail = describe(failure[0]);
				failed = classify(failure[0]);
			} else {
				return new Result(Status.NOT_FOUND, name, "", "", "sin resultado");
			}
			DebugLog.warn("skin " + name + " intento " + attempt + "/" + ATTEMPTS + ": " + detail);
			if (failed == Status.RATE_LIMITED) {
				break;
			}
			if (attempt < ATTEMPTS) {
				try {
					Thread.sleep(RETRY_DELAY);
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		return new Result(failed, name, "", "", detail);
	}

	private static String httpGet(String address, int[] code) throws java.io.IOException {
		java.net.HttpURLConnection connection = (java.net.HttpURLConnection) java.net.URI.create(address).toURL().openConnection();
		connection.setConnectTimeout(6000);
		connection.setReadTimeout(8000);
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("User-Agent", "NPCDirector");
		try {
			code[0] = connection.getResponseCode();
			if (code[0] != 200) {
				return "";
			}
			try (java.io.InputStream in = connection.getInputStream()) {
				byte[] data = in.readNBytes(MAX_TEXTURE * 4);
				return new String(data, java.nio.charset.StandardCharsets.UTF_8);
			}
		} finally {
			connection.disconnect();
		}
	}

	private static Result lookupDirect(String name) {
		try {
			int[] code = {0};
			String body = httpGet("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name, code);
			if (code[0] == 404 || code[0] == 204) {
				return new Result(Status.NOT_FOUND, name, "", "", "no existe (API directa)");
			}
			if (code[0] == 429) {
				return new Result(Status.RATE_LIMITED, name, "", "", "HTTP 429 al buscar el nombre");
			}
			if (code[0] != 200) {
				return new Result(Status.ERROR, name, "", "", "HTTP " + code[0] + " al buscar el nombre");
			}
			com.google.gson.JsonObject profile = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
			String rawId = profile.get("id").getAsString();
			String canonical = profile.has("name") ? profile.get("name").getAsString() : name;
			if (!rawId.matches("[0-9a-fA-F]{32}")) {
				return new Result(Status.ERROR, name, "", "", "id de perfil no valido");
			}
			String dashed = rawId.substring(0, 8) + "-" + rawId.substring(8, 12) + "-" + rawId.substring(12, 16) + "-" + rawId.substring(16, 20) + "-" + rawId.substring(20);
			body = httpGet("https://sessionserver.mojang.com/session/minecraft/profile/" + rawId + "?unsigned=true", code);
			if (code[0] == 429) {
				return new Result(Status.RATE_LIMITED, name, dashed, "", "HTTP 429 al pedir el perfil");
			}
			if (code[0] == 204 || code[0] == 404) {
				return new Result(Status.NOT_FOUND, name, "", "", "perfil sin datos");
			}
			if (code[0] != 200) {
				return new Result(Status.ERROR, name, "", "", "HTTP " + code[0] + " al pedir el perfil");
			}
			com.google.gson.JsonArray properties = com.google.gson.JsonParser.parseString(body).getAsJsonObject().getAsJsonArray("properties");
			String texture = "";
			if (properties != null) {
				for (com.google.gson.JsonElement element : properties) {
					com.google.gson.JsonObject property = element.getAsJsonObject();
					if (property.has("name") && "textures".equals(property.get("name").getAsString()) && property.has("value")) {
						texture = property.get("value").getAsString();
						break;
					}
				}
			}
			if (texture.length() > MAX_TEXTURE) {
				return new Result(Status.ERROR, name, "", "", "textura demasiado grande");
			}
			if (texture.isEmpty()) {
				return new Result(Status.NO_TEXTURE, name, dashed, "", "sin propiedad textures");
			}
			return new Result(Status.OK, canonical, dashed, texture, "");
		} catch (Exception exception) {
			return new Result(classify(exception), name, "", "", describe(exception));
		}
	}

	private static String texture(GameProfile profile) {
		Collection<Property> properties = profile.getProperties().get("textures");
		for (Property property : properties) {
			String value = property.value();
			if (value != null && !value.isEmpty()) {
				return value;
			}
		}
		return "";
	}

	private static String key(String name) {
		return name.toLowerCase(Locale.ROOT);
	}
}
