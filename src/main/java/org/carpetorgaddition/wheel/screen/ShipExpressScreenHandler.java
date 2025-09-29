package org.carpetorgaddition.wheel.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.express.Express;
import org.carpetorgaddition.periodic.express.ExpressManager;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import org.carpetorgaddition.wheel.inventory.ImmutableInventory;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.provider.TextProvider;

import java.io.IOException;
import java.util.Optional;

public class ShipExpressScreenHandler extends GenericContainerScreenHandler {
    private final Inventory inventory;
    private final ExpressManager expressManager;
    private final MinecraftServer server;
    private final ServerPlayerEntity sourcePlayer;
    private final String recipient;

    public ShipExpressScreenHandler(
            int syncId,
            PlayerInventory playerInventory,
            ServerPlayerEntity sourcePlayer,
            String recipient,
            Inventory inventory
    ) {
        super(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.server = FetcherUtils.getServer(sourcePlayer);
        this.expressManager = ServerComponentCoordinator.getCoordinator(this.server).getExpressManager();
        this.sourcePlayer = sourcePlayer;
        this.recipient = recipient;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (this.inventory.isEmpty()) {
            return;
        }
        AutoGrowInventory autoGrowInventory = new AutoGrowInventory();
        // 合并可堆叠的物品
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack itemStack = this.inventory.getStack(i);
            // 不要发送空气物品
            if (itemStack.isEmpty()) {
                continue;
            }
            autoGrowInventory.addStack(itemStack);
        }
        // 发送物品
        for (int i = 0; i < autoGrowInventory.size(); i++) {
            ItemStack stack = autoGrowInventory.getStack(i);
            // 前面已经对物品进行了整理，所以遇到空物品时，说明物品已发送完毕
            if (stack.isEmpty()) {
                break;
            }
            Express express = new Express(this.server, this.sourcePlayer, this.recipient, stack, this.expressManager.generateNumber());
            try {
                expressManager.putNoMessage(express);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("批量发送物品时遇到意外错误", e);
                MessageUtils.sendErrorMessage(this.sourcePlayer.getCommandSource(), e, "carpet.commands.mail.multiple.error");
                return;
            }
        }
        sendFeedback(autoGrowInventory);
    }

    // 发送命令反馈
    public void sendFeedback(AutoGrowInventory inventory) {
        ItemStack firstStack = inventory.getStack(0);
        // 定义变量记录查找状态
        // 如果为0，表示物品栏里只有一种物品，并且NBT也相同
        // 如果为1，表示物品栏里只有一种物品，但是NBT不相同
        // 如果为2，表示物品栏里有多种物品，不考虑NBT
        int onlyOneKind = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            // 比较物品和物品NBT
            if (onlyOneKind != 0 && ItemStack.areEqual(firstStack, stack)) {
                continue;
            }
            onlyOneKind = 1;
            // 只比较物品
            if (firstStack.isOf(stack.getItem())) {
                continue;
            }
            onlyOneKind = 2;
            break;
        }
        Optional<ServerPlayerEntity> optional = GenericUtils.getPlayer(this.server, this.recipient);
        Text playerName = optional.map(PlayerEntity::getDisplayName).orElse(TextBuilder.create(this.recipient));
        Text command = TextProvider.clickRun(CommandProvider.cancelAllExpress());
        int count = inventory.count();
        Object[] args = switch (onlyOneKind) {
            case 0 -> {
                Text itemCount = TextProvider.itemCount(count, firstStack.getMaxCount());
                yield new Object[]{playerName, itemCount, firstStack.toHoverableText(), command};
            }
            case 1 -> {
                // 物品名称
                Text hoverableText = firstStack.getItem().getDefaultStack().toHoverableText();
                // 物品堆叠数
                Text itemCount = TextProvider.itemCount(count, firstStack.getMaxCount());
                yield new Object[]{playerName, itemCount, hoverableText, command};
            }
            case 2 -> {
                // 不显示物品堆叠组数，但鼠标悬停可以显示物品栏
                Text itemText = TextBuilder.translate("carpet.command.item.item");
                yield new Object[]{playerName, count, TextProvider.inventory(itemText, inventory), command};
            }
            default -> throw new IllegalStateException();
        };
        // 向物品发送者发送消息
        MessageUtils.sendMessage(this.sourcePlayer, "carpet.commands.mail.sending.multiple", args);
        if (optional.isEmpty()) {
            TextBuilder builder = TextBuilder.of("carpet.commands.mail.sending.offline_player");
            builder.setGrayItalic();
            MessageUtils.sendMessage(this.sourcePlayer, builder.build());
        } else {
            ServerPlayerEntity player = optional.get();
            // 向物品接收者发送消息
            MessageUtils.sendMessage(player, "carpet.commands.mail.receive.multiple",
                    this.sourcePlayer.getDisplayName(), args[1], args[2],
                    TextProvider.clickRun(CommandProvider.receiveAllExpress())
            );
            Express.playXpOrbPickupSound(player);
            Express.checkRecipientPermission(this.sourcePlayer, player);
        }
        // 日志输出
        if (onlyOneKind == 2) {
            CarpetOrgAddition.LOGGER.info("{}向{}发送了{}",
                    FetcherUtils.getPlayerName(this.sourcePlayer),
                    this.recipient,
                    new ImmutableInventory(inventory));
        } else {
            CarpetOrgAddition.LOGGER.info("{}向{}发送了{}个{}",
                    FetcherUtils.getPlayerName(this.sourcePlayer),
                    this.recipient,
                    ((Text) args[1]).getString(),
                    firstStack.getName().getString());
        }
    }
}
