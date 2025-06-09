package org.carpetorgaddition.util;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import org.carpetorgaddition.util.inventory.ContainerComponentInventory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;

public class InventoryUtilsTest {
    @BeforeAll
    public static void init() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @RepeatedTest(10)
    public void testAddItemToContainer() {
        ItemStack itemStack = new ItemStack(Items.SHULKER_BOX);
        ContainerComponent component = itemStack.get(DataComponentTypes.CONTAINER);
        Assertions.assertNotNull(component);
        List<ItemStack> list = component.stream().toList();
        Assertions.assertTrue(list.isEmpty());
        ContainerComponentInventory inventory = new ContainerComponentInventory(itemStack);
        int count = 0;
        while (true) {
            Item item = MathUtils.getRandomElement(Registries.ITEM.stream().toList());
            ItemStack stack = new ItemStack(item);
            int i = MathUtils.randomInt(1, stack.getMaxCount());
            stack.setCount(i);
            if (stack.isEmpty()) {
                continue;
            }
            count += i;
            System.out.println(stack);
            ItemStack remaining = inventory.addStack(stack);
            if (remaining.isEmpty()) {
                continue;
            }
            count -= remaining.getCount();
            break;
        }
        ContainerComponent newComponent = itemStack.get(DataComponentTypes.CONTAINER);
        System.out.println(inventory);
        Assertions.assertNotNull(newComponent);
        Assertions.assertEquals(27, newComponent.stream().toList().size());
        Assertions.assertEquals(count, inventory.count(stack -> true));
    }
}
