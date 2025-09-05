package org.carpetorgaddition.mixin.accessor.fabricapi;

import net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("UnstableApiUsage")
@Mixin(value = AbstractChanneledNetworkAddon.class, remap = false)
public interface AbstractChanneledNetworkAddonAccessor {
    @Accessor("connection")
    ClientConnection getConnection();
}
