package com.tontoque28.npcdirector.waypoint;

import java.util.List;

public class WaypointPath {

	private final String id;
	private final List<Waypoint> points;
	private final boolean loop;

	public WaypointPath(String id, List<Waypoint> points, boolean loop) {
		this.id = id;
		this.points = points;
		this.loop = loop;
	}

	public String getId() {
		return id;
	}

	public List<Waypoint> getPoints() {
		return points;
	}

	public boolean isLoop() {
		return loop;
	}

	public int nextIndex(int currentIndex) {
		int next = currentIndex + 1;
		if (next >= points.size()) {
			return loop ? 0 : currentIndex;
		}
		return next;
	}
}
