package org.carpetorgaddition.mixin.util;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.screen.CraftingScreenHandler;
import org.carpetorgaddition.periodic.fakeplayer.FakePlayerCraftRecipeInterface;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CraftingScreenHandler.class)
public abstract class CraftingScreenHandlerMixin implements FakePlayerCraftRecipeInterface {
    @Shadow
    @Final
    private RecipeInputInventory input;

    @Override
    public RecipeInputInventory carpet_Org_Addition$getInput() {
        return this.input;
    }
}
