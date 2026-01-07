package boat.carpetorgaddition.periodic.parcel;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.CommandRegister;
import boat.carpetorgaddition.command.MailCommand;
import boat.carpetorgaddition.dataupdate.nbt.ParcelDataUpdater;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.nbt.NbtVersion;
import boat.carpetorgaddition.wheel.nbt.NbtWriter;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import static boat.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask.UNKNOWN;

/**
 * 快递
 */
public class Parcel implements Comparable<Parcel> {
    /**
     * 寄件人
     */
    private final String sender;
    /**
     * 收件人
     */
    private final String recipient;
    /**
     * 快递的内容
     */
    private final ItemStack parcel;
    /**
     * 快递是否已被撤回
     */
    private boolean recall = false;
    /**
     * 快递单号
     */
    private final int id;
    /**
     * 物品接收者的UUID
     */
    @Nullable
    private final UUID uuid;
    private final MinecraftServer server;
    private final LocalDateTime time;
    private final WorldFormat worldFormat;
    public static final String EXPRESS = "express";
    private static final NbtVersion CURRENT_VERSION = new NbtVersion(3, 0);

    public Parcel(MinecraftServer server, ServerPlayer sender, ServerPlayer recipient, int id) throws CommandSyntaxException {
        this(server, sender, recipient.getGameProfile(), getPlayerHandStack(sender), id);
    }

    public Parcel(MinecraftServer server, ServerPlayer sender, GameProfile gameProfile, ItemStack itemStack, int id) {
        this(server, FetcherUtils.getPlayerName(sender), gameProfile.name(), gameProfile.id(), itemStack, id, LocalDateTime.now());
    }

    public Parcel(MinecraftServer server, ServerPlayer sender, GameProfile gameProfile, int id) throws CommandSyntaxException {
        this(server, sender, gameProfile, getPlayerHandStack(sender), id);
    }

    private Parcel(MinecraftServer server, String sender, String recipient, @Nullable UUID uuid, ItemStack parcel, int id, LocalDateTime time) {
        this.server = server;
        this.sender = sender;
        this.recipient = recipient;
        this.uuid = uuid;
        this.parcel = parcel;
        this.id = id;
        this.time = time;
        this.worldFormat = new WorldFormat(server, EXPRESS);
    }

    private static ItemStack getPlayerHandStack(ServerPlayer player) throws CommandSyntaxException {
        ItemStack mainHandStack = player.getMainHandItem();
        if (mainHandStack.isEmpty()) {
            ItemStack offHandStack = player.getOffhandItem();
            if (offHandStack.isEmpty()) {
                throw CommandUtils.createException(MailCommand.SEND.then("prompt").translate());
            }
            return offHandStack.copyAndClear();
        } else {
            return mainHandStack.copyAndClear();
        }
    }

    /**
     * 发送快递
     */
    public void send() {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer senderPlayer = playerManager.getPlayerByName(this.sender);
        ServerPlayer recipientPlayer = playerManager.getPlayerByName(this.recipient);
        if (senderPlayer == null) {
            CarpetOrgAddition.LOGGER.error("The express delivery is sent by non-existent player");
            return;
        }
        // 向快递发送者发送发出快递的消息
        Component cancelText = TextProvider.clickRun(CommandProvider.recallExpress(this.getId(), false));
        Object[] senderArray = {recipientPlayer == null ? this.recipient : recipientPlayer.getDisplayName(), this.parcel.getCount(), this.parcel.getDisplayName(), cancelText};
        LocalizationKey key = MailCommand.SEND;
        MessageUtils.sendMessage(senderPlayer, key.then("sender").translate(senderArray));
        // 向快递接受者发送发出快递的消息
        Component receiveText = TextProvider.clickRun(CommandProvider.collectExpress(this.getId(), false));
        Object[] recipientArray = {senderPlayer.getDisplayName(), this.parcel.getCount(), this.parcel.getDisplayName(), receiveText};
        if (recipientPlayer == null) {
            TextBuilder builder = new TextBuilder(key.then("offline").translate());
            builder.setGrayItalic();
            MessageUtils.sendMessage(senderPlayer, builder.build());
        } else {
            MessageUtils.sendMessage(recipientPlayer, key.then("recipient").translate(recipientArray));
            // 在接收者位置播放音效
            playXpOrbPickupSound(recipientPlayer);
        }
        CarpetOrgAddition.LOGGER.info("{} sent {} {} to {}", this.sender, this.parcel.getCount(), this.parcel.getItem().getName().getString(), this.recipient);
    }

