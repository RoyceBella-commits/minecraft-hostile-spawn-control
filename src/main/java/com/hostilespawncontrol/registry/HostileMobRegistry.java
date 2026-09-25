package com.hostilespawncontrol.registry;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Identifies hostile mobs from the entity registry instead of a hand-maintained list:
 * every entity type whose spawn category is {@link MobCategory#MONSTER}.
 */
public final class HostileMobRegistry {
	private static volatile List<EntityType<?>> cached;

	private HostileMobRegistry() {
	}

	public static boolean isHostile(EntityType<?> type) {
		return type.getCategory() == MobCategory.MONSTER;
	}

	public static List<EntityType<?>> all() {
		List<EntityType<?>> list = cached;
		if (list == null) {
			list = BuiltInRegistries.ENTITY_TYPE.stream()
				.filter(HostileMobRegistry::isHostile)
				.sorted(Comparator.comparing(HostileMobRegistry::id))
				.toList();
			cached = list;
		}
		return list;
	}

	public static List<Identifier> ids() {
		return all().stream().map(HostileMobRegistry::id).toList();
	}

	public static Identifier id(EntityType<?> type) {
		return EntityType.getKey(type);
	}
}
