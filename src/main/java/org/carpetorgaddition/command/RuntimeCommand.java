package org.carpetorgaddition.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.wheel.TextBuilder;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.text.DecimalFormat;

public class RuntimeCommand extends AbstractServerCommand {
    public RuntimeCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        this.dispatcher.register(CommandManager.literal(name)
                .then(CommandManager.literal("memory")
                        .then(CommandManager.literal("jvm")
                                .executes(this::showJvmMemory))
                        .then(CommandManager.literal("physical")
                                .executes(this::showPhysicalMemory))
                        .then(CommandManager.literal("gc")
                                .executes(this::triggerGc)))
                .then(CommandManager.literal("server")
                        .then(CommandManager.literal("openfolder")
                                .executes(this::openFolder))));
    }

    /**
     * 显示jvm内存使用情况
     *
     * @return 已使用的内存大小
     */
    private int showJvmMemory(CommandContext<ServerCommandSource> context) {
        Runtime runtime = Runtime.getRuntime();
        // 已使用内存
        long usedSize = runtime.totalMemory() - runtime.freeMemory();
        Text used = displayMemory(usedSize);
        Text total = displayMemory(runtime.totalMemory());
        Text max = displayMemory(runtime.maxMemory());
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, "carpet.commands.runtime.memory.jvm");
        MessageUtils.sendMessage(context, "carpet.commands.runtime.memory.jvm.used", used);
        MessageUtils.sendMessage(context, "carpet.commands.runtime.memory.jvm.total", total);
        MessageUtils.sendMessage(context, "carpet.commands.runtime.memory.jvm.max", max);
        return (int) usedSize;
    }

    /**
     * 显示物理内存使用情况
     *
     * @return 已使用的物理内存大小
     */
    private int showPhysicalMemory(CommandContext<ServerCommandSource> context) {
        SystemInfo info = new SystemInfo();
        GlobalMemory memory = info.getHardware().getMemory();
        long usedSize = memory.getTotal() - memory.getAvailable();
        Text total = displayMemory(memory.getTotal());
        Text used = displayMemory(usedSize);
        MessageUtils.sendEmptyMessage(context);
        MessageUtils.sendMessage(context, "carpet.commands.runtime.memory.physical");
        MessageUtils.sendMessage(context, "carpet.commands.runtime.memory.physical.total", total);
        MessageUtils.sendMessage(context, "carpet.commands.runtime.memory.physical.used", used);
        return (int) usedSize;
    }

    /**
     * 尝试触发一次gc
     */
    private int triggerGc(CommandContext<ServerCommandSource> context) {
        Runtime runtime = Runtime.getRuntime();
        long l = runtime.freeMemory();
        runtime.gc();
        long size = runtime.freeMemory() - l;
        Text free = displayMemory(size);
        MessageUtils.sendMessage(context, "carpet.commands.runtime.gc", free);
        MutableText prompt = TextBuilder.ofTranslate("carpet.commands.runtime.gc.prompt").setGrayItalic().build();
        MessageUtils.sendMessage(context.getSource(), prompt);
        return (int) size;
    }

    /**
     * 打开世界文件夹
     */
    private int openFolder(CommandContext<ServerCommandSource> context) {
        Util.getOperatingSystem().open(context.getSource().getServer().getSavePath(WorldSavePath.ROOT));
        return 1;
    }

    private Text displayMemory(long size) {
        DecimalFormat format = new DecimalFormat("#.00");
        String mb = format.format(size / 1024.0 / 1024.0);
        TextBuilder builder = TextBuilder.of("%s MB".formatted(mb))
                .setHover("carpet.command.data.unit.byte", size)
                .setColor(Formatting.GRAY);
        return builder.build();
    }

    @Override
    public String getDefaultName() {
        return "runtime";
    }

    @Override
    public boolean shouldRegister() {
        return CarpetOrgAddition.isDebugDevelopment();
    }
}