    /**
     * 接收快递
     */
    public void collect() throws IOException {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer player = playerManager.getPlayerByName(this.recipient);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("The player who received the package does not exist");
            return;
        }
        LocalizationKey key = MailCommand.COLLECT;
        if (this.recall) {
            // 快递已被撤回
            MessageUtils.sendMessage(player, key.then("recalled").translate());
            return;
        }
        int count = this.parcel.getCount();
        ItemStack copy = this.parcel.copy();
        Counter<Item> counter = new Counter<>();
        switch (insertStack(player, false)) {
            case COMPLETE -> {
                // 物品完全接收
                MessageUtils.sendMessage(player, key.then("success").translate(count, copy.getDisplayName()));
                counter.add(copy.getItem(), count);
                // 通知发送者物品已接收
                Function<ServerPlayer, Component> message = _ -> ParcelManager.getReceiveNotice(player, counter);
                this.sendMessageIfPlayerOnline(this.sender, message);
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.parcel.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player, key.then("partial_reception").translate(count - surplusCount, surplusCount));
                counter.add(copy.getItem(), count - surplusCount);
                // 通知发送者物品已接收
                Function<ServerPlayer, Component> message = _ -> ParcelManager.getReceiveNotice(player, counter);
                this.sendMessageIfPlayerOnline(this.sender, message);
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case FAIL -> MessageUtils.sendMessage(player, key.then("insufficient_capacity").translate());
        }
    }

    /**
     * 撤回快递
     */
    public void recall() throws IOException {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer player = playerManager.getPlayerByName(this.sender);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("The player who recall the package does not exist");
            return;
        }
        int count = this.parcel.getCount();
        ItemStack copy = this.parcel.copy();
        // 将快递内容放入物品栏
        final LocalizationKey key = MailCommand.RECALL;
        this.recall = true;
        switch (insertStack(player, true)) {
            case COMPLETE -> {
                MessageUtils.sendMessage(player, key.then("success").translate(count, copy.getDisplayName()));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.parcel.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player, key.then("partial_reception").translate(count - surplusCount, surplusCount));
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            // 物品未能成功放入物品栏
            case FAIL -> MessageUtils.sendMessage(player, key.then("insufficient_capacity").translate());
            default -> throw new IllegalStateException();
        }
        // 如果接收者存在，向接收者发送物品被撤回的消息
        Function<ServerPlayer, Component> message = _ -> new TextBuilder(MailCommand.NOTICE.then("recall").translate(player.getDisplayName())).setGrayItalic().build();
        this.sendMessageIfPlayerOnline(this.recipient, message);
    }

    /**
     * 拦截快递
     */
    public void intercept(ServerPlayer operator) throws IOException {
        int count = this.parcel.getCount();
        ItemStack copy = this.parcel.copy();
        LocalizationKey key = MailCommand.INTERCEPT;
        switch (insertStack(operator, false)) {
            case COMPLETE -> {
                MessageUtils.sendMessage(operator, key.then("success").translate(count, copy.getDisplayName()));
                playItemPickupSound(operator);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.parcel.getCount();
                MessageUtils.sendMessage(operator, key.then("partial_reception").translate(count - surplusCount, surplusCount));
                playItemPickupSound(operator);
            }
            case FAIL -> {
                MessageUtils.sendMessage(operator, key.then("insufficient_capacity").translate());
                return;
            }
        }
        Component hover = TextBuilder.combineAll(copy.getItem().getName(), "*", count - this.parcel.getCount());
        LocalizationKey noticeKey = MailCommand.NOTICE.then("intercept");
        sendMessageIfPlayerOnline(this.sender, player -> {
            Component text = getOperatorPlayerName(operator, player);
            TextBuilder builder = new TextBuilder(noticeKey.then("sender").translate(text, this.recipient));
            builder.setGrayItalic();
            builder.setHover(hover);
            return builder.build();
        });
        sendMessageIfPlayerOnline(this.recipient, player -> {
            Component text = getOperatorPlayerName(operator, player);
            TextBuilder builder = new TextBuilder(noticeKey.then("recipient").translate(text, this.sender));
            builder.setGrayItalic();
            builder.setHover(hover);
            return builder.build();
        });
    }

    private Component getOperatorPlayerName(ServerPlayer operator, ServerPlayer player) {
        MailCommand instance = CommandRegister.getCommandInstance(MailCommand.class);
        return instance.getInterceptPredicate().test(player.createCommandSourceStack()) ? operator.getDisplayName() : LocalizationKeys.Misc.OPERATOR.translate();
    }

    /**
     * 播放物品拾取音效
     */
    public static void playItemPickupSound(@NotNull ServerPlayer player) {
        WorldUtils.playSound(player, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS);
    }

    /**
     * 播放经验球拾取音效
     */
    public static void playXpOrbPickupSound(@NotNull ServerPlayer player) {
        WorldUtils.playSound(player, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS);
    }

    /**
     * 接收每一件快递
     */
    public InsertResult receiveEach() throws IOException {
        ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.recipient);
        if (player == null) {
            throw new IllegalStateException("Cannot find player to receive the delivery");
        }
        return this.insertStack(player, false);
    }

    /**
     * 撤回每一件快递
     */
    public InsertResult recallEach() throws IOException {
        ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.sender);
        if (player == null) {
            throw new IllegalStateException("Cannot find player to recall the delivery");
        }
        return this.insertStack(player, false);
    }

    /**
     * 向物品栏里插入物品
     */
    private InsertResult insertStack(ServerPlayer player, boolean forceSave) throws IOException {
        int count = this.parcel.getCount();
        player.getInventory().add(this.parcel);
        // 物品没有插入
        if (count == this.parcel.getCount()) {
            if (forceSave) {
                this.save();
            }
            return InsertResult.FAIL;
        }
        // 物品完全插入
        if (this.parcel.isEmpty()) {
            // 删除NBT文件
            this.delete();
            return InsertResult.COMPLETE;
        }
        // 物品部分插入
        // 修改NBT文件
        this.save();
        return InsertResult.PART;
    }

    /**
     * 如果指定玩家存在，则向该玩家发送消息
     *
     * @param playerName 要查找的玩家名称
     * @param message    要发送的消息，使用Supplier包装，只在玩家存在时获取消息
     */
    private void sendMessageIfPlayerOnline(String playerName, Function<ServerPlayer, Component> message) {
        ServerPlayer player = this.server.getPlayerList().getPlayerByName(playerName);
        if (player == null) {
            return;
        }
        MessageUtils.sendMessage(player, message.apply(player));
    }

    /**
     * 检查接收方是否有足够的权限执行接收物品的命令，这不会阻止物品发送，而是提示发送者
     */
    public void checkRecipientPermission() {
        PlayerList playerManager = this.server.getPlayerList();
        // 物品接收者玩家
        ServerPlayer recipientPlayer = playerManager.getPlayerByName(this.recipient);
        if (recipientPlayer == null) {
            return;
        }
        // 对方没有结束物品的权限，提示发送者
        ServerPlayer senderPlayer = playerManager.getPlayerByName(this.sender);
        checkRecipientPermission(senderPlayer, recipientPlayer);
    }

    public static void checkRecipientPermission(@Nullable ServerPlayer senderPlayer, ServerPlayer recipientPlayer) {
        // 检查接收者是否有接收物品的权限
        if (CommandUtils.canUseCommand(recipientPlayer.createCommandSourceStack(), CarpetOrgAdditionSettings.commandMail)) {
            return;
        }
        if (senderPlayer == null) {
            return;
        }
        // 将消息设置为灰色斜体
        Component message = new TextBuilder(MailCommand.SEND.then("permission").translate()).setGrayItalic().build();
        MessageUtils.sendMessage(senderPlayer, message);
    }

    /**
     * 将快递信息保存到本地文件
     */
    public void save() throws IOException {
        File file = this.worldFormat.file(this.getId() + IOUtils.NBT_EXTENSION);
        CompoundTag nbt = this.writeNbt(this.server);
        NbtIo.write(nbt, file.toPath());
    }

    /**
     * 删除已经完成的快递
     */
    public void delete() {
        File file = this.worldFormat.file(this.getId() + IOUtils.NBT_EXTENSION);
        if (file.delete()) {
            return;
        }
        CarpetOrgAddition.LOGGER.warn("Failed to successfully delete file: {}", file);
    }

    /**
     * 完成寄件
     */
    public boolean isComplete() {
        return this.parcel.isEmpty();
    }

    /**
     * 将快递内容写入NBT
     */
    public CompoundTag writeNbt(MinecraftServer server) {
        NbtWriter writer = new NbtWriter(server, CURRENT_VERSION);
        writer.putString("sender", this.sender);
        writer.putString("recipient", this.recipient);
        if (this.uuid != null) {
            writer.putUuid("uuid", this.uuid);
        }
        writer.putBoolean("recall", this.recall);
        writer.putItemStack("item", this.parcel);
        writer.putLocalDateTime("time", this.time);
        return writer.toNbt();
    }

    /**
     * 从NBT读取快递信息
     */
    public static Parcel readNbt(MinecraftServer server, CompoundTag nbt, int id) {
        ParcelDataUpdater updater = new ParcelDataUpdater(server);
        NbtVersion version = ParcelDataUpdater.getVersion(nbt);
        int vanillaVersion = ParcelDataUpdater.getVanillaVersion(nbt);
        NbtReader reader = new NbtReader(server, updater.update(nbt, version, vanillaVersion));
        String sender = reader.getStringOrElse("sender", UNKNOWN);
        String recipient = reader.getStringOrElse("recipient", UNKNOWN);
        UUID uuid = reader.getUuidNullable("uuid").orElse(null);
        boolean recall = reader.getBooleanOrElse("recall", false);
        ItemStack stack = reader.getItemStack("item");
        LocalDateTime time = reader.getLocalDateTime("time");
        Parcel parcel = new Parcel(server, sender, recipient, uuid, stack, id, time);
        parcel.recall = recall;
        return parcel;
    }

    /**
     * @return 快递单号
     */
    public int getId() {
        return this.id;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public Component getTime() {
        return LocalizationKeys.Time.FORMAT.translate(
                this.time.getYear(),
                this.time.getMonthValue(),
                this.time.getDayOfMonth(),
                this.time.getHour(),
                this.time.getMinute(),
                this.time.getSecond()
        );
    }

    public ItemStack getParcel() {
        return parcel;
    }

    /**
     * @return 指定玩家是否是当前快递的发送者
     */
    public boolean isSender(ServerPlayer player) {
        return Objects.equals(this.sender, FetcherUtils.getPlayerName(player));
    }

    /**
     * @return 指定玩家是否是当前快递的接收者
     */
    public boolean isRecipient(ServerPlayer player) {
        return Objects.equals(this.recipient, FetcherUtils.getPlayerName(player));
    }

    /**
     * @return 快递是否已被撤回
     */
    public boolean isRecall() {
        return recall;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.isComplete()) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Parcel other = (Parcel) obj;
            if (other.isComplete()) {
                return false;
            }
            return this.id == other.id;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public int compareTo(@NotNull Parcel o) {
        return this.id - o.id;
    }

    /**
     * 向物品栏插入物品，返回插入结果
     */
    public enum InsertResult {
        /**
         * 物品完全插入物品栏
         */
        COMPLETE,
        /**
         * 物品部分插入物品栏
         */
        PART,
        /**
         * 物品没有插入物品栏
         */
        FAIL
    }
}
