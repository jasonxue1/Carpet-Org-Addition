package org.carpetorgaddition.wheel;

import org.carpetorgaddition.wheel.GameProfileCache.Table;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class GameProfileCacheTableTest {
    @Test
    public void testGet() {
        Table table = new Table();
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        UUID uuid5 = UUID.randomUUID();
        table.put(uuid1, "abc");
        table.put(uuid2, "ABC");
        table.put(uuid3, "Abc");
        table.put(uuid4, "aBc");
        table.put(uuid5, "abC");
        List<UUID> list = List.of(uuid1, uuid2, uuid3, uuid4, uuid5);
        Assertions.assertSame(table.get("abc").orElseThrow(), uuid1);
        Assertions.assertSame(table.get("ABC").orElseThrow(), uuid2);
        Assertions.assertSame(table.get("Abc").orElseThrow(), uuid3);
        Optional<UUID> optional = table.get("aBC");
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertTrue(list.contains(optional.get()));
        Assertions.assertTrue(table.get("aaa").isEmpty());
        Assertions.assertTrue(table.get("bbb").isEmpty());
        Assertions.assertTrue(table.get(UUID.randomUUID()).isEmpty());
        Assertions.assertTrue(table.get(UUID.randomUUID()).isEmpty());
        Assertions.assertTrue(table.get(UUID.randomUUID()).isEmpty());
        Assertions.assertEquals("abc", table.get(uuid1).orElseThrow());
        Assertions.assertEquals("ABC", table.get(uuid2).orElseThrow());
        Assertions.assertEquals("Abc", table.get(uuid3).orElseThrow());
        Assertions.assertEquals("aBc", table.get(uuid4).orElseThrow());
        Assertions.assertEquals("abC", table.get(uuid5).orElseThrow());
        Assertions.assertThrows(IllegalArgumentException.class, () -> table.get((String) null));
    }

    @Test
    public void testRemove() {
        Table table = new Table();
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();
        UUID uuid5 = UUID.randomUUID();
        table.put(uuid1, "abc");
        table.put(uuid2, "ABC");
        table.put(uuid3, "Abc");
        table.put(uuid4, "aBc");
        table.put(uuid5, "abC");
        table.put(UUID.randomUUID(), "abcd");
        table.remove("abc");
        table.remove("abc");
        table.remove("aaa");
        table.remove("bbb");
        table.remove("abcd");
        Assertions.assertNotSame(table.get("abc").orElseThrow(), uuid1);
        table.remove(uuid1);
        table.remove(uuid2);
        table.remove(uuid3);
        table.remove(uuid4);
        table.remove(uuid5);
        table.remove(UUID.randomUUID());
        table.remove(UUID.randomUUID());
        table.remove(UUID.randomUUID());
    }
}
