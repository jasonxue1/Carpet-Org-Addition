package boat.carpetorgaddition.periodic.fakeplayer;

import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.text.LocalizationKey;
import carpet.patches.EntityPlayerMPFake;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

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
            case USE -> fakePlayer -> FakePlayerUtils.click(fakePlayer, InteractionHand.OFF_HAND);
            case ATTACK -> fakePlayer -> FakePlayerUtils.click(fakePlayer, InteractionHand.MAIN_HAND);
            case KILL -> fakePlayer -> fakePlayer.kill(ServerUtils.getWorld(fakePlayer));
        };
    }

    public Component getDisplayName(LocalizationKey key) {
        return key.then(this.toString()).translate();
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
