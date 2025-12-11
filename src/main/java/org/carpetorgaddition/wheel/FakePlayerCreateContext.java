package org.carpetorgaddition.wheel;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.carpetorgaddition.util.FetcherUtils;
import org.carpetorgaddition.util.GenericUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public record FakePlayerCreateContext(
        Vec3 pos,
        double yaw,
        double pitch,
        ResourceKey<Level> dimension,
        GameType gamemode,
        boolean flying,
        Consumer<EntityPlayerMPFake> consumer
) {
    @SuppressWarnings("unused")
    public FakePlayerCreateContext(ServerPlayer player) {
        this(player, GenericUtils::pass);
    }

    public FakePlayerCreateContext(ServerPlayer player, @NotNull Consumer<EntityPlayerMPFake> consumer) {
        this(FetcherUtils.getFootPos(player),
                player.getYRot(),
                player.getXRot(),
                FetcherUtils.getWorld(player).dimension(),
                player.gameMode.getGameModeForPlayer(),
                player.getAbilities().flying,
                consumer
        );
    }
}
