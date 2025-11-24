package shake1227.betterroulette.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import shake1227.betterroulette.common.entity.RouletteEntity;

public class SpinCommand {
    public SpinCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("roulette")
                .then(Commands.literal("spin")
                        .then(Commands.argument("entityId", IntegerArgumentType.integer())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    int entityId = IntegerArgumentType.getInteger(context, "entityId");
                                    if (player.level().getEntity(entityId) instanceof RouletteEntity roulette) {
                                        roulette.playerAttemptSpin(player);
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}