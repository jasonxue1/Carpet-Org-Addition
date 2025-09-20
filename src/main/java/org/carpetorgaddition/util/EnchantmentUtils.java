package org.carpetorgaddition.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.carpetorgaddition.wheel.TextBuilder;

import java.util.Optional;

public class EnchantmentUtils {
    /**
     * @return 指定物品上是否有指定附魔
     */
    public static boolean hasEnchantment(World world, RegistryKey<Enchantment> key, ItemStack itemStack) {
        Optional<Registry<Enchantment>> optional = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (optional.isEmpty()) {
            return false;
        }
        Enchantment enchantment = optional.get().get(key);
        return getLevel(world, enchantment, itemStack) > 0;
    }

    /**
     * @return 附魔是否与注册项对应
     */
    public static boolean isSpecified(World world, RegistryKey<Enchantment> key, Enchantment enchantment) {
        Optional<Registry<Enchantment>> optional = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (optional.isEmpty()) {
            return false;
        }
        Enchantment value = optional.get().get(key);
        return value != null && value.equals(enchantment);
    }

    /**
     * 判断指定魔咒是否是保护类魔咒
     *
     * @return 指定魔咒是否是 {@code 保护}、{@code 爆炸保护}，{@code 火焰保护}或{@code 弹射物保护}
     */
    public static boolean isProtectionEnchantment(RegistryKey<Enchantment> key) {
        return key == Enchantments.PROTECTION || key == Enchantments.BLAST_PROTECTION || key == Enchantments.FIRE_PROTECTION || key == Enchantments.PROJECTILE_PROTECTION;
    }

    /**
     * 判断指定魔咒是否为伤害类魔咒
     *
     * @return 指定魔咒是否是 {@code 锋利}、{@code 亡灵杀手}，{@code 节肢杀手}、{@code 穿刺}，{@code 致密}或{@code 破甲}
     */
    public static boolean isDamageEnchantment(RegistryKey<Enchantment> key) {
        return key == Enchantments.SHARPNESS || key == Enchantments.SMITE || key == Enchantments.BANE_OF_ARTHROPODS || key == Enchantments.IMPALING || key == Enchantments.DENSITY || key == Enchantments.BREACH;
    }

    /**
     * @return 获取指定物品上指定附魔的等级
     */
    public static int getLevel(World world, Enchantment enchantment, ItemStack itemStack) {
        Optional<Registry<Enchantment>> optional = world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT);
        if (optional.isEmpty()) {
            return 0;
        }
        RegistryEntry<Enchantment> entry = optional.get().getEntry(enchantment);
        if (itemStack.isOf(Items.ENCHANTED_BOOK)) {
            ItemEnchantmentsComponent component = itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
            return component.getLevel(entry);
        }
        return EnchantmentHelper.getLevel(entry, itemStack);
    }

    /**
     * @return 获取一个附魔的名字，不带等级
     */
    public static Text getName(Enchantment enchantment) {
        TextBuilder builder = new TextBuilder(enchantment.description());
        RegistryEntry<Enchantment> entry = RegistryEntry.of(enchantment);
        // 如果是诅咒附魔，设置为红色，否则，设置为灰色
        Formatting color = entry.isIn(EnchantmentTags.CURSE) ? Formatting.RED : Formatting.GRAY;
        builder.setColor(color);
        return builder.build();
    }

    /**
     * @param level 附魔的等级
     * @return 获取一个附魔的名字，带有等级
     */
    public static Text getName(Enchantment enchantment, int level) {
        Text mutableText = getName(enchantment);
        if (level != 1 || enchantment.getMaxLevel() != 1) {
            mutableText = TextBuilder.combineAll(mutableText, ScreenTexts.SPACE, TextBuilder.translate("enchantment.level." + level));
        }
        return mutableText;
    }

    /**
     * @return 指定物品是否可以使用经验修复
     */
    public static boolean canRepairWithXp(ItemStack itemStack) {
        ItemEnchantmentsComponent component = itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : component.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> registryEntry = entry.getKey();
            if (registryEntry.value().effects().contains(EnchantmentEffectComponentTypes.REPAIR_WITH_XP)) {
                return true;
            }
        }
        return false;
    }
}
