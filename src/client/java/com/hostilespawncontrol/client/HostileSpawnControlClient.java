package com.hostilespawncontrol.client;

import com.hostilespawncontrol.HostileSpawnControl;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class HostileSpawnControlClient implements ClientModInitializer {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
		Identifier.fromNamespaceAndPath(HostileSpawnControl.MOD_ID, "main")
	);
	private static KeyMapping openScreenKey;

	@Override
	public void onInitializeClient() {
		openScreenKey = KeyMappingHelper.registerKeyMapping(
			new KeyMapping("key.hostile_spawn_control.open", InputConstants.KEY_K, CATEGORY)
		);

		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (openScreenKey.consumeClick()) {
				if (minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new SpawnControlScreen(null));
				}
			}
		});
	}
}
