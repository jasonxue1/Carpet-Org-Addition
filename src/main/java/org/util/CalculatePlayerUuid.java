package org.util;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.util.Uuids;
import org.apache.commons.lang3.mutable.MutableInt;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 读取一个包含一系列玩家名称的文件，获取每一个玩家的UUID
 */
public class CalculatePlayerUuid {
    private static final HashSet<String> players = new HashSet<>();
    private static int sum;
    private static final PrintWriter print;
    // 防止控制台乱码
    private static final PrintWriter console = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    private static long time = System.currentTimeMillis();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        try {
            print = new PrintWriter(new FileOutputStream("build/tmp/result.txt"), true, StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        init();
        start();
    }

    private static void start() {
        MutableInt ordinal = new MutableInt(0);
        while (true) {
            rest();
            if (players.isEmpty()) {
                return;
            }
            Iterator<String> iterator = players.iterator();
            ArrayList<String> names = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                if (iterator.hasNext()) {
                    names.add(iterator.next());
                    iterator.remove();
                    continue;
                }
                break;
            }
            List<GameProfile> list = tryGetOnlineUuid(names.toArray(String[]::new));
            for (String name : names) {
                if (writeOnlineUuid(name, list, ordinal)) {
                    continue;
                }
                ordinal.increment();
                write(ordinal.getValue(), false, new GameProfile(generateOfflineUuid(name), name));
            }
        }
    }

    /**
     * 将正版玩家UUID和名称写入文件
     */
    private static boolean writeOnlineUuid(String name, List<GameProfile> list, MutableInt ordinal) {
        for (GameProfile gameProfile : list) {
            if (name.equalsIgnoreCase(gameProfile.getName())) {
                ordinal.increment();
                write(ordinal.getValue(), true, gameProfile);
                return true;
            }
        }
        return false;
    }

    private static void write(int ordinal, boolean isOnline, GameProfile gameProfile) {
        String formatted = "正在查询第%s个玩家，总数:%s，剩余:%s，进度:%.2f%%; %s\t %s".formatted(
                ordinal, sum,
                sum - ordinal,
                ((float) ordinal / sum),
                (isOnline ? "正版" : "离线"),
                gameProfile.getName()
        );
        console.println(formatted);
        print.println(gameProfile.getId() + "=" + gameProfile.getName());
    }

    /**
     * 保证每秒只发送一条请求
     */
    private static void rest() {
        while (true) {
            long l = System.currentTimeMillis();
            if (l - time < 1000) {
                Thread.yield();
            } else {
                time = l;
                return;
            }
        }
    }

    public static void init() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("build/tmp/players.txt"));
        String line;
        while ((line = reader.readLine()) != null) {
            players.add(line);
        }
        sum = players.size();
    }

    /**
     * 尝试获取玩家的在线UUID
     */
    public static List<GameProfile> tryGetOnlineUuid(String[] names) {
        if (names.length == 0 || names.length > 10) {
            throw new IllegalArgumentException();
        }
        JsonArray jsonArray;
        try {
            URI uri = new URI("https://api.mojang.com/profiles/minecraft");
            URL url = uri.toURL();
            // 发送POST请求
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            sendRequestBody(connection, names);
            jsonArray = parseJson(connection);
        } catch (Exception e) {
            return List.of();
        }
        ArrayList<GameProfile> list = new ArrayList<>();
        for (JsonObject json : jsonArray.asList().stream().map(JsonElement::getAsJsonObject).toList()) {
            if (json.has("id")) {
                String id = json.get("id").getAsString();
                String name = json.get("name").getAsString();
                UUID uuid = UUID.fromString(parseId(id));
                list.add(new GameProfile(uuid, name));
            }
        }
        return list;
    }

    /**
     * 发送请求体
     */
    private static void sendRequestBody(HttpURLConnection connection, String[] names) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
        String json = GSON.toJson(names);
        try (writer) {
            writer.write(json);
        }
    }

    /**
     * 解析UUID字符串
     */
    private static String parseId(String id) {
        String str1 = id.substring(0, 8);
        String str2 = id.substring(8, 12);
        String str3 = id.substring(12, 16);
        String str4 = id.substring(16, 20);
        String str5 = id.substring(20);
        return "%s-%s-%s-%s-%s".formatted(str1, str2, str3, str4, str5);
    }

    /**
     * 解析json字符串
     */
    private static JsonArray parseJson(HttpURLConnection connection) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder sb = new StringBuilder();
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return GSON.fromJson(sb.toString(), JsonArray.class);
    }

    /**
     * 获取玩家的离线UUID
     */
    private static UUID generateOfflineUuid(String name) {
        return Uuids.getOfflinePlayerUuid(name);
    }
}
