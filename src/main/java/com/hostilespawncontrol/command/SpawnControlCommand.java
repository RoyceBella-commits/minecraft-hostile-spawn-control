package com.hostilespawncontrol.command;

import com.hostilespawncontrol.registry.HostileMobRegistry;
import com.hostilespawncontrol.rule.SpawnRuleStore;
import com.hostilespawncontrol.rule.SpawnSource;
import com.hostilespawncontrol.spawn.SpawnController;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/**
 * /spawncontrol list | set &lt;mob&gt; natural &lt;true|false&gt; | enableall | disableall | reload | status
 */
public final class SpawnControlCommand {
	private static final DynamicCommandExceptionType NOT_HOSTILE = new DynamicCommandExceptionType(
		id -> Component.translatableWithFallback("commands.hostile_spawn_control.not_hostile", "%s is not a hostile mob", id)
	);

	private SpawnControlCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(
			Commands.literal("spawncontrol")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("list").executes(SpawnControlCommand::list))
				.then(Commands.literal("status").executes(SpawnControlCommand::status))
				.then(Commands.literal("enableall").executes(ctx -> setAll(ctx, true)))
				.then(Commands.literal("disableall").executes(ctx -> setAll(ctx, false)))
				.then(Commands.literal("reload").executes(SpawnControlCommand::reload))
				.then(
					Commands.literal("set")
						.then(
							Commands.argument("mob", IdentifierArgument.id())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(HostileMobRegistry.ids(), builder))
								.then(
									Commands.literal(SpawnSource.NATURAL.key())
										.then(
											Commands.argument("enabled", BoolArgumentType.bool())
												.executes(ctx -> set(ctx, IdentifierArgument.getId(ctx, "mob"), BoolArgumentType.getBool(ctx, "enabled")))
										)
								)
						)
				)
		);
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		SpawnRuleStore store = SpawnRuleStore.get();
		ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.hostile_spawn_control.list.header", "Hostile mobs (%s) - natural spawning:", HostileMobRegistry.all().size()), false);
		for (EntityType<?> type : HostileMobRegistry.all()) {
			Identifier id = HostileMobRegistry.id(type);
			boolean allowed = store.isAllowed(id, SpawnSource.NATURAL);
			MutableComponent line = Component.empty()
				.append(type.getDescription())
				.append(Component.literal(" (" + id + ") ").withStyle(ChatFormatting.GRAY))
				.append(stateText(allowed));
			ctx.getSource().sendSuccess(() -> line, false);
		}
		return HostileMobRegistry.all().size();
	}

	private static int status(CommandContext<CommandSourceStack> ctx) {
		SpawnRuleStore store = SpawnRuleStore.get();
		long disabled = HostileMobRegistry.ids().stream().filter(id -> !store.isAllowed(id, SpawnSource.NATURAL)).count();
		long blocked = SpawnController.blockedCount();
		ctx.getSource().sendSuccess(
			() -> Component.translatableWithFallback("commands.hostile_spawn_control.status", "%s hostile mobs, %s with natural spawning disabled, %s spawn attempts blocked since start", HostileMobRegistry.all().size(), disabled, blocked), false
		);
		return (int) disabled;
	}

	private static int set(CommandContext<CommandSourceStack> ctx, Identifier id, boolean enabled) throws CommandSyntaxException {
		EntityType<?> type = HostileMobRegistry.all().stream().filter(t -> HostileMobRegistry.id(t).equals(id)).findFirst().orElse(null);
		if (type == null) {
			throw NOT_HOSTILE.create(id.toString());
		}

		SpawnRuleStore store = SpawnRuleStore.get();
		store.set(id, SpawnSource.NATURAL, enabled);
		store.save();
		ctx.getSource().sendSuccess(
			() -> Component.translatableWithFallback("commands.hostile_spawn_control.set", "Natural spawning of %s: %s", type.getDescription(), stateText(enabled)), true
		);
		return 1;
	}

	private static int setAll(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		SpawnRuleStore store = SpawnRuleStore.get();
		for (Identifier id : HostileMobRegistry.ids()) {
			store.set(id, SpawnSource.NATURAL, enabled);
		}
		store.save();
		int count = HostileMobRegistry.all().size();
		ctx.getSource().sendSuccess(
			() -> enabled
				? Component.translatableWithFallback("commands.hostile_spawn_control.enableall", "Natural spawning enabled for all %s hostile mobs", count)
				: Component.translatableWithFallback("commands.hostile_spawn_control.disableall", "Natural spawning disabled for all %s hostile mobs", count), true
		);
		return count;
	}

	private static int reload(CommandContext<CommandSourceStack> ctx) {
		SpawnRuleStore.get().load();
		ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.hostile_spawn_control.reload", "Spawn rules reloaded from config"), true);
		return 1;
	}

	public static Component stateText(boolean allowed) {
		return allowed
			? Component.translatableWithFallback("hostile_spawn_control.state.on", "ON").withStyle(ChatFormatting.GREEN)
			: Component.translatableWithFallback("hostile_spawn_control.state.off", "OFF").withStyle(ChatFormatting.RED);
	}
}
