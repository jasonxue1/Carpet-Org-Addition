package boat.carpetorgaddition.mixin.accessor;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.HorseInventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HorseInventoryMenu.class)
public interface HorseScreenHandlerAccessor {
    @Accessor("SADDLE_SLOT_SPRITE")
    static Identifier getEmptySaddleSlotTexture() {
        throw new AssertionError();
    }

    @Accessor("ARMOR_SLOT_SPRITE")
    static Identifier getEmptyHorseArmorSlotTexture() {
        throw new AssertionError();
    }
}
