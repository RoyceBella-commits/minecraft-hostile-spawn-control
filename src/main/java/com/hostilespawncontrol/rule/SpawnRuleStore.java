package com.hostilespawncontrol.rule;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hostilespawncontrol.HostileSpawnControl;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.Identifier;

/**
 * Holds every mob's spawn rules and persists them as JSON.
 * Only non-default entries are stored, so an empty/missing file means vanilla behaviour.
 */
public final class SpawnRuleStore {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int FORMAT_VERSION = 1;
	private static SpawnRuleStore instance;

	private final Path file;
	private final Map<Identifier, MobSpawnRule> rules = new ConcurrentHashMap<>();

	private SpawnRuleStore(Path file) {
		this.file = file;
	}

	public static void init(Path file) {
		instance = new SpawnRuleStore(file);
		instance.load();
	}

	public static SpawnRuleStore get() {
		return instance;
	}

	public boolean isAllowed(Identifier mob, SpawnSource source) {
		MobSpawnRule rule = this.rules.get(mob);
		return rule == null || rule.isAllowed(source);
	}

	public void set(Identifier mob, SpawnSource source, boolean allowed) {
		MobSpawnRule rule = this.rules.computeIfAbsent(mob, id -> new MobSpawnRule());
		rule.set(source, allowed);
		if (rule.isDefault()) {
			this.rules.remove(mob);
		}
	}

	public synchronized void load() {
		this.rules.clear();
		if (!Files.exists(this.file)) {
			return;
		}

		try (Reader reader = Files.newBufferedReader(this.file, StandardCharsets.UTF_8)) {
			JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
			JsonObject mobs = root.has("mobs") ? root.getAsJsonObject("mobs") : new JsonObject();
			for (Map.Entry<String, JsonElement> mob : mobs.entrySet()) {
				Identifier id = Identifier.tryParse(mob.getKey());
				if (id == null || !mob.getValue().isJsonObject()) {
					HostileSpawnControl.LOGGER.warn("Ignoring invalid entry '{}' in {}", mob.getKey(), this.file);
					continue;
				}
				for (Map.Entry<String, JsonElement> entry : mob.getValue().getAsJsonObject().entrySet()) {
					SpawnSource source = SpawnSource.byKey(entry.getKey());
					if (source == null) {
						HostileSpawnControl.LOGGER.warn("Unknown spawn source '{}' for {}", entry.getKey(), id);
						continue;
					}
					this.set(id, source, entry.getValue().getAsBoolean());
				}
			}
			HostileSpawnControl.LOGGER.info("Loaded spawn rules for {} mob(s) from {}", this.rules.size(), this.file);
		} catch (Exception e) {
			HostileSpawnControl.LOGGER.error("Failed to read {}, using defaults (all spawns allowed)", this.file, e);
			this.rules.clear();
		}
	}

	public synchronized void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", FORMAT_VERSION);
		JsonObject mobs = new JsonObject();
		Map<String, MobSpawnRule> sorted = new TreeMap<>();
		this.rules.forEach((id, rule) -> sorted.put(id.toString(), rule));
		sorted.forEach((id, rule) -> {
			JsonObject sources = new JsonObject();
			rule.snapshot().forEach((source, allowed) -> sources.addProperty(source.key(), allowed));
			if (!sources.isEmpty()) {
				mobs.add(id, sources);
			}
		});
		root.add("mobs", mobs);

		try {
			Files.createDirectories(this.file.getParent());
			Path tmp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
				GSON.toJson(root, writer);
			}
			Files.move(tmp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			HostileSpawnControl.LOGGER.error("Failed to save {}", this.file, e);
		}
	}
}
