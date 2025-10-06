package org.carpetorgaddition.util;

import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

public class FakePlayerDamageTracker {
    private static final WeakHashMap<PlayerEntity, List<DamageSource>> DAMAGE_SOURCES_MAP = new WeakHashMap<>();

    public static void recordDamage(PlayerEntity player, DamageSource source) {
        if (player != null) {
            List<DamageSource> recentDamageSources = DAMAGE_SOURCES_MAP.computeIfAbsent(player, k -> new ArrayList<>());

            if (recentDamageSources.size() > 5) {
                recentDamageSources.remove(0);
            }
            recentDamageSources.add(source);
        }
    }

    public static List<DamageSource> getRecentDamageSources(PlayerEntity player) {
        return DAMAGE_SOURCES_MAP.getOrDefault(player, new ArrayList<>());
    }

    public static void clearDamageSources(PlayerEntity player) {
        DAMAGE_SOURCES_MAP.remove(player);
    }
}