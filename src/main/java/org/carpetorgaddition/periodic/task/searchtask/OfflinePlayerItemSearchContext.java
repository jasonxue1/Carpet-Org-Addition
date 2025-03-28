package org.carpetorgaddition.periodic.task.searchtask;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.UserCache;
import org.carpetorgaddition.util.wheel.ItemStackPredicate;

import java.io.File;

public record OfflinePlayerItemSearchContext(
        ServerCommandSource source,
        ItemStackPredicate predicate,
        UserCache userCache,
        ServerPlayerEntity player,
        File[] files,
        boolean displayUnknown
) {
}
