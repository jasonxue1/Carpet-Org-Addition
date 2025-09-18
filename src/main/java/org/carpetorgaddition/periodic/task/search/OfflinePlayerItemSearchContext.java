package org.carpetorgaddition.periodic.task.search;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.NameToIdCache;
import org.carpetorgaddition.wheel.ItemStackPredicate;

import java.io.File;

public record OfflinePlayerItemSearchContext(
        ServerCommandSource source,
        ItemStackPredicate predicate,
        NameToIdCache cache,
        ServerPlayerEntity player,
        File[] files
) {
}
