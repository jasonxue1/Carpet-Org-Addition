package boat.carpetorgaddition.wheel.screen;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.command.MailCommand;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.parcel.Parcel;
import boat.carpetorgaddition.periodic.parcel.ParcelManager;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.inventory.AutoGrowInventory;
import boat.carpetorgaddition.wheel.inventory.ImmutableInventory;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.authlib.GameProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Optional;

public class SendExpressScreenHandler extends ChestMenu {
    private final Container inventory;
    private final ParcelManager parcelManager;
    private final MinecraftServer server;
    private final ServerPlayer sourcePlayer;
    private final GameProfile recipient;
    private static final LocalizationKey SEND = MailCommand.SEND.then("multiple");
    private static final LocalizationKey COLLECT = MailCommand.COLLECT.then("multiple");

    public SendExpressScreenHandler(
            int syncId,
            Inventory playerInventory,
            ServerPlayer sourcePlayer,
            GameProfile recipient,
            Container inventory
    ) {
        super(MenuType.GENERIC_9x3, syncId, playerInventory, inventory, 3);
        this.inventory = inventory;
        this.server = FetcherUtils.getServer(sourcePlayer);
        this.parcelManager = ServerComponentCoordinator.getCoordinator(this.server).getParcelManager();
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
            Parcel parcel = new Parcel(this.server, this.sourcePlayer, this.recipient, stack, this.parcelManager.generateNumber());
            try {
                parcelManager.putNoMessage(parcel);
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.error("Encountered an unexpected error while batch sending items", e);
                CommandSourceStack source = this.sourcePlayer.createCommandSourceStack();
                MessageUtils.sendErrorMessage(source, SEND.then("error").translate(), e);
                return;
            }
        }
        sendFeedback(autoGrowInventory);
    }

    // 发送命令反馈
    public void sendFeedback(AutoGrowInventory inventory) {
        ItemStack firstStack = inventory.getItem(0);
        // 如果为0，表示物品栏里只有一种物品，并且物品堆叠组件也相同
        // 如果为1，表示物品栏里只有一种物品，但是物品堆叠组件不相同
        // 如果为2，表示物品栏里有多种物品，不考虑物品堆叠组件
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
        Component command = TextProvider.clickRun(CommandProvider.recallAllExpress());
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
                Component itemText = LocalizationKeys.Item.ITEM.translate();
                yield new Object[]{playerName, count, TextProvider.inventory(itemText, inventory), command};
            }
            default -> throw new IllegalStateException();
        };
        // 向物品发送者发送消息
        MessageUtils.sendMessage(this.sourcePlayer, SEND.translate(args));
        if (optional.isEmpty()) {
            TextBuilder builder = new TextBuilder(MailCommand.SEND.then("offline").translate());
            builder.setGrayItalic();
            MessageUtils.sendMessage(this.sourcePlayer, builder.build());
        } else {
            ServerPlayer player = optional.get();
            // 向物品接收者发送消息
            MessageUtils.sendMessage(player, COLLECT.translate(
                    this.sourcePlayer.getDisplayName(), args[1], args[2],
                    TextProvider.clickRun(CommandProvider.collectAllExpress())
            ));
            Parcel.playXpOrbPickupSound(player);
            Parcel.checkRecipientPermission(this.sourcePlayer, player);
        }
        // 日志输出
        if (onlyOneKind == 2) {
            CarpetOrgAddition.LOGGER.info("{} sent {} to {}",
                    FetcherUtils.getPlayerName(this.sourcePlayer),
                    new ImmutableInventory(inventory),
                    this.recipient);
        } else {
            CarpetOrgAddition.LOGGER.info("{} sent {} {} to {}",
                    FetcherUtils.getPlayerName(this.sourcePlayer),
                    ((Component) args[1]).getString(),
                    firstStack.getHoverName().getString(),
                    this.recipient);
        }
    }
}
