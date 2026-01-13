package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.CarpetOrgAddition;
import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.command.PlayerManagerCommand;
import boat.carpetorgaddition.dataupdate.json.DataUpdater;
import boat.carpetorgaddition.dataupdate.json.player.FakePlayerSerializeDataUpdater;
import boat.carpetorgaddition.periodic.ServerComponentCoordinator;
import boat.carpetorgaddition.periodic.fakeplayer.action.FakePlayerActionSerializer;
import boat.carpetorgaddition.periodic.task.FakePlayerStartupActionTask;
import boat.carpetorgaddition.periodic.task.ServerTaskManager;
import boat.carpetorgaddition.periodic.task.schedule.DelayedLoginTask;
import boat.carpetorgaddition.util.*;
import boat.carpetorgaddition.wheel.WorldFormat;
import boat.carpetorgaddition.wheel.provider.CommandProvider;
import boat.carpetorgaddition.wheel.provider.TextProvider;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import boat.carpetorgaddition.wheel.text.TextJoiner;
import carpet.fakes.ServerPlayerInterface;
import carpet.patches.EntityPlayerMPFake;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
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
    @NotNull
    private String comment = "";
    /**
     * 位置
     */
    @NotNull
    private final Vec3 playerPos;
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
    private final GameType gameMode;
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
    private final EnumMap<FakePlayerStartupAction, Integer> startups = new EnumMap<>(FakePlayerStartupAction.class);
    /**
     * 当前对象是否已经修改，即是否需要重新保存
     */
    private boolean isChanged = false;
    private final File file;

    public FakePlayerSerializer(EntityPlayerMPFake fakePlayer) {
        this.fakePlayerName = ServerUtils.getPlayerName(fakePlayer);
        this.playerPos = ServerUtils.getFootPos(fakePlayer);
        this.yaw = fakePlayer.getYRot();
        this.pitch = fakePlayer.getXRot();
        this.dimension = ServerUtils.getDimensionId(ServerUtils.getWorld(fakePlayer));
        this.gameMode = fakePlayer.gameMode.getGameModeForPlayer();
        this.flying = fakePlayer.getAbilities().flying;
        this.sneaking = fakePlayer.isShiftKeyDown();
        this.interactiveAction = new EntityPlayerActionPackSerial(((ServerPlayerInterface) fakePlayer).getActionPack());
        this.autoAction = new FakePlayerActionSerializer(fakePlayer);
        this.file = new WorldFormat(ServerUtils.getServer(fakePlayer), PlayerSerializationManager.PLAYER_DATA).file(this.fakePlayerName, "json");
    }

    public FakePlayerSerializer(EntityPlayerMPFake fakePlayer, FakePlayerSerializer serializer) {
        this(fakePlayer);
        // this.groups可能传入一个null
        this.groups.addAll(serializer.getGroups());
        this.startups.putAll(serializer.startups);
        this.autologin = serializer.autologin;
        this.setComment(serializer.comment);
        this.isChanged = true;
    }

    public FakePlayerSerializer(EntityPlayerMPFake fakePlayer, String comment) {
        this(fakePlayer);
        this.setComment(comment);
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
        this.playerPos = new Vec3(pos.get("x").getAsDouble(), pos.get("y").getAsDouble(), pos.get("z").getAsDouble());
        // 获取朝向
        JsonObject direction = json.get("direction").getAsJsonObject();
        this.yaw = direction.get("yaw").getAsFloat();
        this.pitch = direction.get("pitch").getAsFloat();
        // 维度
        this.dimension = json.get("dimension").getAsString();
        // 游戏模式
        this.gameMode = GameType.byName(json.get("gamemode").getAsString());
        // 是否飞行
        this.flying = json.get("flying").getAsBoolean();
        // 是否潜行
        this.sneaking = json.get("sneaking").getAsBoolean();
        // 是否自动登录
        this.autologin = IOUtils.getJsonElement(json, "autologin", false, Boolean.class);
        // 注释
        JsonElement element = json.get("annotation");
        this.comment = (element == null ? "" : element.getAsString());
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
        // 玩家启动动作
        JsonElement startup = json.get("startup");
        if (startup != null && startup.isJsonArray()) {
            JsonArray array = startup.getAsJsonArray();
            for (JsonElement jsonElement : array) {
                JsonObject object = jsonElement.getAsJsonObject();
                int delay = object.has("delay") ? object.get("delay").getAsInt() : 1;
                String action = object.get("action").getAsString();
                Optional<FakePlayerStartupAction> optional = FakePlayerStartupAction.fromString(action);
                if (optional.isEmpty()) {
                    continue;
                }
                this.startups.put(optional.get(), delay);
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
        if (server.getPlayerList().getPlayerByName(this.fakePlayerName) != null) {
            throw CommandUtils.createException(PlayerManagerCommand.KEY.then("spawn", "player_exist").translate());
        }
        CommandSourceStack source = server.createCommandSourceStack();
        ServerTaskManager taskManager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        Consumer<EntityPlayerMPFake> consumer = fakePlayer -> {
            fakePlayer.setShiftKeyDown(this.sneaking);
            // 设置玩家动作
            this.interactiveAction.startAction(fakePlayer);
            this.autoAction.clearPlayer();
            this.autoAction.startAction(fakePlayer);
            for (Map.Entry<FakePlayerStartupAction, Integer> entry : this.startups.entrySet()) {
                FakePlayerStartupActionTask task = new FakePlayerStartupActionTask(source, fakePlayer, entry.getKey(), entry.getValue());
                CommandUtils.handlingException(() -> taskManager.addTask(task), source);
            }
        };
        // 生成假玩家
        ServerUtils.createFakePlayer(
                this.fakePlayerName,
                server,
                this.playerPos,
                this.yaw,
                this.pitch,
                ServerUtils.getWorld(this.dimension),
                this.gameMode,
                this.flying,
                consumer
        );
    }

    // 显示文本信息
    public Component info() {
        TextJoiner joiner = new TextJoiner();
        // 玩家位置
        String pos = MathUtils.numberToTwoDecimalString(this.playerPos.x()) + " "
                     + MathUtils.numberToTwoDecimalString(this.playerPos.y()) + " "
                     + MathUtils.numberToTwoDecimalString(this.playerPos.z());
        LocalizationKey key = PlayerManagerCommand.KEY.then("info");
        joiner.newline(key.then("pos").translate(pos));
        // 获取朝向
        joiner.newline(key.then("direction").translate(
                MathUtils.numberToTwoDecimalString(this.yaw),
                MathUtils.numberToTwoDecimalString(this.pitch))
        );
        // 维度
        joiner.newline(key.then("dimension").translate(TextProvider.dimension(this.dimension)));
        // 游戏模式
        joiner.newline(key.then("gamemode").translate(this.gameMode.getLongDisplayName()));
        // 是否飞行
        joiner.newline(key.then("flying").translate(TextProvider.getBoolean(this.flying)));
        // 是否潜行
        joiner.newline(key.then("sneaking").translate(TextProvider.getBoolean(this.sneaking)));
        // 是否自动登录
        joiner.newline(key.then("autologin").translate(TextProvider.getBoolean(this.autologin)));
        if (this.interactiveAction.hasAction()) {
            joiner.newline(this.interactiveAction.getDisplayText(key));
        }
        if (this.autoAction.hasAction()) {
            joiner.newline(key.then("action").translate());
            joiner.enter(this.autoAction.getDisplayName());
        }
        if (!this.startups.isEmpty()) {
            LocalizationKey startupKey = key.then("startup");
            joiner.newline(startupKey.translate());
            joiner.enter(() -> {
                for (Map.Entry<FakePlayerStartupAction, Integer> entry : this.startups.entrySet()) {
                    joiner.newline(entry.getKey().getDisplayName(startupKey));
                    int delay = entry.getValue();
                    if (delay > 1) {
                        joiner.enter(startupKey.then("delay").translate(delay));
                    }
                }
            });
        }
        if (this.hasComment()) {
            // 添加注释
            joiner.newline(key.then("comment").translate(this.comment));
        }
        return joiner.join();
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
        json.addProperty("gamemode", this.gameMode.getName());
        // 是否飞行
        json.addProperty("flying", this.flying);
        // 是否潜行
        json.addProperty("sneaking", this.sneaking);
        // 自动登录
        json.addProperty("autologin", this.autologin);
        // 注释
        json.addProperty("annotation", this.getComment());
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
        JsonArray startup = new JsonArray();
        for (Map.Entry<FakePlayerStartupAction, Integer> entry : this.startups.entrySet()) {
            JsonObject action = new JsonObject();
            action.addProperty("delay", entry.getValue());
            action.addProperty("action", entry.getKey().toString());
            startup.add(action);
        }
        json.add("startup", startup);
        return json;
    }

    // 修改注释
    public void setComment(@Nullable String comment) {
        this.comment = comment == null ? "" : comment;
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
    public Component getDisplayName() {
        return new TextBuilder(this.fakePlayerName).setHover(this.info()).build();
    }

    public Supplier<Component> toTextSupplier() {
        return this::toText;
    }

    private Component toText() {
        String name = this.getFakePlayerName();
        String logonCommand = CommandProvider.playerManagerSpawn(name);
        String logoutCommand = CommandProvider.killFakePlayer(name);
        Component login = new TextBuilder("[↑]")
                .setCommand(logonCommand)
                .setHover(LocalizationKeys.Button.LOGIN.translate())
                .setColor(ChatFormatting.GREEN)
                .build();
        Component logout = new TextBuilder("[↓]")
                .setCommand(logoutCommand)
                .setHover(LocalizationKeys.Button.LOGOUT.translate())
                .setColor(ChatFormatting.RED)
                .build();
        Component info = new TextBuilder("[?]")
                .setHover(this.info())
                .setColor(ChatFormatting.GRAY)
                .build();
        TextJoiner joiner = new TextJoiner();
        joiner.append(login)
                .space()
                .append(logout)
                .space()
                .append(info)
                .space()
                .append(name);
        if (this.hasComment()) {
            TextBuilder builder = new TextBuilder("    // " + this.getComment());
            builder.setGrayItalic();
            joiner.append(builder.build());
        }
        return joiner.join();
    }

    /**
     * 假玩家自动登录
     */
    public static void autoLogin(MinecraftServer server) {
        ServerTaskManager manager = ServerComponentCoordinator.getCoordinator(server).getServerTaskManager();
        try {
            List<FakePlayerSerializer> list = FetcherUtils.getFakePlayerSerializationManager(server).list();
            int count = server.getPlayerCount();
            for (FakePlayerSerializer serializer : list) {
                if (serializer.autologin) {
                    manager.addTask(new DelayedLoginTask(server, server.createCommandSourceStack(), serializer, 1) {
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
                    if (count >= server.getMaxPlayers() - 1) {
                        CarpetOrgAddition.LOGGER.warn("The number of server players is about to reach its limit");
                        break;
                    }
                }
            }
        } catch (RuntimeException | CommandSyntaxException e) {
            CarpetOrgAddition.LOGGER.error("Unexpected error occurred during player automatic login: ", e);
        }
    }

    public boolean hasComment() {
        return !this.comment.isEmpty();
    }

    @NotNull
    public String getComment() {
        return this.comment;
    }

    public boolean isChanged() {
        return this.isChanged;
    }

    public Set<String> getGroups() {
        return this.groups.isEmpty() ? Collections.singleton(null) : Collections.unmodifiableSet(this.groups);
    }

    public void addStartupFunction(FakePlayerStartupAction action, int delay) {
        if (delay == -1) {
            this.startups.remove(action);
        } else {
            this.startups.put(action, delay);
        }
        this.isChanged = true;
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
