package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;

public class WaypointsCommand extends AbstractServerCommand {
    public WaypointsCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        // TODO 路径点分组，调整命令结构，强制添加注释规则，限制路径点名称不能包含“.”字符，修改者列表带时间，修改时日志记录
    }

    @Override
    public String getDefaultName() {
        return "";
    }
}
