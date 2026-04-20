package com.github.litermc.vsmecha.command;

import com.github.litermc.vsmecha.Constants;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.arguments.ShipArgument;

import java.util.Set;

public final class VSMechaCommands {
	public static final String ROOT_LITERAL = Constants.MOD_ID;

	private VSMechaCommands() {}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		// dispatcher.register(Commands.literal(ROOT_LITERAL)
		// 	.requires((source) -> source.hasPermission(2))
		// 	.then(Commands.literal("delete")
		// 		.then(Commands.argument("ships", ShipArgument.Companion.ships())
		// 			.executes(VSMechaCommands::delete)
		// 		)
		// 	)
		// );
	}

	// private static int delete(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
	// 	final CommandSourceStack source = context.getSource();
	// 	final MinecraftServer server = source.getServer();
	// 	final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>)((CommandContext<?>)(context)), "ships");
	// 	int successCount = 0;
	// 	for (final Ship ship : ships) {
	// 		if (!(ship instanceof ServerShip serverShip)) {
	// 			continue;
	// 		}
	// 		final ServerLevel level = Utils.getLevel(serverShip.getChunkClaimDimension());
	// 		if (level == null) {
	// 			continue;
	// 		}
	// 		ShipAllocator.get(level).putShip(serverShip);
	// 		successCount++;
	// 	}
	// 	final int finalSuccessCount = successCount;
	// 	source.sendSuccess(() ->
	// 		Component.translatable("command.valkyrienskies.delete.success", finalSuccessCount),
	// 		true
	// 	);
	// 	return finalSuccessCount;
	// }
}
