package org.carpetorgaddition.wheel.page;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class PagedCache extends SoftReference<PagedCollection> {
    private final int id;
    private final ServerCommandSource source;

    public PagedCache(PagedCollection referent, ReferenceQueue<PagedCollection> queue, int id) {
        super(referent, queue);
        this.source = referent.getSource();
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    /**
     * 对象是否可以被立即回收
     */
    public boolean canFreeMemory() {
        ServerPlayerEntity player = this.source.getPlayer();
        if (player == null) {
            // 被服务器控制台，命令方块等执行，实际上可能不会发生这种情况
            return false;
        }
        return player.isRemoved();
    }
}
