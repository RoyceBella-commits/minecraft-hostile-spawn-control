package com.hostilespawncontrol.mixin;

import com.hostilespawncontrol.spawn.SpawnController;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Main hook: NaturalSpawner checks spawn rules before it creates the entity. */
@Mixin(SpawnPlacements.class)
public abstract class SpawnPlacementsMixin {
	@Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
	private static <T extends Entity> void hostileSpawnControl$checkSpawnRules(
		EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random,
		CallbackInfoReturnable<Boolean> cir
	) {
		if (!SpawnController.isAllowed(type, reason)) {
			cir.setReturnValue(false);
		}
	}
}
