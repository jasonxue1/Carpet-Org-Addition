package org.carpetorgaddition.wheel.screen;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
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
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Optional;

public class ShipExpressScreenHandler extends ChestMenu {
    private final Container inventory;
    private final ExpressManager expressManager;
    private final MinecraftServer server;
    private final ServerPlayer sourcePlayer;
    private final GameProfile recipient;

    public ShipExpressScreenHandler(
            int syncId,
            Inventory playerInventory,
            ServerPlayer sourcePlayer,
            GameProfile recipient,
            Container inventory
    ) {
        super(MenuType.GENERIC_9x3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.server = FetcherUtils.getServer(sourcePlayer);
        this.expressManager = ServerComponentCoordinator.getCoordinator(this.server).getExpressManager();
        this.sourcePlayer = sourcePlayer;
        this.recipient = recipient;
    }

    @Override
    public void removed(@NonNull Player player) {
        super.removed(player);
        if (this.inventory.isEmpty()) {
            return;
        }
        AutoGrowInventory autoGrowInventory = new AutoGrowInventory();
        // 合并可堆叠的物品
        for (int i = 0; i < this.inventory.getContainerSize(); i++) {
            ItemStack itemStack = this.inventory.getItem(i);
            // 不要发送空气物品
            if (itemStack.isEmpty()) {
                continue;
            }
            autoGrowInventory.addStack(itemStack);
        }
        // 发送物品
        for (int i = 0; i < autoGrowInventory.getContainerSize(); i++) {
            ItemStack stack = autoGrowInventory.getItem(i);
            // 前面已经对物品进行了整理，所以遇到空物品时，说明物品已发送完毕
            if (stack.isEmpty()) {
                break;
            }
            Express express = new Express(this.server, this.sourcePlayer, this.recipient, stack, this.expressManager.generateNumber());
            try {
                expressManager.putNoMessage(express);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("批量发送物品时遇到意外错误", e);
                MessageUtils.sendErrorMessage(this.sourcePlayer.createCommandSourceStack(), e, "carpet.commands.mail.multiple.error");
                return;
            }
        }
        sendFeedback(autoGrowInventory);
    }

    // 发送命令反馈
    public void sendFeedback(AutoGrowInventory inventory) {
        ItemStack firstStack = inventory.getItem(0);
        // 定义变量记录查找状态
        // 如果为0，表示物品栏里只有一种物品，并且NBT也相同
        // 如果为1，表示物品栏里只有一种物品，但是NBT不相同
        // 如果为2，表示物品栏里有多种物品，不考虑NBT
        int onlyOneKind = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            // 比较物品和物品NBT
            if (onlyOneKind != 0 && ItemStack.matches(firstStack, stack)) {
                continue;
            }
            onlyOneKind = 1;
            // 只比较物品
            if (firstStack.is(stack.getItem())) {
                continue;
            }
            onlyOneKind = 2;
            break;
        }
        Optional<ServerPlayer> optional = GenericUtils.getPlayer(this.server, this.recipient);
        Component playerName = optional.map(Player::getDisplayName).orElse(TextBuilder.create(this.recipient.name()));
        Component command = TextProvider.clickRun(CommandProvider.cancelAllExpress());
        int count = inventory.count();
        Object[] args = switch (onlyOneKind) {
            case 0 -> {
                Component itemCount = TextProvider.itemCount(count, firstStack.getMaxStackSize());
                yield new Object[]{playerName, itemCount, firstStack.getDisplayName(), command};
            }
            case 1 -> {
                // 物品名称
                Component hoverableText = firstStack.getItem().getDefaultInstance().getDisplayName();
                // 物品堆叠数
                Component itemCount = TextProvider.itemCount(count, firstStack.getMaxStackSize());
                yield new Object[]{playerName, itemCount, hoverableText, command};
            }
            case 2 -> {
                // 不显示物品堆叠组数，但鼠标悬停可以显示物品栏
                Component itemText = TextBuilder.translate("carpet.command.item.item");
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
            ServerPlayer player = optional.get();
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
                    ((Component) args[1]).getString(),
                    firstStack.getHoverName().getString());
        }
    }
}
