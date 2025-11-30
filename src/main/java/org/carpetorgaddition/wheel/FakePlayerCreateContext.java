package org.carpetorgaddition.wheel;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;

import java.util.function.Consumer;

public record FakePlayerCreateContext(
        Vec3d pos,
        double yaw,
        double pitch,
        RegistryKey<World> dimension,
        GameMode gamemode,
        boolean flying,
        Consumer<EntityPlayerMPFake> consumer
) {
    @SuppressWarnings("unused")
    public FakePlayerCreateContext(ServerPlayerEntity player) {
        this(player, GenericUtils::pass);
    }

    public FakePlayerCreateContext(ServerPlayerEntity player, @NotNull Consumer<EntityPlayerMPFake> consumer) {
        this(FetcherUtils.getFootPos(player),
                player.getYaw(),
                player.getPitch(),
                FetcherUtils.getWorld(player).getRegistryKey(),
                player.interactionManager.getGameMode(),
                player.getAbilities().flying,
                consumer
        );
    }
}
