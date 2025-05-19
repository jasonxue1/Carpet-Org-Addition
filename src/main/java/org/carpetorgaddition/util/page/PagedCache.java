package org.carpetorgaddition.util.page;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

public class PagedCache extends SoftReference<PagedCollection> {
    private final int id;

    public PagedCache(PagedCollection referent, ReferenceQueue<PagedCollection> queue, int id) {
        super(referent, queue);
        this.id = id;
    }

    @Nullable
    public PagedCollection getPagedCollection() {
        return this.get();
    }

    public int getId() {
        return this.id;
    }
}
