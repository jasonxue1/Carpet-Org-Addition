package org.carpetorgaddition.periodic.task.search;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.wheel.ItemStackPredicate;

import java.io.File;

public record OfflinePlayerItemSearchContext(
        ServerCommandSource source,
        ItemStackPredicate predicate,
        UserCache userCache,
        ServerPlayerEntity player,
        File[] files
) {
}
