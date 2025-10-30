package org.carpetorgaddition.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.biome.Biome;
import org.carpetorgaddition.client.command.argument.ClientObjectArgumentType;
import org.carpetorgaddition.client.util.ClientMessageUtils;
import org.carpetorgaddition.client.util.ClientUtils;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.carpetorgaddition.wheel.TextBuilder;
import org.carpetorgaddition.wheel.provider.TextProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class DictionaryCommand extends AbstractClientCommand {
    public static final String DEFAULT_COMMAND_NAME = "dictionary";
    private static final String UNREGISTERED = "[<unregistered>]";

    public DictionaryCommand(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess access) {
        super(dispatcher, access);
    }

    @Override
    public void register(String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> builder = ClientCommandManager.literal(name);
        // 注册每一项子命令
        for (DictionaryType value : DictionaryType.values()) {
            builder.then(ClientCommandManager.literal(value.name)
                    .then(ClientCommandManager.argument(value.name, value.getArgumentType())
                            .executes(context -> getId(context, value))));
        }
        this.dispatcher.register(builder);
    }

    // 获取对象id
    private int getId(CommandContext<FabricClientCommandSource> context, DictionaryType type) {
        List<?> list = ClientObjectArgumentType.getType(context, type.name);
        // list集合至少有一个元素，不与任何对象匹配的字符串在解析命令时不会成功
        if (list.size() == 1) {
            // 字符串只对应一个对象
            Object t = list.getFirst();
            // 获取对象id
            String id = type.id(t);
            sendFeedback(type.name(t), id);
        } else {
            // 字符串对应多个对象
            sendFeedback(list.size());
            for (Object t : list) {
                sendFeedback(type.id(t));
            }
        }
        return list.size();
    }

    // 发送命令反馈
    private void sendFeedback(Text text, String id) {
        ClientMessageUtils.sendMessage("carpet.client.commands.dictionary.id", text, canCopyId(id));
    }

    private void sendFeedback(int count) {
        ClientMessageUtils.sendMessage("carpet.client.commands.dictionary.multiple.id", count);
    }

    private void sendFeedback(String id) {
        ClientMessageUtils.sendMessage("carpet.client.commands.dictionary.multiple.each", canCopyId(id));
    }

    // 将字符串id转换成可以单击复制的形式
    @NotNull
    private Text canCopyId(String id) {
        return new TextBuilder(id)
                .setCopyToClipboard(id)
                .setHover(TextProvider.COPY_CLICK)
                .setColor(Formatting.GREEN)
                .build();
    }

    @Override
    public String getDefaultName() {
        return DEFAULT_COMMAND_NAME;
    }

    public enum DictionaryType {
        /**
         * 物品
         */
        ITEM("item"),
        /**
         * 方块
         */
        BLOCK("block"),
        /**
         * 实体
         */
        ENTITY("entity"),
        /**
         * 附魔
         */
        ENCHANTMENT("enchant"),
        /**
         * 状态效果
         */
        STATUS_EFFECT("effect"),
        /**
         * 生物群系
         */
        BIOME("biome"),
        /**
         * 游戏模式
         */
        GAME_MODE("gamemode"),
        /**
         * 游戏规则
         */
        GAME_RULE("gamerule");
        private final String name;

        DictionaryType(String name) {
            this.name = name;
        }

        // 获取对象id
        private String id(Object obj) {
            ClientPlayerEntity player = ClientUtils.getPlayer();
            DynamicRegistryManager.Immutable registry = player.networkHandler.getRegistryManager();
            return switch (this) {
                case ITEM -> Registries.ITEM.getId((Item) obj).toString();
                case BLOCK -> Registries.BLOCK.getId((Block) obj).toString();
                case ENTITY -> Registries.ENTITY_TYPE.getId((EntityType<?>) obj).toString();
                case ENCHANTMENT -> Optional.ofNullable(registry.get(RegistryKeys.ENCHANTMENT).getId((Enchantment) obj))
                        .map(Identifier::toString)
                        .orElse(UNREGISTERED);
                case STATUS_EFFECT ->
                        Optional.ofNullable(registry.get(RegistryKeys.STATUS_EFFECT).getId((StatusEffect) obj))
                                .map(Identifier::toString)
                                .orElse(UNREGISTERED);
                case BIOME -> Optional.ofNullable(registry.get(RegistryKeys.BIOME).getId((Biome) obj))
                        .map(Identifier::toString)
                        .orElse(UNREGISTERED);
                case GAME_MODE -> ((GameMode) obj).asString();
                case GAME_RULE -> ((GameRules.Key<?>) obj).getName();
            };
        }

        // 获取对象名称
        private Text name(Object obj) {
            // 获取客户端玩家
            ClientPlayerEntity player = ClientUtils.getPlayer();
            // 获取注册管理器
            DynamicRegistryManager.Immutable registry = player.networkHandler.getRegistryManager();
            return switch (this) {
                case ITEM -> ((Item) obj).getName();
                case BLOCK -> ((Block) obj).getName();
                case ENTITY -> ((EntityType<?>) obj).getName();
                case ENCHANTMENT -> EnchantmentUtils.getName((Enchantment) obj);
                case STATUS_EFFECT -> ((StatusEffect) obj).getName();
                case BIOME -> Optional.ofNullable(registry.get(RegistryKeys.BIOME).getId((Biome) obj))
                        .map(identifier -> identifier.toTranslationKey("biome"))
                        .map(TextBuilder::translate)
                        .orElse(TextBuilder.create(UNREGISTERED));
                case GAME_MODE -> ((GameMode) obj).getTranslatableName();
                case GAME_RULE -> TextBuilder.translate(((GameRules.Key<?>) obj).getTranslationKey());
            };
        }

        // 获取参数类型
        private ArgumentType<?> getArgumentType() {
            return switch (this) {
                case ITEM -> new ClientObjectArgumentType.ClientItemArgumentType(false);
                case BLOCK -> new ClientObjectArgumentType.ClientBlockArgumentType(false);
                case ENTITY -> new ClientObjectArgumentType.ClientEntityArgumentType();
                case ENCHANTMENT -> new ClientObjectArgumentType.ClientEnchantmentArgumentType();
                case STATUS_EFFECT -> new ClientObjectArgumentType.ClientStatusEffectArgumentType();
                case BIOME -> new ClientObjectArgumentType.ClientBiomeArgumentType();
                case GAME_MODE -> new ClientObjectArgumentType.ClientGameModeArgumentType();
                case GAME_RULE -> new ClientObjectArgumentType.ClientGameRuleArgumentType();
            };
        }
    }
}
