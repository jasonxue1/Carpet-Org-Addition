package boat.carpetorgaddition.network.event;

import boat.carpetorgaddition.command.PlayerCommandExtension;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
import boat.carpetorgaddition.wheel.page.PageManager;
import boat.carpetorgaddition.wheel.page.PagedCollection;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

public class CustomClickEvents {
    private static final LocalizationKey KEY = LocalizationKeys.OPERATION;
    public static final Identifier OPEN_DIALOG = register("open_dialog", context -> {
        NbtReader reader = context.getReader();
        Identifier id = reader.getIdentifier("id");
        MinecraftServer server = context.getServer();
        DialogProvider provider = FetcherUtils.getDialogProvider(server);
        Dialog dialog = provider.getDialog(id);
        context.getPlayer().openDialog(Holder.direct(dialog));
    });
    public static final Identifier OPEN_INVENTORY = register("open_inventory", context -> {
        NbtReader reader = context.getReader();
        UUID uuid = reader.getUuidNullable(CustomClickKeys.UUID).orElseThrow(() -> unableToResolveUuid(reader));
        PlayerInventoryType type = reader.getPlayerInventoryType(CustomClickKeys.INVENTORY_TYPE);
        PlayerCommandExtension.openPlayerInventory(context.getServer(), uuid, context.getPlayer(), type);
    });
    public static final Identifier QUERY_PLAYER_NAME = register("query_player_name", context -> {
        try {
            LocalizationKey key = KEY.then("query_player_name");
            NbtReader reader = context.getReader();
            UUID uuid = reader.getUuidNullable(CustomClickKeys.UUID).orElseThrow(() -> unableToResolveUuid(reader));
            ServerPlayer player = context.getPlayer();
            if (context.getActionSource() == ActionSource.DIALOG) {
                // 关闭当前对话框（等待服务器响应屏幕）
                player.closeContainer();
            }
            GameProfileCache cache = GameProfileCache.getInstance();
            Optional<String> optional = cache.get(uuid);
            if (optional.isPresent()) {
                // 如果本地存在，就不再从Mojang API获取
                String playerName = optional.get();
                TextBuilder builder = new TextBuilder("UUID");
                builder.setStringHover(uuid.toString());
                sendQueryPlayerNameFeekback(player, key, builder.build(), playerName);
            } else {
                // 本地不存在，从Mojang API获取
                GameProfileCache.QUERY_PLAYER_NAME_THREAD_POOL.submit(() -> queryPlayerName(context.getServer(), key, player, uuid));
                MessageUtils.sendMessage(player, key.then("start").translate());
            }
        } catch (RejectedExecutionException e) {
            // 只允许同时存在一个线程执行查询任务
            throw CommandUtils.createException("carpet.command.thread.wait.last");
        }
    });
    public static final Identifier TURN_THE_PAGE = register("turn_the_page", context -> {
        NbtReader reader = context.getReader();
        int id = reader.getInt(CustomClickKeys.ID);
        int page = reader.getInt(CustomClickKeys.PAGE_NUMBER);
        PageManager manager = FetcherUtils.getPageManager(context.getServer());
        Optional<PagedCollection> optional = manager.get(id);
        if (optional.isPresent()) {
            PagedCollection collection = optional.get();
            collection.print(page, true);
        } else {
            throw CommandUtils.createException("carpet.command.page.non_existent");
        }
    });

    /**
     * 在独立线程查询玩家名称
     */
    private static void queryPlayerName(MinecraftServer server, LocalizationKey key, ServerPlayer player, UUID uuid) {
        TextBuilder builder = new TextBuilder("UUID");
        builder.setStringHover(uuid.toString());
        Component displayUuid = builder.build();
        String name;
        try {
            name = GameProfileCache.queryPlayerNameFromMojangApi(uuid);
        } catch (IOException e) {
            CommandSourceStack source = player.createCommandSourceStack();
            MessageUtils.sendErrorMessage(source, key.then("fail").translate(displayUuid), e);
            return;
        }
        GameProfileCache cache = GameProfileCache.getInstance();
        cache.put(uuid, name);
        // 在服务器线程发送命令反馈
        server.execute(() -> sendQueryPlayerNameFeekback(player, key, displayUuid, name));
    }

    private static void sendQueryPlayerNameFeekback(ServerPlayer player, LocalizationKey key, Component uuid, String playerName) {
        Component name = new TextBuilder(playerName).setCopyToClipboard(playerName).setColor(ChatFormatting.GRAY).build();
        MessageUtils.sendMessage(player, key.then("success").translate(uuid, name));
    }

    /**
     * 字符串无法解析为UUID
     */
    private static CommandSyntaxException unableToResolveUuid(NbtReader reader) {
        LocalizationKey key = KEY.then("uuid", "from_string", "fail");
        return CommandUtils.createException(key.translate(reader.getString(CustomClickKeys.UUID)));
    }

    public static Identifier register(String id, CustomClickAction.CustomClickActionProcessor processor) {
        Identifier identifier = GenericUtils.ofIdentifier(id);
        CustomClickAction.register(identifier, processor);
        return identifier;
    }
}
