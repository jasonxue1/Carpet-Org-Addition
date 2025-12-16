package boat.carpetorgaddition.periodic.express;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.CommandRegister;
import boat.carpetorgaddition.command.MailCommand;
import boat.carpetorgaddition.dataupdate.DataUpdater;
import boat.carpetorgaddition.periodic.task.search.OfflinePlayerSearchTask;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.TextBuilder;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * 快递
 */
public class Express implements Comparable<Express> {
    private static final String NBT_DATA_VERSION = "NbtDataVersion";
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
    private final ItemStack express;
    /**
     * 快递是否已被撤回
     */
    private boolean cancel = false;
    /**
     * 快递单号
     */
    private final int id;
    @Nullable
    private final UUID uuid;
    private final MinecraftServer server;
    private final LocalDateTime time;
    private final WorldFormat worldFormat;
    private final int nbtDataVersion;
    public static final String EXPRESS = "express";

    public Express(MinecraftServer server, ServerPlayer sender, ServerPlayer recipient, int id) throws CommandSyntaxException {
        this(server, sender, recipient.getGameProfile(), getPlayerHandStack(sender), id);
    }

    public Express(MinecraftServer server, ServerPlayer sender, GameProfile gameProfile, ItemStack itemStack, int id) {
        this(server, FetcherUtils.getPlayerName(sender), gameProfile.name(), gameProfile.id(), itemStack, id, LocalDateTime.now(), GenericUtils.getNbtDataVersion());
    }

    public Express(MinecraftServer server, ServerPlayer sender, GameProfile gameProfile, int id) throws CommandSyntaxException {
        this(server, sender, gameProfile, getPlayerHandStack(sender), id);
    }

    private Express(MinecraftServer server, String sender, String recipient, @Nullable UUID uuid, ItemStack express, int id, LocalDateTime time, int nbtDataVersion) {
        this.server = server;
        this.sender = sender;
        this.recipient = recipient;
        this.uuid = uuid;
        this.express = express;
        this.id = id;
        this.time = time;
        this.worldFormat = new WorldFormat(server, EXPRESS);
        this.nbtDataVersion = nbtDataVersion;
    }

    private static ItemStack getPlayerHandStack(ServerPlayer player) throws CommandSyntaxException {
        ItemStack mainHandStack = player.getMainHandItem();
        if (mainHandStack.isEmpty()) {
            ItemStack offHandStack = player.getOffhandItem();
            if (offHandStack.isEmpty()) {
                throw CommandUtils.createException("carpet.commands.mail.structure");
            }
            return offHandStack.copyAndClear();
        } else {
            return mainHandStack.copyAndClear();
        }
    }

    /**
     * 发送快递
     */
    public void sending() {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer senderPlayer = playerManager.getPlayerByName(this.sender);
        ServerPlayer recipientPlayer = playerManager.getPlayerByName(this.recipient);
        if (senderPlayer == null) {
            CarpetOrgAddition.LOGGER.error("The express delivery is sent by non-existent player");
            return;
        }
        // 向快递发送者发送发出快递的消息
        Component cancelText = TextProvider.clickRun(CommandProvider.cancelExpress(this.getId(), false));
        Object[] senderArray = {recipientPlayer == null ? this.recipient : recipientPlayer.getDisplayName(), this.express.getCount(), this.express.getDisplayName(), cancelText};
        MessageUtils.sendMessage(senderPlayer, TextBuilder.translate("carpet.commands.mail.sending.sender", senderArray));
        // 向快递接受者发送发出快递的消息
        Component receiveText = TextProvider.clickRun(CommandProvider.receiveExpress(this.getId(), false));
        Object[] recipientArray = {senderPlayer.getDisplayName(), this.express.getCount(), this.express.getDisplayName(), receiveText};
        if (recipientPlayer == null) {
            TextBuilder builder = TextBuilder.of("carpet.commands.mail.sending.offline_player");
            builder.setGrayItalic();
            MessageUtils.sendMessage(senderPlayer, builder.build());
        } else {
            MessageUtils.sendMessage(recipientPlayer, TextBuilder.translate("carpet.commands.mail.sending.recipient", recipientArray));
            // 在接收者位置播放音效
            playXpOrbPickupSound(recipientPlayer);
        }
        CarpetOrgAddition.LOGGER.info("{}向{}发送了{}个{}", this.sender, this.recipient, this.express.getCount(), this.express.getHoverName().getString());
    }

