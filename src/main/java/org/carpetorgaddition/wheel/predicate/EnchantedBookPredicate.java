package org.carpetorgaddition.wheel.predicate;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.carpetorgaddition.util.EnchantmentUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.function.Predicate;

public class EnchantedBookPredicate implements Predicate<ItemStack> {
    private final MinecraftServer server;
    private final Enchantment enchantment;
    private final String id;

    public EnchantedBookPredicate(MinecraftServer server, Enchantment enchantment) {
        this.server = server;
        this.enchantment = enchantment;
        this.id = GenericUtils.getId(server, this.enchantment)
                .map(Identifier::toString)
                .orElse("<unregistered>");
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return itemStack.isOf(Items.ENCHANTED_BOOK) && getLevel(itemStack) > 0;
    }

    public int getLevel(ItemStack itemStack) {
        int level = EnchantmentUtils.getLevel(this.server, this.enchantment, itemStack);
        if (level > 0) {
            return level;
        }
        return -1;
    }

    public Text getWithLevel(int level) {
        TextBuilder builder = new TextBuilder(EnchantmentUtils.getName(this.enchantment, level));
        builder.setStringHover(this.id);
        return builder.build();
    }

    public Text getDisplayName() {
        // 获取附魔名称，不带等级
        TextBuilder builder = new TextBuilder(EnchantmentUtils.getName(enchantment));
        builder.setStringHover(this.id);
        return builder.build();
    }
}
