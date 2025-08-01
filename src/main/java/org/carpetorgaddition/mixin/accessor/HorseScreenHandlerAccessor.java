package org.carpetorgaddition.mixin.accessor;

import net.minecraft.screen.HorseScreenHandler;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseScreenHandler.class)
public interface HorseScreenHandlerAccessor {
    @Accessor("EMPTY_SADDLE_SLOT_TEXTURE")
    static Identifier getEmptySaddleSlotTexture() {
        throw new AssertionError();
    }

    @Accessor("EMPTY_HORSE_ARMOR_SLOT_TEXTURE")
    static Identifier getEmptyHorseArmorSlotTexture() {
        throw new AssertionError();
    }
}
