package com.hostilespawncontrol.command;

import com.hostilespawncontrol.registry.HostileMobRegistry;
import com.hostilespawncontrol.rule.SpawnRuleStore;
import com.hostilespawncontrol.rule.SpawnSource;
import com.hostilespawncontrol.spawn.SpawnController;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * /spawncontrol list | status | reload
 * /spawncontrol set &lt;mob&gt; &lt;source&gt; &lt;true|false&gt;
 * /spawncontrol enableall|disableall [source]
 */
public final class SpawnControlCommand {
	private static final DynamicCommandExceptionType NOT_HOSTILE = new DynamicCommandExceptionType(
		id -> Component.translatableWithFallback("commands.hostile_spawn_control.not_hostile", "%s is not a hostile mob", id)
	);
	private static final DynamicCommandExceptionType UNKNOWN_SOURCE = new DynamicCommandExceptionType(
		key -> Component.translatableWithFallback(
			"commands.hostile_spawn_control.unknown_source", "Unknown spawn source '%s' (expected one of: %s)", key, sourceKeys()
		)
	);

	private SpawnControlCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("spawncontrol")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("list").executes(SpawnControlCommand::list))
				.then(Commands.literal("status").executes(SpawnControlCommand::status))
				.then(Commands.literal("reload").executes(SpawnControlCommand::reload))
				.then(
					Commands.literal("enableall")
						.executes(ctx -> setAll(ctx, null, true))
						.then(sourceArgument().executes(ctx -> setAll(ctx, getSource(ctx), true)))
				)
				.then(
					Commands.literal("disableall")
						.executes(ctx -> setAll(ctx, null, false))
						.then(sourceArgument().executes(ctx -> setAll(ctx, getSource(ctx), false)))
				)
				.then(
					Commands.literal("set")
						.then(
							Commands.argument("mob", IdentifierArgument.id())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(HostileMobRegistry.ids(), builder))
								.then(
									sourceArgument()
										.then(
											Commands.argument("enabled", BoolArgumentType.bool())
												.executes(ctx -> set(ctx, IdentifierArgument.getId(ctx, "mob"), getSource(ctx), BoolArgumentType.getBool(ctx, "enabled")))
										)
								)
						)
				)
		);
	}

	private static RequiredArgumentBuilder<CommandSourceStack, String> sourceArgument() {
		return Commands.argument("source", StringArgumentType.word())
			.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(SpawnSource.values()).map(SpawnSource::key), builder));
	}

	private static SpawnSource getSource(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String key = StringArgumentType.getString(ctx, "source");
		SpawnSource source = SpawnSource.byKey(key);
		if (source == null) {
			throw UNKNOWN_SOURCE.create(key);
		}
		return source;
	}

	private static String sourceKeys() {
		return String.join(", ", Arrays.stream(SpawnSource.values()).map(SpawnSource::key).toList());
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		SpawnRuleStore store = SpawnRuleStore.get();
		ctx.getSource().sendSuccess(
			() -> Component.translatableWithFallback(
				"commands.hostile_spawn_control.list.header", "Hostile mobs (%s) - disabled spawn sources:", HostileMobRegistry.all().size()
			),
			false
		);
		for (EntityType<?> type : HostileMobRegistry.all()) {
			Identifier id = HostileMobRegistry.id(type);
			MutableComponent line = Component.empty()
				.append(type.getDescription())
				.append(Component.literal(" (" + id + ") ").withStyle(ChatFormatting.GRAY));
			if (!store.hasRestriction(id)) {
				line.append(Component.translatableWithFallback("commands.hostile_spawn_control.list.all_on", "all ON").withStyle(ChatFormatting.GREEN));
			} else {
				MutableComponent off = Component.empty().withStyle(ChatFormatting.RED);
				boolean first = true;
				for (SpawnSource source : SpawnSource.values()) {
					if (!store.isAllowed(id, source)) {
						if (!first) {
							off.append(", ");
						}
						off.append(sourceName(source));
						first = false;
					}
				}
				line.append(Component.translatableWithFallback("commands.hostile_spawn_control.list.off", "OFF: %s", off).withStyle(ChatFormatting.RED));
			}
			ctx.getSource().sendSuccess(() -> line, false);
		}
		return HostileMobRegistry.all().size();
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		SpawnRuleStore store = SpawnRuleStore.get();
		long restricted = HostileMobRegistry.ids().stream().filter(store::hasRestriction).count();
		ctx.getSource().sendSuccess(
			() -> Component.translatableWithFallback(
				"commands.hostile_spawn_control.status",
				"%s hostile mobs, %s with restrictions, %s spawn attempts blocked since start",
				HostileMobRegistry.all().size(),
				restricted,
				SpawnController.blockedCount()
			),
			false
		);
		for (SpawnSource source : SpawnSource.values()) {
			long disabled = HostileMobRegistry.ids().stream().filter(id -> !store.isAllowed(id, source)).count();
			long blocked = SpawnController.blockedCount(source);
			ctx.getSource().sendSuccess(
				() -> Component.empty()
					.append(Component.literal(" - ").withStyle(ChatFormatting.GRAY))
					.append(sourceName(source))
					.append(": ")
					.append(Component.translatableWithFallback(
						"commands.hostile_spawn_control.status.source", "%s mobs disabled, %s blocked", disabled, blocked
					).withStyle(ChatFormatting.GRAY)),
				false
			);
		}
		return (int) restricted;
	}

	private static int set(CommandContext<CommandSourceStack> ctx, Identifier id, SpawnSource source, boolean enabled) throws CommandSyntaxException {
		EntityType<?> type = HostileMobRegistry.all().stream().filter(t -> HostileMobRegistry.id(t).equals(id)).findFirst().orElse(null);
		if (type == null) {
			throw NOT_HOSTILE.create(id.toString());
		}

		SpawnRuleStore store = SpawnRuleStore.get();
		store.set(id, source, enabled);
		store.save();
		ctx.getSource().sendSuccess(
			() -> Component.translatableWithFallback(
				"commands.hostile_spawn_control.set", "%s - %s: %s", type.getDescription(), sourceName(source), stateText(enabled)
			),
			true
		);
		return 1;
	}

	private static int setAll(CommandContext<CommandSourceStack> ctx, @Nullable SpawnSource source, boolean enabled) {
		SpawnRuleStore store = SpawnRuleStore.get();
		store.setAll(HostileMobRegistry.ids(), source, enabled);
		store.save();
		int count = HostileMobRegistry.all().size();
		Component scope = source == null
			? Component.translatableWithFallback("commands.hostile_spawn_control.all_sources", "all spawn sources")
			: sourceName(source);
		ctx.getSource().sendSuccess(
			() -> Component.translatableWithFallback(
				"commands.hostile_spawn_control.setall", "%s for %s hostile mobs: %s", scope, count, stateText(enabled)
			),
			true
		);
		return count;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		SpawnRuleStore.get().load();
		ctx.getSource().sendSuccess(
			() -> Component.translatableWithFallback("commands.hostile_spawn_control.reload", "Spawn rules reloaded from config"), true
		);
		return 1;
	}

	public static Component sourceName(SpawnSource source) {
		return Component.translatableWithFallback(source.translationKey(), source.key());
	}

	public static Component stateText(boolean allowed) {
		return allowed
			? Component.translatableWithFallback("hostile_spawn_control.state.on", "ON").withStyle(ChatFormatting.GREEN)
			: Component.translatableWithFallback("hostile_spawn_control.state.off", "OFF").withStyle(ChatFormatting.RED);
	}
}
