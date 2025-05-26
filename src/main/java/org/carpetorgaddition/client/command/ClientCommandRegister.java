package org.carpetorgaddition.client.command;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import org.carpetorgaddition.config.CustomCommandConfig;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class ClientCommandRegister {
    public static final HashMap<Class<? extends AbstractClientCommand>, String> DEFAULT_COMMAND_NAMES = new HashMap<>();
    private static final HashMap<Class<? extends AbstractClientCommand>, AbstractClientCommand> commands = new HashMap<>();

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientCommandRegister::register);
    }

    private static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        // 字典命令
        register(new DictionaryCommand(dispatcher, access));
        // 高亮路径点命令
        register(new HighlightCommand(dispatcher, access));
        // 初始化自定义命令名称
        CustomCommandConfig.getInstance().refreshIfExpired();
    }

    private static <T extends AbstractClientCommand> void register(T command) {
        command.register();
        Class<? extends AbstractClientCommand> clazz = command.getClass();
        try {
            Field field = clazz.getField("DEFAULT_COMMAND_NAME");
            String name = (String) field.get(null);
            DEFAULT_COMMAND_NAMES.put(clazz, name);
        } catch (Exception e) {
            // 不处理，直接抛出，阻止游戏启动
            throw new RuntimeException(e);
        }
        commands.put(clazz, command);
    }

    public static <T extends AbstractClientCommand> T getCommandInstance(Class<T> clazz) {
        return clazz.cast(commands.get(clazz));
    }

    public static List<AbstractClientCommand> listCommands() {
        return commands.values().stream().toList();
    }
}
