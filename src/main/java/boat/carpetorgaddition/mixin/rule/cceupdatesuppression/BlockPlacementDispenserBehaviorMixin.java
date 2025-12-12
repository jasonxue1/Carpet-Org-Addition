package boat.carpetorgaddition.mixin.rule.cceupdatesuppression;

import boat.carpetorgaddition.rule.RuleUtils;
import boat.carpetorgaddition.util.InventoryUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ShulkerBoxDispenseBehavior;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxDispenseBehavior.class)
public class BlockPlacementDispenserBehaviorMixin {
    // 更新抑制潜影盒在被发射器放置时移除自定义名称
    @Inject(method = "execute", at = @At("HEAD"))
    private void dispenseSilently(BlockSource pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (InventoryUtils.isShulkerBoxItem(stack) && RuleUtils.canUpdateSuppression(stack.getHoverName().getString())) {
            stack.remove(DataComponents.CUSTOM_NAME);
        }
    }
}
