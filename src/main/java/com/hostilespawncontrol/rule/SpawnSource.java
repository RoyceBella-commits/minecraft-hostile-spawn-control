package com.hostilespawncontrol.rule;

import java.util.Locale;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/**
 * Ways an entity can come into the world. Each hostile mob has one allow/deny switch per source.
 * Reasons that map to no source (loading, dimension travel, breeding, reinforcements...) are never blocked.
 */
public enum SpawnSource {
	NATURAL,
	SPAWNER,
	STRUCTURE,
	EVENT,
	CONVERSION,
	SPAWN_EGG,
	COMMAND;

	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
	}

	public String translationKey() {
		return "hostile_spawn_control.source." + this.key();
	}

	public String descriptionKey() {
		return this.translationKey() + ".desc";
	}

	public static @Nullable SpawnSource byKey(String key) {
		for (SpawnSource source : values()) {
			if (source.key().equals(key)) {
				return source;
			}
		}
		return null;
	}

	public static @Nullable SpawnSource from(EntitySpawnReason reason) {
		return switch (reason) {
			case NATURAL, CHUNK_GENERATION -> NATURAL;
			case SPAWNER, TRIAL_SPAWNER -> SPAWNER;
			case STRUCTURE -> STRUCTURE;
			case EVENT, PATROL, TRIGGERED -> EVENT;
			case CONVERSION -> CONVERSION;
			case SPAWN_ITEM_USE, DISPENSER -> SPAWN_EGG;
			case COMMAND -> COMMAND;
			// LOAD and DIMENSION_TRAVEL are existing entities; the rest are mob-internal mechanics.
			default -> null;
		};
	}
}
