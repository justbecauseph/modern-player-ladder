package town.lampas.modernplayerladder.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import town.lampas.modernplayerladder.ladder.PlayerLadderState;

public final class PlayerLadderCommands {

    private PlayerLadderCommands() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register(PlayerLadderCommands::registerCommands);
    }

    public static void registerCommands(
        CommandDispatcher<CommandSourceStack> dispatcher,
        CommandBuildContext buildContext,
        Commands.CommandSelection selection
    ) {
        dispatcher.register(
            Commands.literal("ladder")
                .then(Commands.literal("toggle")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return executeToggle(player);
                    })
                )
        );

        dispatcher.register(
            Commands.literal("playerladder")
                .then(Commands.literal("toggle")
                    .executes(context -> {
                        ServerPlayer player = context.getSource().getPlayerOrException();
                        return executeToggle(player);
                    })
                )
        );
    }

    public static int executeToggle(ServerPlayer player) {
        boolean newState = PlayerLadderState.toggle(player);
        if (newState) {
            player.sendSystemMessage(
                Component.translatable("modern_player_ladder.message.enabled")
                    .withStyle(ChatFormatting.GREEN)
            );
        } else {
            player.sendSystemMessage(
                Component.translatable("modern_player_ladder.message.disabled")
                    .withStyle(ChatFormatting.RED)
            );

            // Force dismount any direct passengers
            if (player.isVehicle()) {
                for (Entity passenger : player.getPassengers()) {
                    passenger.stopRiding();
                }
            }

            // Force dismount player if they are a passenger
            if (player.isPassenger()) {
                player.stopRiding();
            }
        }
        return 1;
    }
}
