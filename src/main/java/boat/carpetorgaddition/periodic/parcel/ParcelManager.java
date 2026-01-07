package boat.carpetorgaddition.periodic.parcel;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.command.MailCommand;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.IOUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.Counter;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 快递管理器
 */
public class ParcelManager {
    private final TreeSet<Parcel> parcels = new TreeSet<>();
    private final WorldFormat worldFormat;
    private final MinecraftServer server;

    public ParcelManager(MinecraftServer server) {
        this.server = server;
        this.worldFormat = new WorldFormat(server, "express");
        // 从文件读取快递信息
        for (File file : this.worldFormat.toImmutableFileList()) {
            CompoundTag nbt;
            try {
                // TODO 改为使用文件名识别ID
                nbt = NbtIo.read(file.toPath());
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("Failed to read cached data from file {}", file, e);
                continue;
            }
            if (nbt == null) {
                continue;
            }
            Parcel parcel = Parcel.readNbt(server, nbt);
            // 快递对象物品为空，删除对应的文件
            if (parcel.isComplete()) {
                parcel.delete();
                continue;
            }
            this.parcels.add(parcel);
        }
    }

    /**
     * 提示玩家接收快递
     */
    public void promptToCollect(ServerPlayer player) {
        List<Parcel> list = this.parcels.stream()
                .filter(parcel -> parcel.isRecipient(player))
                .filter(parcel -> !parcel.isRecall())
                .toList();
        if (list.isEmpty()) {
            return;
        }
        ArrayList<Supplier<Component>> messages = new ArrayList<>();
        for (Parcel parcel : list) {
            messages.add(() -> {
                Component clickRun = TextProvider.clickRun(CommandProvider.collectExpress(parcel.getId(), false));
                ItemStack stack = parcel.getExpress();
                return MailCommand.KEY.then("prompt_collect").translate(stack.getCount(), stack.getDisplayName(), clickRun);
            });
        }
        PageManager pageManager = FetcherUtils.getPageManager(this.server);
        CommandSourceStack source = player.createCommandSourceStack();
        PagedCollection collection = pageManager.newPagedCollection(source);
        collection.addContent(messages);
        MessageUtils.sendEmptyMessage(source);
        CommandUtils.handlingException(collection::print, source);
    }

    /**
     * 每个游戏刻删除已经寄件完成的快递
     */
    public void tick() {
        this.parcels.removeIf(Parcel::isComplete);
    }

    /**
     * 添加新快递
     */
    public void put(Parcel parcel) throws IOException {
        put(parcel, true);
    }

    /**
     * 添加新快递，但不发送消息
     */
    public void putNoMessage(Parcel parcel) throws IOException {
        put(parcel, false);
    }

    private void put(Parcel parcel, boolean message) throws IOException {
        if (parcel.getExpress().isEmpty()) {
            CarpetOrgAddition.LOGGER.info("Attempted to send an empty item, ignored");
            return;
        }
        this.parcels.add(parcel);
        if (message) {
            parcel.send();
            parcel.checkRecipientPermission();
        }
        // 将快递信息写入本地文件
        NbtIo.write(parcel.writeNbt(this.server), this.worldFormat.file(parcel.getId() + IOUtils.NBT_EXTENSION).toPath());
    }

    public Stream<Parcel> stream() {
        return this.parcels.stream();
    }

    public int collectAll(ServerPlayer player) throws IOException, CommandSyntaxException {
        List<Parcel> list = this.stream()
                .filter(parcel -> parcel.isRecipient(player))
                .toList();
        LocalizationKey key = MailCommand.COLLECT;
        if (list.isEmpty()) {
            throw CommandUtils.createException(key.then("no_parcels").translate());
        }
        // 总物品堆叠数
        int total = 0;
        // 接收物品堆叠数
        int receive = 0;
        HashMap<String, Counter<Item>> hashMap = new HashMap<>();
        for (Parcel parcel : list) {
            // 物品插入物品栏之前的堆叠数
            int count = parcel.getExpress().getCount();
            Item item = parcel.getExpress().getItem();
            total += count;
            Parcel.InsertResult each = parcel.receiveEach();
            int result = switch (each) {
                // 完全插入物品栏
                case COMPLETE -> count;
                // 部分插入物品栏
                case PART -> count - parcel.getExpress().getCount();
                // 未插入物品栏
                case FAIL -> 0;
            };
            Counter<Item> counter = hashMap.get(parcel.getSender());
            if (counter == null) {
                Counter<Item> value = new Counter<>();
                value.add(item, result);
                hashMap.put(parcel.getSender(), value);
            } else {
                counter.add(item, result);
            }
            receive += result;
        }
        if (receive == 0) {
            MessageUtils.sendMessage(player, key.then("insufficient_capacity").translate());
        } else {
            if (receive == total) {
                MessageUtils.sendMessage(player, key.then("success").translate(total, LocalizationKeys.Item.ITEM.translate()));
            } else {
                MessageUtils.sendMessage(player, key.then("partial_reception").translate(receive, total - receive));
            }
            // 播放物品拾取音效
            Parcel.playItemPickupSound(player);
            PlayerList playerManager = FetcherUtils.getServer(player).getPlayerList();
            for (Map.Entry<String, Counter<Item>> entry : hashMap.entrySet()) {
                // 通知发送者物品已接收
                Component message = getReceiveNotice(player, entry.getValue());
                ServerPlayer playerEntity = playerManager.getPlayerByName(entry.getKey());
                if (playerEntity == null) {
                    continue;
                }
                MessageUtils.sendMessage(playerEntity, message);
            }
        }
        return receive;
    }

