package org.carpetorgaddition.wheel;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record CreateFakePlayerContext(
        Vec3d pos,
        double yaw,
        double pitch,
        RegistryKey<World> dimension,
        GameMode gamemode,
        boolean flying,
        @NotNull
        Consumer<EntityPlayerMPFake> consumer
) {
    @SuppressWarnings("unused")
    public CreateFakePlayerContext(ServerPlayerEntity player) {
        this(player, GenericUtils::pass);
    }

    public CreateFakePlayerContext(ServerPlayerEntity player, @NotNull Consumer<EntityPlayerMPFake> consumer) {
        this(player.getPos(),
                player.getYaw(),
                player.getPitch(),
                FetcherUtils.getWorld(player).getRegistryKey(),
                player.interactionManager.getGameMode(),
                player.getAbilities().flying,
                consumer
        );
    }
}
