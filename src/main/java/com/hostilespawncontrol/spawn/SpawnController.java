package com.hostilespawncontrol.spawn;

import com.hostilespawncontrol.HostileSpawnControl;
import com.hostilespawncontrol.registry.HostileMobRegistry;
import com.hostilespawncontrol.rule.SpawnRuleStore;
import com.hostilespawncontrol.rule.SpawnSource;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/** Decides, before an entity is created, whether a spawn attempt may proceed. */
public final class SpawnController {
	private static final Map<SpawnSource, AtomicLong> BLOCKED = new EnumMap<>(SpawnSource.class);

	static {
		for (SpawnSource source : SpawnSource.values()) {
			BLOCKED.put(source, new AtomicLong());
		}
	}

	private SpawnController() {
	}

	public static boolean isAllowed(EntityType<?> type, EntitySpawnReason reason) {
		if (!HostileMobRegistry.isHostile(type)) {
			return true;
		}

		SpawnSource source = SpawnSource.from(reason);
		if (source == null || SpawnRuleStore.get().isAllowed(HostileMobRegistry.id(type), source)) {
			return true;
		}

		long count = BLOCKED.get(source).incrementAndGet();
		if (HostileSpawnControl.LOGGER.isDebugEnabled()) {
			HostileSpawnControl.LOGGER.debug("Blocked {} spawn of {} ({} blocked for {})", reason, HostileMobRegistry.id(type), count, source.key());
		}
		return false;
	}

	public static long blockedCount(SpawnSource source) {
		return BLOCKED.get(source).get();
	}

	public static long blockedCount() {
		return BLOCKED.values().stream().mapToLong(AtomicLong::get).sum();
	}
}
