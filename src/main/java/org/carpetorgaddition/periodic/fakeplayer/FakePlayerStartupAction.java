package org.carpetorgaddition.periodic.fakeplayer;

import carpet.patches.EntityPlayerMPFake;
import net.minecraft.util.Hand;
import org.carpetorgaddition.util.FetcherUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public enum FakePlayerStartupAction implements Consumer<EntityPlayerMPFake> {
    USE,
    ATTACK,
    KILL;

    private static final Map<String, FakePlayerStartupAction> MAP = Arrays.stream(FakePlayerStartupAction.values())
            .map(action -> Map.entry(action.toString(), action))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public static Optional<FakePlayerStartupAction> fromString(String name) {
        FakePlayerStartupAction action = MAP.get(name.toLowerCase(Locale.ROOT));
        return Optional.ofNullable(action);
    }

    @Override
    public void accept(EntityPlayerMPFake fakePlayer) {
        this.function().accept(fakePlayer);
    }

    private Consumer<EntityPlayerMPFake> function() {
        return switch (this) {
            case USE -> fakePlayer -> FakePlayerUtils.click(fakePlayer, Hand.OFF_HAND);
            case ATTACK -> fakePlayer -> FakePlayerUtils.click(fakePlayer, Hand.MAIN_HAND);
            case KILL -> fakePlayer -> fakePlayer.kill(FetcherUtils.getWorld(fakePlayer));
        };
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
