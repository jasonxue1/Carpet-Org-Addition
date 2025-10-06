package org.carpetorgaddition.wheel;

import carpet.CarpetServer;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.ItemPredicateArgumentType;
import net.minecraft.command.argument.ItemPredicateArgumentType.ItemStackPredicateArgument;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.carpetorgaddition.util.GenericUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class ItemStackPredicate implements Predicate<ItemStack> {
    private final Predicate<ItemStack> predicate;
    private final String input;
    private final boolean isWildcard;
    @Nullable
    private final Item convert;
    public static final ItemStackPredicate EMPTY = new ItemStackPredicate(Items.AIR);
    /**
     * 匹配任何非空气物品
     */
    public static final ItemStackPredicate WILDCARD = new ItemStackPredicate();

    /**
     * 创建一个匹配任意非空气物品的物品谓词
     */
    private ItemStackPredicate() {
        this.predicate = itemStack -> !itemStack.isEmpty();
        this.input = "*";
        this.isWildcard = true;
        this.convert = null;
    }

    public ItemStackPredicate(CommandContext<ServerCommandSource> context, String arguments) {
        for (ParsedCommandNode<ServerCommandSource> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                this.input = context.getInput().substring(range.getStart(), range.getEnd());
                ItemStackPredicateArgument predicate = ItemPredicateArgumentType.getItemStackPredicate(context, arguments);
                this.isWildcard = this.isWildcard();
                this.predicate = this.isWildcard ? itemStack -> !itemStack.isEmpty() && predicate.test(itemStack) : predicate;
                this.convert = tryConvert(this.input);
                return;
            }
        }
        throw new IllegalArgumentException();
    }

    public ItemStackPredicate(@NotNull Item item) {
        this.predicate = itemStack -> itemStack.isOf(item);
        this.input = GenericUtils.getIdAsString(item);
        this.isWildcard = false;
        this.convert = item;
    }

    public ItemStackPredicate(Collection<Item> collection) {
        Set<Item> set = new LinkedHashSet<>(collection);
        switch (set.size()) {
            case 0 -> throw new IllegalArgumentException();
            case 1 -> {
                ItemStackPredicate stackPredicate = new ItemStackPredicate(set.iterator().next());
                this.predicate = stackPredicate.predicate;
                this.input = stackPredicate.input;
                this.isWildcard = stackPredicate.isWildcard;
                this.convert = stackPredicate.convert;
            }
            default -> {
                this.predicate = itemStack -> {
                    for (Item item : set) {
                        if (itemStack.isOf(item)) {
                            return true;
                        }
                    }
                    return false;
                };
                StringJoiner joiner = new StringJoiner("|", "[", "]");
                for (Item item : set) {
                    joiner.add(GenericUtils.getIdAsString(item));
                }
                this.input = joiner.toString();
                this.isWildcard = false;
                this.convert = null;
            }
        }
    }

    private ItemStackPredicate(Predicate<ItemStack> predicate, String input) {
        this.input = input;
        this.isWildcard = this.isWildcard();
        this.predicate = this.isWildcard ? itemStack -> !itemStack.isEmpty() && predicate.test(itemStack) : predicate;
        this.convert = tryConvert(input);
    }

    @Nullable
    private static Item tryConvert(String input) {
        if (input.startsWith("#") || input.startsWith("*") || input.matches(".*\\[.*]")) {
            return null;
        }
        try {
            return Registries.ITEM.get(Identifier.of(input));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * @return 自身是否可以与空气物品匹配
     */
    public boolean isEmpty() {
        return this.test(ItemStack.EMPTY);
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return this.predicate.test(itemStack);
    }

    /**
     * 从字符串解析物品谓词
     *
     * @throws NullPointerException 如果在游戏外加载物品谓词则抛出
     */
    public static ItemStackPredicate parse(String input) {
        CommandRegistryAccessor accessor = (CommandRegistryAccessor) CarpetServer.minecraft_server.getCommandManager();
        CommandRegistryAccess access = accessor.carpet_Org_Addition$getAccess();
        try {
            StringReader reader = new StringReader(input);
            ItemStackPredicateArgument predicate = ItemPredicateArgumentType.itemPredicate(access).parse(reader);
            return new ItemStackPredicate(predicate, input);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return 当前谓词字符串是否为通配符
     */
    private boolean isWildcard() {
        return "*".equals(this.input) || "*[]".equals(this.input);
    }

    /**
     * @return 谓词的字符串首字母
     */
    public Text getInitialUpperCase() {
        if (this.isEmpty()) {
            TextBuilder builder = new TextBuilder("[A]");
            builder.setColor(Formatting.DARK_GRAY);
            builder.setHover(Items.AIR.getName());
            return builder.build();
        }
        TextBuilder builder = null;
        if (this.input.startsWith("#")) {
            builder = new TextBuilder("[#]");
        } else if (this.input.startsWith("*")) {
            builder = new TextBuilder("[*]");
        } else if (this.input.contains("[")) {
            // 不会执行到这里
            builder = new TextBuilder("[@]");
        }
        if (builder != null) {
            return builder.setHover(this.input).build();
        }
        // 如果有命名空间，将“:”后的单词首字母取出，否则直接获取首字母
        String[] split = this.input.split(":");
        int index = split.length == 1 ? 0 : 1;
        builder = new TextBuilder("[" + Character.toUpperCase(split[index].charAt(0)) + "]");
        Text name = Registries.ITEM.get(Identifier.of(this.input)).getName();
        return builder.setHover(name).build();
    }

    /**
     * 将当前命令参数输入转换成文本对象
     *
     * @return 如果是谓词，返回封装的字符串，如果是物品，返回物品名称
     * @throws InvalidIdentifierException 如果解析到了不正确的字符串
     */
    public Text toText() throws InvalidIdentifierException {
        if (this.isEmpty()) {
            return Items.AIR.getName();
        }
        if (this.isWildcard) {
            return TextBuilder.translate("carpet.command.item.predicate.wildcard");
        }
        if (this.convert != null) {
            return this.convert.getName();
        }
        if (this.input.length() > 30) {
            String substring = this.input.substring(0, 30);
            Text ellipsis = TextBuilder.create("...");
            Text result = TextBuilder.combineAll(substring, ellipsis);
            TextBuilder builder = new TextBuilder(result).setGrayItalic().setHover(this.input);
            return builder.build();
        }
        return TextBuilder.create(this.input);
    }

    /**
     * 将命令参数转换为对应物品
     */
    @Deprecated(forRemoval = true)
    public Item asItem() {
        if (this.isEmpty()) {
            return Items.AIR;
        }
        if (this.isConvertible()) {
            return Registries.ITEM.get(Identifier.of(this.input));
        }
        throw new UnsupportedOperationException(this.input + " cannot be converted to item");
    }

    /**
     * 获取input转换而来的物品
     */
    public Optional<Item> getConvert() {
        return Optional.ofNullable(this.convert);
    }

    /**
     * @return {@code input}字符串是否可以转换为物品
     */
    @Deprecated(forRemoval = true)
    public boolean isConvertible() {
        return !(this.input.startsWith("#") || this.input.startsWith("*") || this.input.matches(".*\\[.*]"));
    }

    public String getInput() {
        return this.input;
    }

    @Override
    public String toString() {
        return this.input;
    }
}
