package boat.carpetorgaddition.mixin.util;

import boat.carpetorgaddition.client.util.ClientUtils;
import boat.carpetorgaddition.wheel.inventory.WithButtonPlayerInventory;
import boat.carpetorgaddition.wheel.screen.WithButtonScreenClientSide;
import boat.carpetorgaddition.wheel.text.LocalizationKeys;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(Item.class)
public class ItemMixin {
    @Environment(EnvType.CLIENT)
    @Inject(method = "appendHoverText", at = @At("HEAD"))
    private void addTooltip(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag, CallbackInfo ci) {
        AbstractContainerMenu menun = ClientUtils.getContainerMenun();
        WithButtonScreenClientSide buttonMenu = (WithButtonScreenClientSide) menun;
        boolean withButton = buttonMenu.carpet_Org_Addition$isWithButton();
        if (withButton) {
            CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
            if (customData == null) {
                return;
            }
            if (customData.copyTag().get(WithButtonPlayerInventory.STOP_BUTTON_ITEM) != null) {
                builder.accept(LocalizationKeys.Button.Action.Stop.RIGHT.builder().setGrayItalic().build());
            }
        }
    }
}
