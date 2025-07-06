package org.carpetorgaddition.mixin.rule.quickshulker;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.collection.DefaultedList;
import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerUtils;
import org.carpetorgaddition.util.InventoryUtils;
import org.carpetorgaddition.util.MathUtils;
import org.carpetorgaddition.util.ScreenUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public abstract class ScreenHandlerMixin {
    @Shadow
    @Final
    public DefaultedList<Slot> slots;

    @Shadow
    public abstract Slot getSlot(int index);

    @Shadow
    public abstract ItemStack getCursorStack();

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo ci) {
        if (CarpetOrgAdditionSettings.quickShulker.get() && MathUtils.isInRange(0, this.slots.size(), slotIndex) && actionType == SlotActionType.PICKUP && button == FakePlayerUtils.PICKUP_RIGHT_CLICK) {
            ItemStack stack = this.getSlot(slotIndex).getStack();
            if (this.canOpenShulker() && InventoryUtils.isOperableSulkerBox(stack) && this.getCursorStack().isEmpty()) {
                // 创造模式物品栏是一个客户端屏幕，因此点击潜影盒不会打开物品栏
                if (player instanceof ServerPlayerEntity) {
                    ScreenUtils.openShulkerScreenHandler((ServerPlayerEntity) player, stack);
                }
                ci.cancel();
            }
        }
    }

    /**
     * @return 当前屏幕是否可以打开潜影盒
     */
    @Unique
    protected boolean canOpenShulker() {
        return true;
    }
}
