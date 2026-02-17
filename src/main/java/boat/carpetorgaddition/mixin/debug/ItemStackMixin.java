package boat.carpetorgaddition.mixin.debug;

import boat.carpetorgaddition.debug.DebugSettings;
import boat.carpetorgaddition.debug.OnlyDeveloped;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@OnlyDeveloped
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Unique
    private final ItemStack self = (ItemStack) (Object) this;

    @Inject(method = "addDetailsToTooltip", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/DefaultedRegistry;getKey(Ljava/lang/Object;)Lnet/minecraft/resources/Identifier;"))
    private void addItemTagTooltip(Item.TooltipContext context, TooltipDisplay display, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> builder, CallbackInfo ci) {
        if (DebugSettings.displayItemTagTooltip.get()) {
            this.self.tags()
                    .map(TagKey::location)
                    .map(Identifier::toString)
                    .sorted()
                    .map("#%s"::formatted)
                    .map(TextBuilder::of)
                    .map(text -> text.setColor(ChatFormatting.GRAY))
                    .map(TextBuilder::build)
                    .forEach(builder);
        }
    }
}
