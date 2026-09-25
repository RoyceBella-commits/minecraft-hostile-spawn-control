package com.hostilespawncontrol.mixin;

import com.hostilespawncontrol.spawn.SpawnController;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fallback hook for natural spawns that skip SpawnPlacements (e.g. PhantomSpawner).
 * Returning null here means the entity is never constructed; vanilla callers handle null.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> {
	@Inject(
		method = "create(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnRequest;)Lnet/minecraft/world/entity/Entity;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void hostileSpawnControl$create(Level level, EntitySpawnRequest request, CallbackInfoReturnable<T> cir) {
		if (!level.isClientSide() && !SpawnController.isAllowed((EntityType<?>) (Object) this, request.reason())) {
			cir.setReturnValue(null);
		}
	}
}
