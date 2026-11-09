package com.tontoque28.npcdirector.waypoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.util.Ids;
import com.tontoque28.npcdirector.util.JsonDataReloadListener;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaypointPathManager {

	private static final Map<Identifier, WaypointPath> PATHS = new HashMap<>();

	public static java.util.List<String> ids() {
		return PATHS.keySet().stream().map(Identifier::toString).sorted().collect(java.util.stream.Collectors.toList());
	}

	public static void register() {
		ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
				Identifier.fromNamespaceAndPath(NpcDirector.MOD_ID, "paths"),
				new JsonDataReloadListener("paths", WaypointPathManager::loadFromJson));
	}

	public static WaypointPath get(Identifier id) {
		return PATHS.get(id);
	}

	public static WaypointPath get(String id) {
		Identifier parsed = Ids.parse(id);
		return parsed == null ? null : PATHS.get(parsed);
	}

	public static void loadFromJson(Map<Identifier, JsonElement> prepared) {
		PATHS.clear();
		for (Map.Entry<Identifier, JsonElement> entry : prepared.entrySet()) {
			try {
				PATHS.put(entry.getKey(), parsePath(entry.getKey(), entry.getValue().getAsJsonObject()));
			} catch (Exception exception) {
				NpcDirector.LOGGER.warn("Camino invalido {}: {}", entry.getKey(), exception.getMessage());
			}
		}
		NpcDirector.LOGGER.info("Caminos cargados: {}", PATHS.size());
	}

	private static WaypointPath parsePath(Identifier id, JsonObject root) {
		boolean loop = root.has("loop") && root.get("loop").getAsBoolean();
		List<Waypoint> points = new ArrayList<>();
		JsonArray pointsArray = root.getAsJsonArray("points");
		for (JsonElement pointElement : pointsArray) {
			points.add(parseWaypoint(pointElement.getAsJsonObject()));
		}
		return new WaypointPath(id.toString(), points, loop);
	}

	private static Waypoint parseWaypoint(JsonObject pointObject) {
		BlockPos position = new BlockPos(
				pointObject.get("x").getAsInt(),
				pointObject.get("y").getAsInt(),
				pointObject.get("z").getAsInt()
		);
		int waitTicks = pointObject.has("wait") ? pointObject.get("wait").getAsInt() : 0;

		String onArriveQuestId = null;
		String onArriveState = null;
		if (pointObject.has("onArrive")) {
			JsonObject onArrive = pointObject.getAsJsonObject("onArrive");
			onArriveQuestId = onArrive.get("quest").getAsString();
			onArriveState = onArrive.get("state").getAsString();
		}

		String onDepartQuestId = null;
		String onDepartState = null;
		if (pointObject.has("onDepart")) {
			JsonObject onDepart = pointObject.getAsJsonObject("onDepart");
			onDepartQuestId = onDepart.get("quest").getAsString();
			onDepartState = onDepart.get("state").getAsString();
		}

		return new Waypoint(position, waitTicks, onArriveQuestId, onArriveState, onDepartQuestId, onDepartState);
	}
}
