package com.hostilespawncontrol;

import com.hostilespawncontrol.command.SpawnControlCommand;
import com.hostilespawncontrol.rule.SpawnRuleStore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostileSpawnControl implements ModInitializer {
	public static final String MOD_ID = "hostile_spawn_control";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SpawnRuleStore.init(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> SpawnControlCommand.register(dispatcher));
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> SpawnRuleStore.get().save());
	}
}
