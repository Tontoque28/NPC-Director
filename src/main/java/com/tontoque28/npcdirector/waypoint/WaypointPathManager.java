package com.tontoque28.npcdirector.waypoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tontoque28.npcdirector.NpcDirector;
import com.tontoque28.npcdirector.util.Ids;
import com.tontoque28.npcdirector.util.JsonDataReloadListener;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.resources.ResourcePackType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaypointPathManager {

	public static final int MAX_POINTS = 64;

	private static final Map<ResourceLocation, WaypointPath> PATHS = new HashMap<>();
	private static final Map<ResourceLocation, WaypointPath> WORLD = new HashMap<>();

	public static List<String> ids() {
		return java.util.stream.Stream.concat(PATHS.keySet().stream(), WORLD.keySet().stream()).map(ResourceLocation::toString).distinct().sorted().collect(java.util.stream.Collectors.toList());
	}

	public static void setWorld(Map<String, String> json) {
		WORLD.clear();
		for (Map.Entry<String, String> entry : json.entrySet()) {
			ResourceLocation id = new ResourceLocation(com.tontoque28.npcdirector.world.WorldContent.NAMESPACE, entry.getKey());
			try {
				WORLD.put(id, parsePath(id, new com.google.gson.JsonParser().parse(entry.getValue()).getAsJsonObject()));
			} catch (Exception exception) {
				NpcDirector.LOGGER.warn("Camino de mundo invalido {}: {}", id, exception.getMessage());
			}
		}
	}

	public static JsonDataReloadListener listener() {
		return new JsonDataReloadListener("paths", WaypointPathManager::loadFromJson);
	}

	public static WaypointPath get(ResourceLocation id) {
		WaypointPath path = PATHS.get(id);
		return path != null ? path : WORLD.get(id);
	}

	public static WaypointPath get(String id) {
		ResourceLocation parsed = Ids.parse(id);
		return parsed == null ? null : get(parsed);
	}

	public static void loadFromJson(Map<ResourceLocation, JsonElement> prepared) {
		PATHS.clear();
		for (Map.Entry<ResourceLocation, JsonElement> entry : prepared.entrySet()) {
			try {
				PATHS.put(entry.getKey(), parsePath(entry.getKey(), entry.getValue().getAsJsonObject()));
			} catch (Exception exception) {
				NpcDirector.LOGGER.warn("Camino invalido {}: {}", entry.getKey(), exception.getMessage());
			}
		}
		NpcDirector.LOGGER.info("Caminos cargados: {}", PATHS.size());
	}

	private static WaypointPath parsePath(ResourceLocation id, JsonObject root) {
		boolean loop = root.has("loop") && root.get("loop").getAsBoolean();
		List<Waypoint> points = new ArrayList<>();
		JsonArray pointsArray = root.getAsJsonArray("points");
		for (JsonElement pointElement : pointsArray) {
			if (points.size() >= MAX_POINTS) {
				break;
			}
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
