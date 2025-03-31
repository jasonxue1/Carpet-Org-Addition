package org.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.util.Uuids;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

/**
 * 读取一个包含一系列玩家名称的文件，获取每一个玩家的UUID
 */
public class CalculatePlayerUuid {
    private static final HashSet<String> players = new HashSet<>();
    private static final PrintWriter print;
    private static long time = System.currentTimeMillis();
    private static final Gson GSON = new Gson();

    static {
        try {
            print = new PrintWriter(new FileOutputStream("build/tmp/result.txt"), true, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        init();
        int i = 0;
        for (String player : players) {
            while (true) {
                long l = System.currentTimeMillis();
                if (l - time < 1000) {
                    Thread.yield();
                } else {
                    time = l;
                    break;
                }
            }
            i++;
            // 防止控制台乱码
            PrintWriter console = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
            Optional<UUID> optional = tryGetOnlineUuid(player);
            console.println("第" + i + "个玩家(总" + players.size() + "个): " + (optional.isPresent() ? "正版\t" : "离线\t") + player);
            UUID uuid = optional.orElseGet(() -> generateOfflineUuid(player));
            print.println(uuid + "=" + player);
        }
    }

    public static void init() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("build/tmp/players.txt"));
        String line;
        while ((line = reader.readLine()) != null) {
            players.add(line);
        }
    }

    /**
     * 尝试获取玩家的在线UUID
     */
    public static Optional<UUID> tryGetOnlineUuid(String name) {
        try {
            URI uri = new URI("https://api.mojang.com/users/profiles/minecraft/%s".formatted(name));
            URL url = uri.toURL();
            URLConnection connection = url.openConnection();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JsonObject json = GSON.fromJson(sb.toString(), JsonObject.class);
            if (json.has("id")) {
                String id = json.get("id").getAsString();
                String str1 = id.substring(0, 8);
                String str2 = id.substring(8, 12);
                String str3 = id.substring(12, 16);
                String str4 = id.substring(16, 20);
                String str5 = id.substring(20);
                try {
                    UUID uuid = UUID.fromString("%s-%s-%s-%s-%s".formatted(str1, str2, str3, str4, str5));
                    return Optional.of(uuid);
                } catch (IllegalArgumentException e) {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 获取玩家的离线UUID
     */
    private static UUID generateOfflineUuid(String name) {
        return Uuids.getOfflinePlayerUuid(name);
    }
}
