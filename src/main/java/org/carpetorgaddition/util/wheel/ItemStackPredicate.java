package org.carpetorgaddition.util.wheel;

import carpet.CarpetServer;
import carpet.patches.EntityPlayerMPFake;
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
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public class ItemStackPredicate implements Predicate<ItemStack> {
    private final Predicate<ItemStack> predicate;
    private final String input;
    private final boolean isWildcard;
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
    }

    public ItemStackPredicate(CommandContext<ServerCommandSource> context, String arguments) {
        for (ParsedCommandNode<ServerCommandSource> commandNode : context.getNodes()) {
            if (commandNode.getNode() instanceof ArgumentCommandNode<?, ?> node && Objects.equals(node.getName(), arguments)) {
                StringRange range = commandNode.getRange();
                this.input = context.getInput().substring(range.getStart(), range.getEnd());
                ItemStackPredicateArgument predicate = ItemPredicateArgumentType.getItemStackPredicate(context, arguments);
                this.isWildcard = this.isWildcard();
                this.predicate = this.isWildcard ? itemStack -> !itemStack.isEmpty() && predicate.test(itemStack) : predicate;
                return;
            }
        }
        throw new IllegalArgumentException();
    }

    public ItemStackPredicate(Item item) {
        this.predicate = itemStack -> itemStack.isOf(item);
        this.input = Registries.ITEM.getId(item).toString();
        this.isWildcard = false;
    }

    private ItemStackPredicate(Predicate<ItemStack> predicate, String input) {
        this.input = input;
        this.isWildcard = this.isWildcard();
        this.predicate = this.isWildcard ? itemStack -> !itemStack.isEmpty() && predicate.test(itemStack) : predicate;
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
        CommandRegistryAccess access = accessor.getAccess();
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
        if (isConvertible()) {
            Identifier identifier = Identifier.of(this.input);
            return Registries.ITEM.get(identifier).getName();
        }
        if (this.input.length() > 30) {
            String substring = this.input.substring(0, 30);
            MutableText ellipsis = TextBuilder.create("...");
            MutableText result = TextBuilder.combineAll(substring, ellipsis);
            TextBuilder builder = new TextBuilder(result).setGrayItalic().setHover(this.input);
            return builder.build();
        }
        return TextBuilder.create(this.input);
    }

    /**
     * 将命令参数转换为对应物品
     */
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
     * 获取指定配方的输出物品
     *
     * @param predicates  合成配方
     * @param widthHeight 合成方格的宽高，工作台是3，物品栏是2
     * @param fakePlayer  合成该物品的假玩家
     * @return 如果能够合成物品，返回合成输出物品，否则返回空物品，如果配方中包含不能转换为物品的元素，也返回空物品
     */
    public static ItemStack getCraftOutput(ItemStackPredicate[] predicates, int widthHeight, EntityPlayerMPFake fakePlayer) {
        for (ItemStackPredicate predicate : predicates) {
            if (predicate.isConvertible()) {
                continue;
            }
            return ItemStack.EMPTY;
        }
        // 前面的循环中已经判断了字符串是否能转换成物品，所以这里不需要再判断
        List<ItemStack> list = Arrays.stream(predicates)
                .map(ItemStackPredicate::getInput)
                .map(Identifier::of)
                .map(Registries.ITEM::get)
                .map(Item::getDefaultStack)
                .toList();
        CraftingRecipeInput input = CraftingRecipeInput.create(widthHeight, widthHeight, list);
        World world = fakePlayer.getWorld();
        Optional<RecipeEntry<CraftingRecipe>> optional = fakePlayer.server.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, world);
        return optional.map(recipe -> recipe.value().craft(input, world.getRegistryManager())).orElse(ItemStack.EMPTY);
    }

    /**
     * 将字符串ID转换为物品
     */
    public static Item stringAsItem(String id) {
        return Registries.ITEM.get(Identifier.of(id));
    }

    /**
     * @return {@code input}字符串是否可以转换为物品
     */
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