    /**
     * 接收快递
     */
    public void receive() throws IOException {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer player = playerManager.getPlayerByName(this.recipient);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("The player who received the package does not exist");
            return;
        }
        if (this.cancel) {
            // 快递已被撤回
            MessageUtils.sendMessage(player, TextBuilder.translate("carpet.commands.mail.receive.cancel"));
            return;
        }
        int count = this.express.getCount();
        ItemStack copy = this.express.copy();
        Counter<Item> counter = new Counter<>();
        switch (insertStack(player)) {
            case COMPLETE -> {
                // 物品完全接收
                MessageUtils.sendMessage(player,
                        TextBuilder.translate("carpet.commands.mail.receive.success",
                                count, copy.getDisplayName()));
                counter.add(copy.getItem(), count);
                // 通知发送者物品已接收
                Function<ServerPlayer, Component> message = _ -> ExpressManager.getReceiveNotice(player, counter);
                this.sendMessageIfPlayerOnline(this.sender, message);
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.express.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player,
                        TextBuilder.translate("carpet.commands.mail.receive.partial_reception",
                                count - surplusCount, surplusCount));
                counter.add(copy.getItem(), count - surplusCount);
                // 通知发送者物品已接收
                Function<ServerPlayer, Component> message = _ -> ExpressManager.getReceiveNotice(player, counter);
                this.sendMessageIfPlayerOnline(this.sender, message);
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case FAIL ->
                    MessageUtils.sendMessage(player, TextBuilder.translate("carpet.commands.mail.receive.insufficient_capacity"));
        }
    }

    /**
     * 撤回快递
     */
    public void cancel() throws IOException {
        PlayerList playerManager = this.server.getPlayerList();
        ServerPlayer player = playerManager.getPlayerByName(this.sender);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("The player who withdrew the package does not exist");
            return;
        }
        int count = this.express.getCount();
        ItemStack copy = this.express.copy();
        // 将快递内容放入物品栏
        switch (insertStack(player)) {
            case COMPLETE -> {
                MessageUtils.sendMessage(player, "carpet.commands.mail.cancel.success", count, copy.getDisplayName());
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.express.getCount();
                // 物品部分放入物品栏
                MessageUtils.sendMessage(player, "carpet.commands.mail.cancel.partial_reception", count - surplusCount, surplusCount);
                // 播放物品拾取音效
                playItemPickupSound(player);
            }
            // 物品未能成功放入物品栏
            case FAIL -> MessageUtils.sendMessage(player, "carpet.commands.mail.cancel.insufficient_capacity");
            default -> throw new IllegalStateException();
        }
        // 如果接收者存在，向接收者发送物品被撤回的消息
        Function<ServerPlayer, Component> message = _ -> TextBuilder.of("carpet.commands.mail.cancel.notice", player.getDisplayName()).setGrayItalic().build();
        this.sendMessageIfPlayerOnline(this.recipient, message);
        this.cancel = true;
    }

    /**
     * 拦截快递
     */
    public void intercept(ServerPlayer operator) throws IOException {
        int count = this.express.getCount();
        ItemStack copy = this.express.copy();
        switch (insertStack(operator)) {
            case COMPLETE -> {
                MessageUtils.sendMessage(operator, "carpet.commands.mail.intercept.success", count, copy.getDisplayName());
                playItemPickupSound(operator);
            }
            case PART -> {
                // 剩余的物品数量
                int surplusCount = this.express.getCount();
                MessageUtils.sendMessage(operator, "carpet.commands.mail.intercept.partial_reception", count - surplusCount, surplusCount);
                playItemPickupSound(operator);
            }
            case FAIL -> {
                MessageUtils.sendMessage(operator, "carpet.commands.mail.intercept.insufficient_capacity");
                return;
            }
        }
        Component hover = TextBuilder.combineAll(copy.getItem().getName(), "*", count - this.express.getCount());
        sendMessageIfPlayerOnline(this.sender, player -> {
            Component text = getOperatorPlayerName(operator, player);
            TextBuilder builder = TextBuilder.of("carpet.commands.mail.intercept.notice.sender", text, this.recipient);
            builder.setGrayItalic();
            builder.setHover(hover);
            return builder.build();
        });
        sendMessageIfPlayerOnline(this.recipient, player -> {
            Component text = getOperatorPlayerName(operator, player);
            TextBuilder builder = TextBuilder.of("carpet.commands.mail.intercept.notice.recipient", text, this.sender);
            builder.setGrayItalic();
            builder.setHover(hover);
            return builder.build();
        });
    }

    private Component getOperatorPlayerName(ServerPlayer operator, ServerPlayer player) {
        MailCommand instance = CommandRegister.getCommandInstance(MailCommand.class);
        return instance.intercept.test(player.createCommandSourceStack()) ? operator.getDisplayName() : TextBuilder.translate("carpet.generic.operator");
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
            CarpetOrgAddition.LOGGER.error("找不到接收快递的玩家，正在停止接收");
            throw new NullPointerException();
        }
        return this.insertStack(player);
    }

    /**
     * 撤回每一件快递
     */
    public InsertResult cancelEach() throws IOException {
        ServerPlayer player = this.server.getPlayerList().getPlayerByName(this.sender);
        if (player == null) {
            CarpetOrgAddition.LOGGER.error("找不到撤回快递的玩家，正在停止撤回");
            throw new NullPointerException();
        }
        return this.insertStack(player);
    }

    /**
     * 向物品栏里插入物品
     */
    private InsertResult insertStack(ServerPlayer player) throws IOException {
        int count = this.express.getCount();
        player.getInventory().add(this.express);
        // 物品没有插入
        if (count == this.express.getCount()) {
            return InsertResult.FAIL;
        }
        // 物品完全插入
        if (this.express.isEmpty()) {
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
        Component message = TextBuilder.of("carpet.commands.mail.sending.permission").setGrayItalic().build();
        MessageUtils.sendMessage(senderPlayer, message);
    }

    /**
     * 将快递信息保存到本地文件
     */
    public void save() throws IOException {
        NbtIo.write(this.writeNbt(this.server), this.worldFormat.file(this.getId() + IOUtils.NBT_EXTENSION).toPath());
    }

    /**
     * 删除已经完成的快递
     */
    public void delete() {
        File file = this.worldFormat.file(this.getId() + IOUtils.NBT_EXTENSION);
        if (file.delete()) {
            return;
        }
        CarpetOrgAddition.LOGGER.warn("未能成功删除名为{}的文件", file);
    }

    /**
     * 完成寄件
     */
    public boolean isComplete() {
        return this.express.isEmpty();
    }

    /**
     * 将快递内容写入NBT
     */
    public CompoundTag writeNbt(MinecraftServer server) {
        ProblemReporter.ScopedCollector logging = createErrorReporter();
        try (logging) {
            TagValueOutput nbt = TagValueOutput.createWithContext(logging, server.registryAccess());
            nbt.putInt(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
            nbt.putInt(NBT_DATA_VERSION, GenericUtils.getNbtDataVersion());
            nbt.putString("sender", this.sender);
            nbt.putString("recipient", this.recipient);
            if (this.uuid != null) {
                nbt.putString("uuid", this.uuid.toString());
            }
            nbt.putBoolean("cancel", this.cancel);
            nbt.putInt("id", this.id);
            int[] args = {time.getYear(), time.getMonthValue(), time.getDayOfMonth(), time.getHour(), time.getMinute(), time.getSecond()};
            nbt.putIntArray("time", args);
            nbt.store("item", ItemStack.CODEC, this.express);
            return nbt.buildResult();
        }
    }

    /**
     * 从NBT读取快递信息
     */
    public static Express readNbt(MinecraftServer server, CompoundTag nbt) {
        ProblemReporter.ScopedCollector logging = createErrorReporter();
        try (logging) {
            ValueInput view = TagValueInput.create(logging, server.registryAccess(), nbt);
            int nbtDataVersion = nbt.getInt(NBT_DATA_VERSION).orElse(-1);
            String sender = view.getString("sender").orElse(OfflinePlayerSearchTask.UNKNOWN);
            String recipient = view.getString("recipient").orElse(OfflinePlayerSearchTask.UNKNOWN);
            UUID uuid = GenericUtils.uuidFromString(nbt.getString("uuid").orElse(null)).orElse(null);
            boolean cancel = view.getBooleanOr("cancel", false);
            ItemStack stack = view.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            int id = view.getInt("id").orElse(-1);
            int[] times = view.getIntArray("time").orElse(new int[]{0, 0, 0, 0, 0, 0,});
            LocalDateTime localDateTime = LocalDateTime.of(times[0], times[1], times[2], times[3], times[4], times[5]);
            Express express = new Express(server, sender, recipient, uuid, stack, id, localDateTime, nbtDataVersion);
            express.cancel = cancel;
            return express;
        }
    }

    private static ProblemReporter.ScopedCollector createErrorReporter() {
        return new ProblemReporter.ScopedCollector(Express.class::toString, CarpetOrgAddition.LOGGER);
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
        return TextBuilder.translate("carpet.command.time.format",
                this.time.getYear(),
                this.time.getMonthValue(),
                this.time.getDayOfMonth(),
                this.time.getHour(),
                this.time.getMinute(),
                this.time.getSecond());
    }

    public ItemStack getExpress() {
        return express;
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
    public boolean isCancel() {
        return cancel;
    }

    @Override
    public boolean equals(Object obj) {
        if (this.isComplete()) {
            return false;
        }
        if (this.getClass() == obj.getClass()) {
            Express other = (Express) obj;
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
    public int compareTo(@NotNull Express o) {
        return this.id - o.id;
    }

    public int getNbtDataVersion() {
        return this.nbtDataVersion;
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
