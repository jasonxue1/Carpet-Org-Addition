package org.carpetorgaddition.wheel;

import net.minecraft.util.Uuids;
import org.carpetorgaddition.wheel.GameProfileCache.Table;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public class GameProfileCacheTableTest {
    private Table table;
    private static Set<String> usernames;

    @BeforeAll
    public static void initUsernames() {
        usernames = new HashSet<>();
        usernames.add("abc");
        usernames.add("ABC");
        usernames.add("Abc");
        usernames.add("aBc");
        usernames.add("abC");
        usernames.add("AAaa");
        usernames.add("aaAA");
        usernames.add("AaaA");
        usernames.add("aAAa");
        usernames.add("aaaA");
        usernames.add("Steve");
        usernames.add("Alex");
        usernames.add("HelloWorld");
        usernames.add("bot_1");
        usernames.add("Bot_1");
        usernames.add("BOT_1");
    }

    @BeforeEach
    public void init() {
        this.table = new Table();
        for (String username : usernames) {
            this.table.put(Uuids.getOfflinePlayerUuid(username), username);
        }
    }

    @Test
    public void testGet() {
        for (String username : usernames) {
            UUID expected = Uuids.getOfflinePlayerUuid(username);
            UUID actual = table.get(username).map(Map.Entry::getKey).orElseThrow();
            System.out.println("username: " + username);
            System.out.println("expected: " + expected);
            System.out.println("actual: " + actual);
            System.out.println();
            Assertions.assertEquals(expected, actual);
        }
        Assertions.assertThrows(IllegalArgumentException.class, () -> table.get((String) null));
    }

    @Test
    public void testGetCaseVariant() {
        Map<String, String> map = Map.ofEntries(
                Map.entry("aaaa", "aaaA"),
                Map.entry("AAAA", "AAaa"),
                Map.entry("Aaaa", "AaaA"),
                Map.entry("aAaA", "aAAa"),
                Map.entry("AaAa", "AaaA"),
                Map.entry("aAAa", "aAAa"),
                Map.entry("aaAA", "aaAA"),
                Map.entry("bot_1", "bot_1"),
                Map.entry("Bot_1", "Bot_1"),
                Map.entry("BOT_1", "BOT_1"),
                Map.entry("BOt_1", "BOT_1"),
                Map.entry("bOT_1", "bot_1")
        );
        for (Map.Entry<String, String> entry : map.entrySet()) {
            UUID expected = Uuids.getOfflinePlayerUuid(entry.getValue());
            UUID actual = table.get(entry.getKey()).map(Map.Entry::getKey).orElseThrow();
            System.out.println("username: " + entry.getKey() + " / " + entry.getValue());
            System.out.println("cache_username: " + table.get(actual).orElseThrow());
            System.out.println("expected: " + expected);
            System.out.println("actual: " + actual);
            System.out.println();
            Assertions.assertEquals(expected, actual);
        }
    }
}
