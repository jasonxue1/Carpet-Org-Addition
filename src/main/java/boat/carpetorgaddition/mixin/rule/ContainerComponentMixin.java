package boat.carpetorgaddition.mixin.rule;

import boat.carpetorgaddition.mixin.accessor.ContainerComponentAccessor;
import boat.carpetorgaddition.wheel.ContainerDeepCopy;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemContainerContents.class)
public class ContainerComponentMixin implements ContainerDeepCopy {

    @Shadow
    @Final
    private NonNullList<ItemStack> items;

    @Override
    public ItemContainerContents carpet_Org_Addition$copy() {
        NonNullList<ItemStack> list = this.items;
        NonNullList<ItemStack> copy = NonNullList.withSize(list.size(), ItemStack.EMPTY);
        for (int index = 0; index < copy.size(); index++) {
            copy.set(index, list.get(index).copy());
        }
        return ContainerComponentAccessor.constructor(copy);
    }
}
