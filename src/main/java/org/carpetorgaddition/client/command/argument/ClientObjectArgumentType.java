package org.carpetorgaddition.client.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.biome.Biome;
import org.carpetorgaddition.client.util.ClientCommandUtils;
import org.carpetorgaddition.client.util.ClientUtils;
import org.carpetorgaddition.util.CommandUtils;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public abstract class ClientObjectArgumentType<T> implements ArgumentType<List<T>> {
    private static final List<String> PATTERNS = Arrays.stream(MatchPattern.values()).map(MatchPattern::toString).toList();
    /**
     * 字符串是否使用匹配模式
     */
    private final boolean patternMatching;

    private ClientObjectArgumentType() {
        this(false);
    }

    private ClientObjectArgumentType(boolean patternMatching) {
        this.patternMatching = patternMatching;
    }

    public static List<?> getType(CommandContext<FabricClientCommandSource> context, String name) {
        return context.getArgument(name, List.class);
    }

    @Override
    public List<T> parse(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        String name = ClientCommandUtils.readWord(reader);
        MatchPattern pattern = this.patternMatching ? readParameters(reader) : MatchPattern.EQUAL;
        // 由于可以使用资源包更改对象名称，因此一个名称可能对应多个对象
        ArrayList<T> list = new ArrayList<>();
        if (!name.isEmpty()) {
            for (T t : getRegistry().toList()) {
                // 获取所有与字符串对应的对象
                if (pattern.match(name.toLowerCase(Locale.ROOT), objectToString(t).toLowerCase(Locale.ROOT))) {
                    list.add(t);
                }
            }
        }
        // 没有对象与字符串对应
        if (list.isEmpty()) {
            reader.setCursor(cursor);
            throw CommandUtils.createException("carpet.client.commands.dictionary.not_matched");
        }
        // 字符串过于宽泛
        if (this.patternMatching && list.size() > 40 && pattern != MatchPattern.EQUAL) {
            reader.setCursor(cursor);
            throw CommandUtils.createException("carpet.client.command.string.broad");
        }
        return list;
    }

    private MatchPattern readParameters(StringReader reader) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        reader.skipWhitespace();
        String mode = reader.readUnquotedString().toLowerCase(Locale.ROOT);
        if (mode.startsWith("-")) {
            return switch (mode) {
                case "-equal" -> MatchPattern.EQUAL;
                case "-contain" -> MatchPattern.CONTAIN;
                case "-regex" -> MatchPattern.REGEX;
                default -> throw CommandUtils.createException("carpet.client.command.matching_pattern.invalid");
            };
        } else {
            reader.setCursor(cursor);
            return MatchPattern.EQUAL;
        }
    }

    /**
     * @return 对象的翻译后名称
     */
    protected abstract String objectToString(T t);

    /**
     * 列出命令建议
     */
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (context.getSource() instanceof CommandSource) {
            String[] array = getRegistry()
                    .map(this::objectToString)
                    .map(s -> s.contains(" ") ? "\"" + s + "\"" : s)
                    .toArray(String[]::new);
            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
            String[] split = this.splitArguments(remaining);
            if (this.patternMatching && (split.length > 1 || remaining.endsWith(" "))) {
                // 补全匹配模式
                StringReader reader = new StringReader(builder.getInput());
                // 跳过名称字符串和空格
                if (split.length > 0) {
                    reader.setCursor(builder.getStart() + split[0].length());
                }
                reader.skipWhitespace();
                SuggestionsBuilder offset = builder.createOffset(reader.getCursor());
                for (String pattern : PATTERNS) {
                    if (!reader.canRead() || (split.length > 1 && pattern.startsWith(split[1].toLowerCase(Locale.ROOT)))) {
                        offset.suggest(pattern);
                    }
                }
                return offset.buildFuture();
            } else {
                for (String candidate : array) {
                    // 列出所有名称中包含输入字符串的对象
                    if (candidate.toLowerCase(Locale.ROOT).contains(remaining)) {
                        builder.suggest(candidate);
                    }
                }
                return builder.buildFuture();
            }
        }
        return Suggestions.empty();
    }

    /**
     * 将参数切割为数组，允许被引号包裹的字符串中出现空格
     */
    private String[] splitArguments(String remaining) {
        ArrayList<String> list = new ArrayList<>();
        StringReader reader = new StringReader(remaining);
        while (reader.canRead()) {
            int cursor = reader.getCursor();
            try {
                ClientCommandUtils.readWord(reader);
                list.add(remaining.substring(cursor, reader.getCursor()));
                reader.skipWhitespace();
            } catch (CommandSyntaxException e) {
                break;
            }
        }
        return list.toArray(String[]::new);
    }

    /**
     * 获取对象对应的注册表
     */
    protected abstract Stream<T> getRegistry();

    /**
     * 物品参数
     */
    public static class ClientItemArgumentType extends ClientObjectArgumentType<Item> {
        public ClientItemArgumentType(boolean patternMatching) {
            super(patternMatching);
        }

        @Override
        protected String objectToString(Item item) {
            return item.getName().getString();
        }

        @Override
        protected Stream<Item> getRegistry() {
            return Registries.ITEM.stream();
        }
    }

    /**
     * 方块参数
     */
    public static class ClientBlockArgumentType extends ClientObjectArgumentType<Block> {
        public ClientBlockArgumentType(boolean patternMatching) {
            super(patternMatching);
        }

        @Override
        protected String objectToString(Block block) {
            return block.getName().getString();
        }

        @Override
        protected Stream<Block> getRegistry() {
            return Registries.BLOCK.stream();
        }
    }

    /**
     * 实体参数
     */
    public static class ClientEntityArgumentType extends ClientObjectArgumentType<EntityType<?>> {
        @Override
        protected String objectToString(EntityType<?> entityType) {
            return entityType.getName().getString();
        }

        @Override
        protected Stream<EntityType<?>> getRegistry() {
            ClientPlayerEntity player = ClientUtils.getPlayer();
            return player.networkHandler.getRegistryManager().getOrThrow(RegistryKeys.ENTITY_TYPE).stream();
        }
    }

    /**
     * 附魔参数
     */
    public static class ClientEnchantmentArgumentType extends ClientObjectArgumentType<Enchantment> {
        @Override
        protected String objectToString(Enchantment enchantment) {
            return EnchantmentUtils.getName(enchantment).getString();
        }

        @Override
        protected Stream<Enchantment> getRegistry() {
            Registry<Enchantment> registry = ClientUtils.getPlayer().networkHandler.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            return registry.stream();
        }
    }

    /**
     * 状态效果参数
     */
    public static class ClientStatusEffectArgumentType extends ClientObjectArgumentType<StatusEffect> {
        @Override
        protected String objectToString(StatusEffect statusEffect) {
            return statusEffect.getName().getString();
        }

        @Override
        protected Stream<StatusEffect> getRegistry() {
            ClientPlayerEntity player = ClientUtils.getPlayer();
            return player.networkHandler.getRegistryManager().getOrThrow(RegistryKeys.STATUS_EFFECT).stream();
        }
    }

    public static class ClientBiomeArgumentType extends ClientObjectArgumentType<Biome> {
        @Override
        protected String objectToString(Biome biome) {
            Registry<Biome> biomes = ClientUtils.getPlayer().networkHandler.getRegistryManager().getOrThrow(RegistryKeys.BIOME);
            return TextBuilder.translate(Objects.requireNonNull(biomes.getId(biome)).toTranslationKey("biome")).getString();

        }

        @Override
        protected Stream<Biome> getRegistry() {
            ClientPlayerEntity player = ClientUtils.getPlayer();
            return player.networkHandler.getRegistryManager().getOrThrow(RegistryKeys.BIOME).stream();
        }
    }

    /**
     * 游戏模式参数
     */
    public static class ClientGameModeArgumentType extends ClientObjectArgumentType<GameMode> {
        @Override
        protected String objectToString(GameMode gameMode) {
            return gameMode.getTranslatableName().getString();
        }

        @Override
        protected Stream<GameMode> getRegistry() {
            return Stream.of(GameMode.values());
        }
    }

    /**
     * 游戏规则参数
     */
    public static class ClientGameRuleArgumentType extends ClientObjectArgumentType<GameRules.Key<?>> {
        @Override
        protected String objectToString(GameRules.Key<?> key) {
            return TextBuilder.translate(key.getTranslationKey()).getString();
        }

        @Override
        protected Stream<GameRules.Key<?>> getRegistry() {
            ArrayList<GameRules.Key<?>> list = new ArrayList<>();
            new GameRules(FeatureSet.of(FeatureFlags.MINECART_IMPROVEMENTS)).accept(new GameRules.Visitor() {
                @Override
                public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                    list.add(key);
                }
            });
            return list.stream();
        }
    }

    public enum MatchPattern {
        /**
         * 完全匹配
         */
        EQUAL,
        /**
         * 包含
         */
        CONTAIN,
        /**
         * 正则表达式
         */
        REGEX;

        private boolean match(String arguments, String str) {
            return switch (this) {
                case EQUAL -> Objects.equals(str, arguments);
                case CONTAIN -> str.contains(arguments);
                case REGEX -> str.matches(arguments);
            };
        }

        @Override
        public String toString() {
            return switch (this) {
                case EQUAL -> "-equal";
                case CONTAIN -> "-contain";
                case REGEX -> "-regex";
            };
        }
    }
}
