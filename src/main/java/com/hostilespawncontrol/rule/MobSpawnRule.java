package com.hostilespawncontrol.rule;

import java.util.EnumMap;
import java.util.Map;

/** Per-mob rule: one allow/deny flag per spawn source. Missing sources are allowed. */
public final class MobSpawnRule {
	private final Map<SpawnSource, Boolean> allowed = new EnumMap<>(SpawnSource.class);

	public synchronized boolean isAllowed(SpawnSource source) {
		return this.allowed.getOrDefault(source, true);
	}

	public synchronized void set(SpawnSource source, boolean value) {
		if (value) {
			this.allowed.remove(source);
		} else {
			this.allowed.put(source, false);
		}
	}

	public synchronized boolean isDefault() {
		return this.allowed.isEmpty();
	}

	public synchronized Map<SpawnSource, Boolean> snapshot() {
		Map<SpawnSource, Boolean> copy = new EnumMap<>(SpawnSource.class);
		copy.putAll(this.allowed);
		return copy;
	}
}
