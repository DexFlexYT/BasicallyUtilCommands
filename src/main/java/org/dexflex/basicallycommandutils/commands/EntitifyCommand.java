package org.dexflex.basicallycommandutils.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Arrays;

public class EntitifyCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("entitify")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                        .then(CommandManager.argument("entity_type", StringArgumentType.word())
                                .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                        Arrays.asList("falling_block", "block_display"/*, "item_display"*/), builder))
                                .executes(ctx -> execute(ctx, "keep", null))
                                .then(CommandManager.argument("keep_remove", StringArgumentType.word())
                                        .suggests((ctx, builder) -> CommandSource.suggestMatching(
                                                Arrays.asList("keep", "remove"), builder))
                                        .executes(ctx -> execute(ctx,
                                                StringArgumentType.getString(ctx, "keep_remove"), null))
                                        .then(CommandManager.argument("tag", StringArgumentType.greedyString())
                                                .executes(ctx -> execute(ctx,
                                                        StringArgumentType.getString(ctx, "keep_remove"),
                                                        StringArgumentType.getString(ctx, "tag"))))))));
    }

    private static int execute(CommandContext<ServerCommandSource> context, String keepRemove, String tag)
            throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        World world = source.getWorld();
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
        BlockState blockState = world.getBlockState(pos);
        if (blockState.isAir()) {
            source.sendError(Text.literal("No block found."));
            return 0;
        }

        Vec3d spawnPos = Vec3d.ofCenter(pos);
        String entityType = StringArgumentType.getString(context, "entity_type");
        String tagToUse = (tag == null || tag.isEmpty()) ? "BUC.entitified." + entityType : tag;
        //String blockId = Registries.BLOCK.getId(blockState.getBlock()).toString();
        NbtCompound stateTag = NbtHelper.fromBlockState(blockState);
        double x = spawnPos.x, y = spawnPos.y, z = spawnPos.z;

        if (entityType.equals("falling_block")) {
            String nbt = String.format(
                    "{BlockState:%s,Time:1b,NoGravity:1b,DropItem:0b,Tags:[\"%s\"]}",
                    stateTag, tagToUse
            );
            source.getServer()
                    .getCommandManager()
                    .getDispatcher()
                    .execute(String.format("summon falling_block %f %f %f %s", x, y, z, nbt), source.withSilent());
        } else if (entityType.equals("block_display")) {
            String nbt = String.format(
                    "{block_state:%s,Tags:[\"%s\"]}",
                    stateTag, tagToUse
            );
            source.getServer()
                    .getCommandManager()
                    .getDispatcher()
                    .execute(String.format("summon block_display %f %f %f %s", x, y, z, nbt), source.withSilent());
        }
        /*
        // Commented out item_display for now. Re-enable if needed later:
        else if (entityType.equals("item_display")) {
            String itemId = Registries.ITEM.getId(blockState.getBlock().asItem()).toString();
            String nbt = String.format(
                "{Item:{id:\"%s\",Count:1b},Tags:[\"%s\"]}",
                itemId, tagToUse
            );
            source.getServer()
                  .getCommandManager()
                  .getDispatcher()
                  .execute(String.format("summon item_display %f %f %f %s", x, y, z, nbt), source.withSilent());
        }
        */
        else {
            source.sendError(Text.literal("Invalid type. Use falling_block or block_display."));
            return 0;
        }

        if ("remove".equalsIgnoreCase(keepRemove)) {
            world.removeBlock(pos, false);
        }

        source.sendFeedback(() -> Text.literal("Entitified as " + entityType + " with tag '" + tagToUse + "'."), true);
        return 1;
    }
}
