package boat.carpetorgaddition.periodic.event;

import boat.carpetorgaddition.command.PlayerCommandExtension;
import boat.carpetorgaddition.periodic.dialog.DialogProvider;
import boat.carpetorgaddition.util.CommandUtils;
import boat.carpetorgaddition.util.FetcherUtils;
import boat.carpetorgaddition.util.GenericUtils;
import boat.carpetorgaddition.util.MessageUtils;
import boat.carpetorgaddition.wheel.GameProfileCache;
import boat.carpetorgaddition.wheel.TextBuilder;
import boat.carpetorgaddition.wheel.inventory.PlayerInventoryType;
import boat.carpetorgaddition.wheel.nbt.NbtReader;
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
                sendQueryPlayerNameFeekback(player, builder.build(), playerName);
            } else {
                // 本地不存在，从Mojang API获取
                GameProfileCache.QUERY_PLAYER_NAME_THREAD_POOL.submit(() -> queryPlayerName(context.getServer(), player, uuid));
                MessageUtils.sendMessage(player, "carpet.clickevent.query_player_name.start");
            }
        } catch (RejectedExecutionException e) {
            // 只允许同时存在一个线程执行查询任务
            throw CommandUtils.createException("carpet.command.thread.wait.last");
        }
    });

    /**
     * 在独立线程查询玩家名称
     */
    private static void queryPlayerName(MinecraftServer server, ServerPlayer player, UUID uuid) {
        TextBuilder builder = new TextBuilder("UUID");
        builder.setStringHover(uuid.toString());
        Component displayUuid = builder.build();
        String name;
        try {
            name = GameProfileCache.queryPlayerNameFromMojangApi(uuid);
        } catch (IOException e) {
            CommandSourceStack source = player.createCommandSourceStack();
            MessageUtils.sendErrorMessage(source, e, "carpet.clickevent.query_player_name.fail", displayUuid);
            return;
        }
        GameProfileCache cache = GameProfileCache.getInstance();
        cache.put(uuid, name);
        // 在服务器线程发送命令反馈
        server.execute(() -> sendQueryPlayerNameFeekback(player, displayUuid, name));
    }

    private static void sendQueryPlayerNameFeekback(ServerPlayer player, Component uuid, String playerName) {
        Component name = new TextBuilder(playerName).setCopyToClipboard(playerName).setColor(ChatFormatting.GRAY).build();
        MessageUtils.sendMessage(player, "carpet.clickevent.query_player_name.success", uuid, name);
    }

    /**
     * 字符串无法解析为UUID
     */
    private static CommandSyntaxException unableToResolveUuid(NbtReader reader) {
        return CommandUtils.createException("carpet.clickevent.uuid.from_string.fail", reader.getString(CustomClickKeys.UUID));
    }

    public static Identifier register(String id, CustomClickAction.CustomClickActionProcessor processor) {
        Identifier identifier = GenericUtils.ofIdentifier(id);
        CustomClickAction.register(identifier, processor);
        return identifier;
    }
}
