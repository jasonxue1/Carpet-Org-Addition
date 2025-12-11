package org.carpetorgaddition.periodic.express;

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
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.wheel.Counter;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.WorldFormat;
import org.carpetorgaddition.wheel.page.PageManager;
import org.carpetorgaddition.wheel.page.PagedCollection;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.provider.TextProvider;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * 快递管理器
 */
public class ExpressManager {
    private final TreeSet<Express> expresses = new TreeSet<>();
    private final WorldFormat worldFormat;
    private final MinecraftServer server;

    public ExpressManager(MinecraftServer server) {
        this.server = server;
        this.worldFormat = new WorldFormat(server, "express");
        // 从文件读取快递信息
        for (File file : this.worldFormat.toImmutableFileList()) {
            CompoundTag nbt;
            try {
                nbt = NbtIo.read(file.toPath());
            } catch (IOException e) {
                CarpetOrgAddition.LOGGER.warn("从文件{}读取快递信息失败", file, e);
                continue;
            }
            if (nbt == null) {
                continue;
            }
            Express express = Express.readNbt(server, nbt);
            // 快递对象物品为空，删除对应的文件
            if (express.isComplete()) {
                express.delete();
                continue;
            }
            this.expresses.add(express);
        }
    }

    /**
     * 提示玩家接收快递
     */
    public void promptToReceive(ServerPlayer player) {
        List<Express> list = this.expresses.stream()
                .filter(express -> express.isRecipient(player))
                .filter(express -> !express.isCancel())
                .toList();
        if (list.isEmpty()) {
            return;
        }
        ArrayList<Supplier<Component>> messages = new ArrayList<>();
        for (Express express : list) {
            messages.add(() -> {
                Component clickRun = TextProvider.clickRun(CommandProvider.receiveExpress(express.getId(), false));
                ItemStack stack = express.getExpress();
                return TextBuilder.translate("carpet.commands.mail.prompt_receive", stack.getCount(), stack.getDisplayName(), clickRun);
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
        this.expresses.removeIf(Express::isComplete);
    }

    /**
     * 添加新快递
     */
    public void put(Express express) throws IOException {
        put(express, true);
    }

    /**
     * 添加新快递，但不发送消息
     */
    public void putNoMessage(Express express) throws IOException {
        put(express, false);
    }

    private void put(Express express, boolean message) throws IOException {
        if (express.getExpress().isEmpty()) {
            CarpetOrgAddition.LOGGER.info("Attempted to send an empty item, ignored");
            return;
        }
        this.expresses.add(express);
        if (message) {
            express.sending();
            express.checkRecipientPermission();
        }
        // 将快递信息写入本地文件
        NbtIo.write(express.writeNbt(this.server), this.worldFormat.file(express.getId() + IOUtils.NBT_EXTENSION).toPath());
    }

    public Stream<Express> stream() {
        return this.expresses.stream();
    }

    public int receiveAll(ServerPlayer player) throws IOException, CommandSyntaxException {
        List<Express> list = this.stream()
                .filter(express -> express.isRecipient(player))
                .filter(express -> express.getNbtDataVersion() == GenericUtils.CURRENT_DATA_VERSION)
                .toList();
        if (list.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.mail.receive.all.non_existent");
        }
        // 总物品堆叠数
        int total = 0;
        // 接收物品堆叠数
        int receive = 0;
        HashMap<String, Counter<Item>> hashMap = new HashMap<>();
        for (Express express : list) {
            // 物品插入物品栏之前的堆叠数
            int count = express.getExpress().getCount();
            Item item = express.getExpress().getItem();
            total += count;
            Express.InsertResult each = express.receiveEach();
            int result = switch (each) {
                // 完全插入物品栏
                case COMPLETE -> count;
                // 部分插入物品栏
                case PART -> count - express.getExpress().getCount();
                // 未插入物品栏
                case FAIL -> 0;
            };
            Counter<Item> counter = hashMap.get(express.getSender());
            if (counter == null) {
                Counter<Item> value = new Counter<>();
                value.add(item, result);
                hashMap.put(express.getSender(), value);
            } else {
                counter.add(item, result);
            }
            receive += result;
        }
        if (receive == 0) {
            MessageUtils.sendMessage(player, "carpet.commands.mail.receive.insufficient_capacity");
        } else {
            if (receive == total) {
                MessageUtils.sendMessage(player, "carpet.commands.mail.receive.success", total, TextProvider.ITEM);
            } else {
                MessageUtils.sendMessage(player, "carpet.commands.mail.receive.partial_reception", receive, total - receive);
            }
            // 播放物品拾取音效
            Express.playItemPickupSound(player);
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

    public int cancelAll(ServerPlayer player) throws IOException, CommandSyntaxException {
        List<Express> list = this.stream()
                .filter(express -> express.isSender(player))
                .filter(express -> express.getNbtDataVersion() == GenericUtils.CURRENT_DATA_VERSION)
                .toList();
        if (list.isEmpty()) {
            throw CommandUtils.createException("carpet.commands.mail.cancel.all.non_existent");
        }
        // 总物品堆叠数
        int total = 0;
        // 撤回物品堆叠数
        int cancel = 0;
        HashSet<String> players = new HashSet<>();
        for (Express express : list) {
            players.add(express.getRecipient());
            // 物品插入物品栏之前的堆叠数
            int count = express.getExpress().getCount();
            total += count;
            Express.InsertResult each = express.cancelEach();
            cancel += switch (each) {
                // 完全插入物品栏
                case COMPLETE -> count;
                // 部分插入物品栏
                case PART -> count - express.getExpress().getCount();
                // 未插入物品栏
                case FAIL -> 0;
            };
        }
        if (cancel == 0) {
            MessageUtils.sendMessage(player, "carpet.commands.mail.cancel.insufficient_capacity");
        } else {
            if (cancel == total) {
                MessageUtils.sendMessage(player, "carpet.commands.mail.cancel.success", total, TextProvider.ITEM);
            } else {
                MessageUtils.sendMessage(player, "carpet.commands.mail.cancel.partial_reception", cancel, total - cancel);
            }
            // 播放物品拾取音效
            Express.playItemPickupSound(player);
            TextBuilder builder = TextBuilder.of("carpet.commands.mail.cancel.notice", player.getDisplayName());
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
        return cancel;
    }

    /**
     * @return 获取快递发送者的快递被接收者接收的消息
     */
    public static Component getReceiveNotice(ServerPlayer player, Counter<Item> counter) {
        ArrayList<Component> list = new ArrayList<>();
        for (Item item : counter) {
            list.add(TextBuilder.combineAll(item.getName(), "*", counter.getCount(item)));
        }
        TextBuilder builder = TextBuilder.of("carpet.commands.mail.sending.notice", player.getDisplayName());
        builder.setGrayItalic();
        builder.setHover(TextBuilder.joinList(list));
        return builder.build();
    }

    /**
     * 使用二分查找获取指定单号的快递
     *
     * @param id 要查找的快递单号
     */
    public Optional<Express> binarySearch(int id) {
        List<Express> list = this.expresses.stream().toList();
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
        if (this.expresses.isEmpty()) {
            return 1;
        }
        // 集合最后一个元素id等于集合长度，说明前面的单号都是连续的，新单号为集合长度+1
        if (this.expresses.last().getId() == this.expresses.size()) {
            return this.expresses.size() + 1;
        }
        // 遍历集合找到空缺的单号
        int number = 0;
        for (Express express : this.expresses) {
            number++;
            if (number == express.getId()) {
                continue;
            }
            return number;
        }
        throw new IllegalStateException();
    }
}
