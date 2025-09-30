package org.carpetorgaddition.periodic.fakeplayer;

import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import org.carpetorgaddition.CarpetOrgAddition;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.dataupdate.DataUpdater;
import org.carpetorgaddition.dataupdate.player.FakePlayerSerializeDataUpdater;
import org.carpetorgaddition.periodic.ServerComponentCoordinator;
import org.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionSerializer;
import org.carpetorgaddition.periodic.task.ServerTaskManager;
import org.carpetorgaddition.periodic.task.schedule.DelayedLoginTask;
import org.carpetorgaddition.util.*;
import org.carpetorgaddition.wheel.MetaComment;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.WorldFormat;
import org.carpetorgaddition.wheel.provider.CommandProvider;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FakePlayerSerializer implements Comparable<FakePlayerSerializer> {
    /**
     * 玩家名称
     */
    private final String fakePlayerName;
    /**
     * 注释
     */
    private final MetaComment comment = new MetaComment();
    /**
     * 位置
     */
    @NotNull
    private final Vec3d playerPos;
    /**
     * 偏航角
     */
    private final float yaw;
    /**
     * 俯仰角
     */
    private final float pitch;
    /**
     * 维度
     */
    @NotNull
    private final String dimension;
    /**
     * 游戏模式
     */
    @NotNull
    private final GameMode gameMode;
    /**
     * 是否飞行
     */
    private final boolean flying;
    /**
     * 是否潜行
     */
    private final boolean sneaking;
    /**
     * 是否自动登录
     */
    private boolean autologin = false;
    /**
     * 假玩家手部动作
     */
    @NotNull
    private final EntityPlayerActionPackSerial interactiveAction;
    /**
     * 假玩家自动动作
     */
    @NotNull
    private final FakePlayerActionSerializer autoAction;
    /**
     * 玩家所在的组，集合中可能包含null元素
     */
    private final HashSet<@Nullable String> groups = new HashSet<>();
    /**
     * 当前对象是否已经修改，即是否需要重新保存
     */
    private boolean isChanged = false;
    private final File file;

    public FakePlayerSerializer(EntityPlayerMPFake fakePlayer) {
        this.fakePlayerName = FetcherUtils.getPlayerName(fakePlayer);
        this.playerPos = FetcherUtils.getFootPos(fakePlayer);
        this.yaw = fakePlayer.getYaw();
        this.pitch = fakePlayer.getPitch();
        this.dimension = WorldUtils.getDimensionId(FetcherUtils.getWorld(fakePlayer));
        this.gameMode = fakePlayer.interactionManager.getGameMode();
        this.flying = fakePlayer.getAbilities().flying;
        this.sneaking = fakePlayer.isSneaking();
        this.interactiveAction = new EntityPlayerActionPackSerial(((ServerPlayerInterface) fakePlayer).getActionPack());
        this.autoAction = new FakePlayerActionSerializer(fakePlayer);
        this.file = new WorldFormat(FetcherUtils.getServer(fakePlayer), PlayerSerializationManager.PLAYER_DATA).file(this.fakePlayerName, "json");
    }

    public FakePlayerSerializer(EntityPlayerMPFake fakePlayer, FakePlayerSerializer serializer) {
        this(fakePlayer);
        // this.groups可能传入一个null
        this.groups.addAll(serializer.getGroups());
        this.autologin = serializer.autologin;
        this.comment.setComment(serializer.comment.getComment());
        this.isChanged = true;
    }

    public FakePlayerSerializer(EntityPlayerMPFake fakePlayer, String comment) {
        this(fakePlayer);
        this.comment.setComment(comment);
    }

    public FakePlayerSerializer(File file) throws IOException {
        JsonObject json = IOUtils.loadJson(file);
        int version = DataUpdater.getVersion(json);
        if (version < DataUpdater.VERSION) {
            FakePlayerSerializeDataUpdater dataUpdater = new FakePlayerSerializeDataUpdater();
            // 需要重新保存吗？这可能会提高下一次读取文件的效率，但是会导致配置文件与低版本不兼容
            json = dataUpdater.update(json, version);
        }
        // 玩家名
        this.fakePlayerName = file.getName().split("\\.")[0];
        // 玩家位置
        JsonObject pos = json.get("pos").getAsJsonObject();
        this.playerPos = new Vec3d(pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble());
        // 获取朝向
        JsonObject direction = json.get("direction").getAsJsonObject();
        this.yaw = direction.get("yaw").getAsFloat();
        this.pitch = direction.get("pitch").getAsFloat();
        // 维度
        this.dimension = json.get("dimension").getAsString();
        // 游戏模式
        this.gameMode = GameMode.byId(json.get("gamemode").getAsString());
        // 是否飞行
        this.flying = json.get("flying").getAsBoolean();
        // 是否潜行
        this.sneaking = json.get("sneaking").getAsBoolean();
        // 是否自动登录
        this.autologin = IOUtils.getJsonElement(json, "autologin", false, Boolean.class);
        // 注释
        JsonElement element = json.get("annotation");
        this.comment.setComment(element == null ? "" : element.getAsString());
        // 假玩家左右手动作
        if (json.has("hand_action")) {
            this.interactiveAction = new EntityPlayerActionPackSerial(json.get("hand_action").getAsJsonObject());
        } else {
            this.interactiveAction = EntityPlayerActionPackSerial.NO_ACTION;
        }
        // 假玩家动作，自动合成自动交易等
        if (json.has(PlayerSerializationManager.SCRIPT_ACTION)) {
            JsonObject scriptJson = json.get(PlayerSerializationManager.SCRIPT_ACTION).getAsJsonObject();
            this.autoAction = new FakePlayerActionSerializer(scriptJson);
        } else {
            this.autoAction = FakePlayerActionSerializer.NO_ACTION;
        }
        // 玩家组
        if (json.has("group")) {
            JsonArray array = json.getAsJsonArray("group");
            for (JsonElement group : array) {
                if (group.isJsonNull()) {
                    continue;
                }
                this.groups.add(group.getAsString());
            }
        }
        this.file = file;
    }

    public void save() {
        try {
            IOUtils.write(this.file, this.toJson());
        } catch (IOException e) {
            // 译：未能成功保存玩家数据
            CarpetOrgAddition.LOGGER.warn("Failed to successfully save player data", e);
        }
        this.isChanged = false;
    }

    // 从json加载并生成假玩家
    public void spawn(MinecraftServer server) throws CommandSyntaxException {
        if (server.getPlayerManager().getPlayer(this.fakePlayerName) != null) {
            throw CommandUtils.createException("carpet.commands.playerManager.spawn.player_exist");
        }
        Consumer<EntityPlayerMPFake> consumer = fakePlayer -> {
            fakePlayer.setSneaking(this.sneaking);
            // 设置玩家动作
            this.interactiveAction.startAction(fakePlayer);
            this.autoAction.clearPlayer();
            this.autoAction.startAction(fakePlayer);
        };
        // 生成假玩家
        GenericUtils.createFakePlayer(
                this.fakePlayerName,
                server,
                this.playerPos,
                this.yaw,
                this.pitch,
                WorldUtils.getWorld(this.dimension),
                this.gameMode,
                this.flying,
                consumer
        );
    }

    // 显示文本信息
    public Text info() {
        ArrayList<Text> list = new ArrayList<>();
        // 玩家位置
        String pos = MathUtils.numberToTwoDecimalString(this.playerPos.getX()) + " "
                     + MathUtils.numberToTwoDecimalString(this.playerPos.getY()) + " "
                     + MathUtils.numberToTwoDecimalString(this.playerPos.getZ());
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.pos", pos));
        // 获取朝向
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.direction",
                MathUtils.numberToTwoDecimalString(this.yaw),
                MathUtils.numberToTwoDecimalString(this.pitch)));
        // 维度
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.dimension", TextProvider.dimension(this.dimension)));
        // 游戏模式
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.gamemode", this.gameMode.getTranslatableName()));
        // 是否飞行
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.flying", TextProvider.getBoolean(this.flying)));
        // 是否潜行
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.sneaking", TextProvider.getBoolean(this.sneaking)));
        // 是否自动登录
        list.add(TextBuilder.translate("carpet.commands.playerManager.info.autologin", TextProvider.getBoolean(this.autologin)));
        if (this.interactiveAction.hasAction()) {
            list.add(this.interactiveAction.toText());
        }
        if (this.autoAction.hasAction()) {
            list.add(this.autoAction.toText());
        }
        if (this.comment.hasContent()) {
            // 添加注释
            list.add(TextBuilder.translate("carpet.commands.playerManager.info.comment", this.comment.getText()));
        }
        return TextBuilder.joinList(list);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty(DataUpdater.DATA_VERSION, DataUpdater.VERSION);
        // 玩家位置
        JsonObject pos = new JsonObject();
        pos.addProperty("x", this.playerPos.x);
        pos.addProperty("y", this.playerPos.y);
        pos.addProperty("z", this.playerPos.z);
        json.add("pos", pos);
        // 玩家朝向
        JsonObject direction = new JsonObject();
        direction.addProperty("yaw", this.yaw);
        direction.addProperty("pitch", this.pitch);
        json.add("direction", direction);
        // 维度
        json.addProperty("dimension", this.dimension);
        // 游戏模式
        json.addProperty("gamemode", this.gameMode.getId());
        // 是否飞行
        json.addProperty("flying", this.flying);
        // 是否潜行
        json.addProperty("sneaking", this.sneaking);
        // 自动登录
        json.addProperty("autologin", this.autologin);
        // 注释
        json.addProperty("annotation", this.comment.getComment());
        // 添加左键右键动作
        json.add("hand_action", interactiveAction.toJson());
        // 添加玩家动作
        json.add(PlayerSerializationManager.SCRIPT_ACTION, this.autoAction.toJson());
        // 添加玩家组
        JsonArray groups = new JsonArray();
        for (String group : this.groups) {
            if (group == null) {
                continue;
            }
            groups.add(group);
        }
        json.add("group", groups);
        return json;
    }

    // 修改注释
    public void setComment(@Nullable String comment) {
        this.comment.setComment(comment);
        this.isChanged = true;
    }

    // 设置自动登录
    public void setAutologin(boolean autologin) {
        this.autologin = autologin;
        this.isChanged = true;
    }

    /**
     * 将玩家添加到组
     */
    public void addToGroup(String group) {
        this.groups.add(group);
        this.isChanged = true;
    }

    /**
     * 将玩家从组中删除
     *
     * @return 是否删除成功
     */
    public boolean removeFromGroup(String group) {
        boolean remove = this.groups.remove(group);
        this.isChanged = true;
        return remove;
    }

    // 获取玩家名
    public String getFakePlayerName() {
        return this.fakePlayerName;
    }

    // 获取显示名称
    public Text getDisplayName() {
        return new TextBuilder(this.fakePlayerName).setHover(this.info()).build();
    }

    public Supplier<Text> toTextSupplier() {
        return this::toText;
    }

    private Text toText() {
        Text loginHover = TextBuilder.translate("carpet.commands.playerManager.click.online");
        Text logoutHover = TextBuilder.translate("carpet.commands.playerManager.click.offline");
        String name = this.getFakePlayerName();
        String logonCommand = CommandProvider.playerManagerSpawn(name);
        String logoutCommand = CommandProvider.killFakePlayer(name);
        Text login = new TextBuilder("[↑]").setCommand(logonCommand).setHover(loginHover).setColor(Formatting.GREEN).build();
        Text logout = new TextBuilder("[↓]").setCommand(logoutCommand).setHover(logoutHover).setColor(Formatting.RED).build();
        Text info = new TextBuilder("[?]").setHover(this.info()).setColor(Formatting.GRAY).build();
        TextBuilder builder = new TextBuilder(name);
        builder.setHover(this.comment);
        return TextBuilder.combineAll(login, " ", logout, " ", info, " ", builder.build());
    }

    /**
     * 假玩家自动登录
     */
    public static void autoLogin(MinecraftServer server) {
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        try {
            List<FakePlayerSerializer> list = FetcherUtils.getFakePlayerSerializationManager(server).list();
            int count = server.getCurrentPlayerCount();
            for (FakePlayerSerializer serializer : list) {
                if (serializer.autologin) {
                    manager.addTask(new DelayedLoginTask(server, serializer, 1) {
                        @Override
                        public void tick() {
                            try {
                                // 隐藏登录游戏的消息：无法保证单人游戏中自己一定先比假玩家先加入游戏，也就无法保证登录消息一定显示
                                CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(true);
                                super.tick();
                            } finally {
                                CarpetOrgAdditionSettings.hiddenLoginMessages.setExternal(false);
                            }
                        }
                    });
                    count++;
                    // 阻止假玩家把玩家上线占满，至少为一名真玩家保留一个名额
                    if (count >= server.getMaxPlayerCount() - 1) {
                        CarpetOrgAddition.LOGGER.warn("The number of server players is about to reach its limit");
                        break;
                    }
                }
            }
        } catch (RuntimeException | CommandSyntaxException e) {
            CarpetOrgAddition.LOGGER.error("玩家自动登录出现意外错误", e);
        }
    }

    public String getComment() {
        return this.comment.getComment();
    }

    public boolean isChanged() {
        return this.isChanged;
    }

    public Set<String> getGroups() {
        return this.groups.isEmpty() ? Collections.singleton(null) : Collections.unmodifiableSet(this.groups);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (this.getClass() == obj.getClass() && this.fakePlayerName.equals(((FakePlayerSerializer) obj).fakePlayerName));
    }

    @Override
    public int hashCode() {
        return this.fakePlayerName.hashCode();
    }

    @Override
    public int compareTo(@NotNull FakePlayerSerializer o) {
        return this.fakePlayerName.compareTo(o.fakePlayerName);
    }

    public boolean remove() {
        return this.file.delete();
    }
}
