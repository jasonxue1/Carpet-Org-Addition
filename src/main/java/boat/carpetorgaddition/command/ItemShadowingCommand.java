package boat.carpetorgaddition.command;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.inventory.ImmutableInventory;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemShadowingCommand extends AbstractServerCommand {
    public ItemShadowingCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(Commands.literal(name)
                .requires(CommandUtils.canUseCommand(CarpetOrgAdditionSettings.commandItemShadowing))
                .executes(this::itemShadowing));
    }

    // 制作物品分身
    private int itemShadowing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = CommandUtils.getSourcePlayer(context);
        // 获取主副手上的物品
        // TODO 其中一只手为空即可制作
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.isEmpty()) {
            // 主手不能为空
            throw CommandUtils.createException("carpet.commands.itemshadowing.main_hand_is_empty");
        } else if (off.isEmpty()) {
            // 制作物品分身
            player.setItemInHand(InteractionHand.OFF_HAND, main);
            // 广播制作物品分身的消息
            MessageUtils.broadcastMessage(
                    context.getSource().getServer(),
                    TextBuilder.translate("carpet.commands.itemshadowing.broadcast", player.getDisplayName(), main.getDisplayName())
            );
            // 将玩家制作物品分身的消息写入日志
            Level world = FetcherUtils.getWorld(player);
            if (InventoryUtils.isShulkerBoxItem(main)) {
                // 获取潜影盒的物品栏
                ImmutableInventory inventory = InventoryUtils.getInventory(main);
                if (inventory.isEmpty()) {
                    CarpetOrgAddition.LOGGER.info("{}制作了一个空[{}]的物品分身，在{}，坐标:[{}]",
                            FetcherUtils.getPlayerName(player), main.getItem().getName().getString(),
                            WorldUtils.getDimensionId(world), WorldUtils.toPosString(player.blockPosition()));
                } else {
                    CarpetOrgAddition.LOGGER.info("{}制作了一个{}的物品分身，包含{}，在{}，坐标:[{}]",
                            FetcherUtils.getPlayerName(player), main.getItem().getName().getString(), inventory,
                            WorldUtils.getDimensionId(world), WorldUtils.toPosString(player.blockPosition()));
                }
            } else {
                CarpetOrgAddition.LOGGER.info("{}制作了一个[{}]的物品分身，在{}，坐标:[{}]",
                        FetcherUtils.getPlayerName(player), main.getItem().getName().getString(),
                        WorldUtils.getDimensionId(world), WorldUtils.toPosString(player.blockPosition()));
            }
            return 1;
        } else {
            // 副手必须为空
            throw CommandUtils.createException("carpet.commands.itemshadowing.off_hand_not_empty");
        }
    }

    @Override
    public String getDefaultName() {
        return "itemshadowing";
    }
}