    public int recallAll(ServerPlayer player) throws IOException, CommandSyntaxException {
        List<Parcel> list = this.stream()
                .filter(parcel -> parcel.isSender(player))
                .toList();
        LocalizationKey key = MailCommand.RECALL;
        if (list.isEmpty()) {
            throw CommandUtils.createException(key.then("no_parcels").translate());
        }
        // 总物品堆叠数
        int total = 0;
        // 撤回物品堆叠数
        int recall = 0;
        HashSet<String> players = new HashSet<>();
        for (Parcel parcel : list) {
            players.add(parcel.getRecipient());
            // 物品插入物品栏之前的堆叠数
            int count = parcel.getExpress().getCount();
            total += count;
            Parcel.InsertResult each = parcel.recallEach();
            recall += switch (each) {
                // 完全插入物品栏
                case COMPLETE -> count;
                // 部分插入物品栏
                case PART -> count - parcel.getExpress().getCount();
                // 未插入物品栏
                case FAIL -> 0;
            };
        }
        if (recall == 0) {
            MessageUtils.sendMessage(player, key.then("insufficient_capacity").translate());
        } else {
            if (recall == total) {
                MessageUtils.sendMessage(player, key.then("success").translate(total, LocalizationKeys.Item.ITEM.translate()));
            } else {
                MessageUtils.sendMessage(player, key.then("partial_reception").translate(recall, total - recall));
            }
            // 播放物品拾取音效
            Parcel.playItemPickupSound(player);
            TextBuilder builder = new TextBuilder(MailCommand.NOTICE.then("recall").translate(player.getDisplayName()));
            builder.setGrayItalic();
            Component message = builder.build();
            for (String name : players) {
                PlayerList playerManager = FetcherUtils.getServer(player).getPlayerList();
                ServerPlayer receivePlayer = playerManager.getPlayerByName(name);
                if (receivePlayer == null) {
                    continue;
                }
                MessageUtils.sendMessage(receivePlayer, message);
            }
        }
        return recall;
    }

    /**
     * @return 获取快递发送者的快递被接收者接收的消息
     */
    public static Component getReceiveNotice(ServerPlayer player, Counter<Item> counter) {
        ArrayList<Component> list = new ArrayList<>();
        for (Item item : counter) {
            list.add(TextBuilder.combineAll(item.getName(), "*", counter.getCount(item)));
        }
        TextBuilder builder = new TextBuilder(MailCommand.NOTICE.then("collect").translate(player.getDisplayName()));
        builder.setGrayItalic();
        builder.setHover(TextBuilder.joinList(list));
        return builder.build();
    }

    /**
     * 使用二分查找获取指定单号的快递
     *
     * @param id 要查找的快递单号
     */
    public Optional<Parcel> binarySearch(int id) {
        List<Parcel> list = this.parcels.stream().toList();
        int left = 0;
        int right = list.size() - 1;
        while (left <= right) {
            int mid = (left + right) >>> 1;
            int currentId = list.get(mid).getId();
            if (currentId < id) {
                left = mid + 1;
            } else if (currentId > id) {
                right = mid - 1;
            } else {
                return Optional.of(list.get(mid));
            }
        }
        return Optional.empty();
    }

    /**
     * 生成快递单号
     */
    public int generateNumber() {
        // 没有快递发出，快递单号为1
        if (this.parcels.isEmpty()) {
            return 1;
        }
        // 集合最后一个元素id等于集合长度，说明前面的单号都是连续的，新单号为集合长度+1
        if (this.parcels.last().getId() == this.parcels.size()) {
            return this.parcels.size() + 1;
        }
        // 遍历集合找到空缺的单号
        int number = 0;
        for (Parcel parcel : this.parcels) {
            number++;
            if (number == parcel.getId()) {
                continue;
            }
            return number;
        }
        throw new IllegalStateException();
    }
}
