package com.hostilespawncontrol.rule;

import java.util.Locale;
import net.minecraft.world.entity.EntitySpawnReason;
import org.jspecify.annotations.Nullable;

/**
 * Ways an entity can come into the world. The rule model is "mob x source" from the start;
 * V1 only exposes and enforces {@link #NATURAL}.
 */
public enum SpawnSource {
	NATURAL(true),
	SPAWNER(false),
	STRUCTURE(false),
	EVENT(false),
	CONVERSION(false),
	SPAWN_EGG(false),
	COMMAND(false);

	private final boolean enabledInV1;

	SpawnSource(boolean enabledInV1) {
		this.enabledInV1 = enabledInV1;
	}

	public boolean enabledInV1() {
		return this.enabledInV1;
	}

	public String key() {
		return this.name().toLowerCase(Locale.ROOT);
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
			case EVENT, PATROL -> EVENT;
			case CONVERSION -> CONVERSION;
			case SPAWN_ITEM_USE, DISPENSER -> SPAWN_EGG;
			case COMMAND -> COMMAND;
			default -> null;
		};
	}
}
