package org.carpetorgaddition.wheel.page;

import net.minecraft.server.command.ServerCommandSource;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Optional;

public class PageManager {
    private final ReferenceQueue<PagedCollection> referenceQueue = new ReferenceQueue<>();
    private final HashMap<Integer, PagedCache> pagingList = new HashMap<>();
    private int currentId = 0;

    public PageManager() {
    }

    /**
     * 清除已经被回收的软引用对象
     */
    public void tick() {
        PagedCache cache;
        while ((cache = (PagedCache) this.referenceQueue.poll()) != null) {
            this.pagingList.remove(cache.getId());
        }
        this.pagingList.entrySet().removeIf(entry -> entry.getValue().canFreeMemory());
    }

    public PagedCollection newPagedCollection(ServerCommandSource source) {
        PagedCollection collection = new PagedCollection(this.currentId, source);
        this.pagingList.put(this.currentId, new PagedCache(collection, referenceQueue, this.currentId));
        this.currentId++;
        return collection;
    }

    public Optional<PagedCollection> get(int id) {
        PagedCache cache = this.pagingList.get(id);
        if (cache == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get());
    }
}
