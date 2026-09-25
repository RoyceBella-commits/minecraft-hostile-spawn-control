package com.hostilespawncontrol.spawn;

import com.hostilespawncontrol.HostileSpawnControl;
import com.hostilespawncontrol.registry.HostileMobRegistry;
import com.hostilespawncontrol.rule.SpawnRuleStore;
import com.hostilespawncontrol.rule.SpawnSource;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/** Decides, before an entity is created, whether a spawn attempt may proceed. */
public final class SpawnController {
	private static final AtomicLong BLOCKED = new AtomicLong();

	private SpawnController() {
	}

	public static boolean isAllowed(EntityType<?> type, EntitySpawnReason reason) {
		if (!HostileMobRegistry.isHostile(type)) {
			return true;
		}

		SpawnSource source = SpawnSource.from(reason);
		// V1 only enforces sources that are exposed to the player; everything else keeps vanilla behaviour.
		if (source == null || !source.enabledInV1()) {
			return true;
		}

		if (SpawnRuleStore.get().isAllowed(HostileMobRegistry.id(type), source)) {
			return true;
		}

		long count = BLOCKED.incrementAndGet();
		if (HostileSpawnControl.LOGGER.isDebugEnabled()) {
			HostileSpawnControl.LOGGER.debug("Blocked {} spawn of {} (total blocked: {})", reason, HostileMobRegistry.id(type), count);
		}
		return false;
	}

	public static long blockedCount() {
		return BLOCKED.get();
	}
}
