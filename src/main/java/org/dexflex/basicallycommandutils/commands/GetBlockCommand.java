package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class GetBlockCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("getblock")
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .executes(ctx -> execute(ctx.getSource(), BlockPosArgumentType.getBlockPos(ctx, "pos"), true))
                        .then(CommandManager.argument("include_blockstate", BoolArgumentType.bool())
                                .executes(ctx -> execute(
                                        ctx.getSource(),
                                        BlockPosArgumentType.getBlockPos(ctx, "pos"),
                                        BoolArgumentType.getBool(ctx, "include_blockstate")
                                ))
                        )
                )
        );
    }

    private static int execute(ServerCommandSource source, BlockPos pos, boolean includeState) {
        var world = source.getWorld();
        BlockState state = world.getBlockState(pos);

        String blockStr = includeState
                ? state.toString()
                : state.getBlock().getTranslationKey().replace("block.minecraft.", "minecraft:");

        MutableText message = Text.literal("Copied: " + blockStr)
                .styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, blockStr))
                        .withColor(0x55FFFF)
                );

        source.sendFeedback(() -> message, false);
        return 1;
    }
}
