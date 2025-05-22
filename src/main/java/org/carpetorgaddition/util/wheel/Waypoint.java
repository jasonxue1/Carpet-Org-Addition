package org.carpetorgaddition.util.wheel;

import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.dataupdate.waypoint.WaypointDataUpdater;
import org.carpetorgaddition.util.IOUtils;
import org.carpetorgaddition.util.MessageUtils;
import org.carpetorgaddition.util.WorldUtils;
import org.carpetorgaddition.util.provider.TextProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class Waypoint {
    public static final String WAYPOINT = "waypoint";
    private BlockPos blockPos;
    @Nullable
    private BlockPos anotherBlockPos;
    private final String dimension;
    @NotNull
    private MetaComment comment = new MetaComment();
    private final String creator;
    public final String name;

    public Waypoint(BlockPos blockPos, String name, String dimension, String creator) {
        this.name = name;
        this.blockPos = blockPos;
        this.dimension = dimension;
        this.creator = creator;
    }

    // 将路径点写入本地文件
    public void save(MinecraftServer server) throws IOException {
        JsonObject json = new JsonObject();
        // 数据版本
        json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
        // 路径点的坐标
        JsonObject pos = new JsonObject();
        pos.addProperty("x", this.blockPos.getX());
        pos.addProperty("y", this.blockPos.getY());
        pos.addProperty("z", this.blockPos.getZ());
        json.add("pos", pos);
        // 路径点所在维度
        json.addProperty("dimension", this.dimension);
        // 路径点的创建者
        json.addProperty("creator", this.creator);
        // 路径点的注释
        json.addProperty("comment", this.comment.getComment());
        // 路径点的另一个路径点坐标
        JsonObject anotherPos = new JsonObject();
        if (this.anotherBlockPos != null) {
            anotherPos.addProperty("x", this.anotherBlockPos.getX());
            anotherPos.addProperty("y", this.anotherBlockPos.getY());
            anotherPos.addProperty("z", this.anotherBlockPos.getZ());
        }
        json.add("another_pos", anotherPos);
        WorldFormat worldFormat = new WorldFormat(server, WAYPOINT);
        File file = worldFormat.file(this.name + IOUtils.JSON_EXTENSION);
        IOUtils.saveJson(file, json);
    }

    // 从本地文件加载一个路径点对象
    public static Waypoint load(MinecraftServer server, String name) throws IOException {
        WorldFormat worldFormat = new WorldFormat(server, WAYPOINT);
        File file = worldFormat.file(name, IOUtils.JSON_EXTENSION);
        JsonObject json = IOUtils.loadJson(file);
        WaypointDataUpdater dataUpdater = new WaypointDataUpdater();
        json = dataUpdater.update(json, DataUpdater.getVersion(json));
        // 路径点的位置
        JsonObject pos = json.get("pos").getAsJsonObject();
        int x = pos.get("x").getAsInt();
        int y = pos.get("y").getAsInt();
        int z = pos.get("z").getAsInt();
        BlockPos blockPos = new BlockPos(x, y, z);
        // 路径点的维度
        String dimension = IOUtils.jsonHasElement(json, "dimension") ? json.get("dimension").getAsString() : WorldUtils.OVERWORLD;
        // 路径点的创建者
        String creator = IOUtils.jsonHasElement(json, "creator") ? json.get("creator").getAsString() : "#none";
        Waypoint waypoint = new Waypoint(blockPos, name, dimension, creator);
        // 添加路径点的另一个坐标
        if (json.has("another_pos")) {
            JsonObject anotherPos = json.get("another_pos").getAsJsonObject();
            if (IOUtils.jsonHasElement(anotherPos, "x", "y", "z")) {
                int anotherX = anotherPos.get("x").getAsInt();
                int anotherY = anotherPos.get("y").getAsInt();
                int anotherZ = anotherPos.get("z").getAsInt();
                waypoint.setAnotherBlockPos(new BlockPos(anotherX, anotherY, anotherZ));
            }
        }
        // 添加路径点的说明文本
        waypoint.setComment(json.get("comment").getAsString());
        return waypoint;
    }

    // 显示路径点
    public void show(ServerCommandSource source) {
        MutableText text = switch (this.dimension) {
            case WorldUtils.OVERWORLD -> this.anotherBlockPos == null
                    ? TextBuilder.translate("carpet.commands.locations.show.overworld",
                    this.formatName(), TextProvider.blockPos(this.blockPos, Formatting.GREEN))
                    : TextBuilder.translate("carpet.commands.locations.show.overworld_and_the_nether",
                    this.formatName(), TextProvider.blockPos(this.blockPos, Formatting.GREEN),
                    TextProvider.blockPos(this.anotherBlockPos, Formatting.RED));
            case WorldUtils.THE_NETHER -> this.anotherBlockPos == null
                    ? TextBuilder.translate("carpet.commands.locations.show.the_nether",
                    this.formatName(), TextProvider.blockPos(this.blockPos, Formatting.RED))
                    : TextBuilder.translate("carpet.commands.locations.show.the_nether_and_overworld",
                    this.formatName(), TextProvider.blockPos(this.blockPos, Formatting.RED),
                    TextProvider.blockPos(this.anotherBlockPos, Formatting.GREEN));
            case WorldUtils.THE_END -> TextBuilder.translate("carpet.commands.locations.show.the_end",
                    this.formatName(), TextProvider.blockPos(this.blockPos, Formatting.DARK_PURPLE));
            default -> TextBuilder.translate("carpet.commands.locations.show.custom_dimension",
                    this.formatName(), this.dimension, TextProvider.blockPos(this.blockPos, Formatting.GREEN));
        };
        MessageUtils.sendMessage(source, text);
    }

    // 将路径点名称改为带有方括号和悬停样式的文本组件对象
    private Text formatName() {
        // TODO 很多位置都不显示注释，应统一使用此方法获取名称
        TextBuilder builder = new TextBuilder("[" + this.name.split("\\.")[0] + "]");
        if (this.comment.isEmpty()) {
            return builder.build();
        }
        builder.setHover(this.comment.getText());
        return builder.build();
    }

    public void setAnotherBlockPos(@Nullable BlockPos anotherBlockPos) {
        this.anotherBlockPos = anotherBlockPos;
    }

    public void setComment(String comment) {
        this.comment = comment == null || comment.isEmpty() ? new MetaComment() : new MetaComment(comment);
    }

    public void setBlockPos(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public @Nullable BlockPos getAnotherBlockPos() {
        return this.anotherBlockPos;
    }

    public String getDimension() {
        return dimension;
    }

    public String getName() {
        return name;
    }

    // 是否可以添加对向坐标
    public boolean canAddAnother() {
        return WorldUtils.isOverworld(this.dimension) || WorldUtils.isTheNether(this.dimension);
    }
}
