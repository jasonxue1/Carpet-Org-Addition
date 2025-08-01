package org.carpetorgaddition.util;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import org.carpetorgaddition.CarpetOrgAddition;
import org.jetbrains.annotations.Contract;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class IOUtils {
    public static final String JSON_EXTENSION = ".json";
    public static final String NBT_EXTENSION = ".nbt";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /**
     * 模组配置文件路径
     */
    public static final Path CONFIGURE_DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve(CarpetOrgAddition.MOD_NAME_LOWER_CASE);
    /**
     * 不能包含在文件名中的字符
     */
    public static final String INVALID_FILENAME_CHARS = "\\/:*?\"<>|";
    /**
     * Windows保留文件名
     */
    public static final String[] WINDOWS_RESERVED_NAME = {
            "CON", "PRN", "AUX", "NUL",
            "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
            "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    };
    /**
     * {@code usercache.json}文件
     */
    public static final File USERCACHE_JSON = FabricLoader.getInstance().getGameDir().resolve("usercache.json").toFile();

    private IOUtils() {
    }

    /**
     * 创建一个UTF-8编码的字符输入流对象
     */
    public static BufferedReader toReader(File file) throws IOException {
        return new BufferedReader(new FileReader(file, StandardCharsets.UTF_8));
    }

    /**
     * 创建一个UTF-8编码的字符输出流对象
     */
    public static BufferedWriter toWriter(File file) throws IOException {
        // 如果文件不存在则创建
        createFileIfNotExists(file);
        return new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8));
    }

    /**
     * 将一个json对象保存到文件
     */
    public static void saveJson(File file, JsonObject json) throws IOException {
        String jsonString = GSON.toJson(json, JsonObject.class);
        try (BufferedWriter writer = toWriter(file)) {
            writer.write(jsonString);
        }
    }

    /**
     * 从文件加载一个json对象
     */
    public static JsonObject loadJson(File file) throws IOException {
        return loadJson(file, JsonObject.class);
    }

    public static <T> T loadJson(File file, Class<T> type) throws IOException {
        BufferedReader reader = toReader(file);
        try (reader) {
            return GSON.fromJson(reader, type);
        }
    }

    /**
     * 如果一个文件不存在，则创建，如果这个文件的父级也不存在则同时创建
     *
     * @return 是否创建成功
     */
    public static boolean createFileIfNotExists(File file) {
        if (file.isFile()) {
            return true;
        }
        File parent = file.getParentFile();
        // 如果父级路径不存在则创建
        if (parent.isDirectory() || parent.mkdirs()) {
            try {
                return file.createNewFile();
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    /**
     * 复制一个文件
     *
     * @param original 源文件，该文件必须存在且不能是文件夹
     * @param copy     复制的目标位置
     * @return 是否正常完成备份
     */
    public static void copyFile(File original, File copy) {
        try {
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(original));
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(copy));
            try (input; output) {
                byte[] bytes = new byte[1024 * 8]; // 1024 * 8 = 8192;
                int len;
                while ((len = input.read(bytes)) != -1) {
                    output.write(bytes, 0, len);
                }
            }
        } catch (IOException e) {
            IOUtils.loggerError(e);
        }
    }

    /**
     * 备份一个文件
     *
     * @param file 要备份的文件
     */
    public static void backupFile(File file) {
        backupFile(file, true);
    }

    /**
     * @param force 如果已经存在相同名字的备份了，是否覆盖
     */
    public static File backupFile(File file, boolean force) {
        File backup = new File(file.getParent(), "backup_" + System.currentTimeMillis() + "_" + file.getName());
        if (!force && backup.exists()) {
            throw new IllegalStateException("The backup file already exists");
        }
        copyFile(file, backup);
        return backup;
    }

    /**
     * json对象中是否包含指定元素
     *
     * @param elements 一个字符串数组，数组中只要有一个元素不存在于json中方法就返回false
     */
    public static boolean jsonHasElement(JsonObject json, String... elements) {
        for (String element : elements) {
            if (json.has(element)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /**
     * 检查文件名称是否有效
     */
    public static boolean isValidFileName(String fileName) {
        // 不允许以“.”开头，因为这样的文件名可能被视为隐藏文件
        if (fileName.startsWith(".")) {
            return true;
        }
        // 不能是Windows保留文件名
        int index = fileName.indexOf(".");
        String substring = (index == -1 ? fileName : fileName.substring(0, index)).toUpperCase(Locale.ROOT);
        for (String str : WINDOWS_RESERVED_NAME) {
            if (Objects.equals(str, substring)) {
                return true;
            }
        }
        // 文件名包含无效字符
        for (int i = 0; i < IOUtils.INVALID_FILENAME_CHARS.length(); i++) {
            char c = IOUtils.INVALID_FILENAME_CHARS.charAt(i);
            if (fileName.contains(String.valueOf(c))) {
                return true;
            }
        }
        return false;
    }

    public static File configFile(String fileName, boolean create) {
        File file = CONFIGURE_DIRECTORY.resolve(fileName).toFile();
        if (create) {
            createFileIfNotExists(file);
        }
        return file;
    }

    /**
     * 根据键获取json中对应的值，如果不存在，返回默认值
     *
     * @param defaultValue 如果为获取到值，返回默认值
     * @param type         返回值的类型
     */
    @Contract(value = "_,_,!null,_ -> !null")
    public static <T> T getJsonElement(JsonObject json, String key, T defaultValue, Class<T> type) {
        JsonElement element = json.get(key);
        if (element == null) {
            return defaultValue;
        }
        // 布尔值
        if (type == boolean.class || type == Boolean.class) {
            return type.cast(element.getAsBoolean());
        }
        // 整数
        if (type == byte.class || type == Byte.class) {
            return type.cast(element.getAsByte());
        }
        if (type == short.class || type == Short.class) {
            return type.cast(element.getAsShort());
        }
        if (type == int.class || type == Integer.class) {
            return type.cast(element.getAsInt());
        }
        if (type == long.class || type == Long.class) {
            return type.cast(element.getAsLong());
        }
        // 浮点数
        if (type == float.class || type == Float.class) {
            return type.cast(element.getAsFloat());
        }
        if (type == double.class || type == Double.class) {
            return type.cast(element.getAsDouble());
        }
        // 字符串
        if (type == String.class) {
            return type.cast(element.getAsString());
        }
        // jsonObject
        if (JsonObject.class.isAssignableFrom(type)) {
            return type.cast(element.getAsJsonObject());
        }
        // JsonArray
        if (JsonArray.class.isAssignableFrom(type)) {
            return type.cast(element.getAsJsonArray());
        }
        throw new IllegalArgumentException();
    }

    public static <T> Optional<T> getJsonElement(JsonObject json, String key, Class<T> type) {
        return Optional.ofNullable(getJsonElement(json, key, null, type));
    }

    /**
     * 删除文件扩展名
     */
    @Deprecated(forRemoval = true)
    public static String removeJsonExtension(String fileName) {
        if (fileName.endsWith(JSON_EXTENSION)) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        }
        return fileName;
    }

    public static String removeExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        return index == -1 ? fileName : fileName.substring(0, index);
    }

    public static String removeExtension(String fileName, String extension) {
        String fineNameLowerCase = fileName.toLowerCase(Locale.ROOT);
        String extensionNameLowerCase = (extension.startsWith(".") ? extension : "." + extension).toLowerCase(Locale.ROOT);
        if (fineNameLowerCase.endsWith(extensionNameLowerCase)) {
            return removeExtension(fileName);
        }
        return fileName;
    }

    /**
     * 删除一个文件
     *
     * @apiNote 此方法是为了避免编译器发出未使用方法返回值警告
     */
    public static void removeFile(File file) {
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    /**
     * 重命名文件
     */
    public static void renameFile(File file, String name) {
        //noinspection ResultOfMethodCallIgnored
        file.renameTo(new File(file.getParent(), name));
    }

    /**
     * 将一个文件标记为弃用
     */
    public static void deprecatedFile(File file) {
        renameFile(file, "deprecated_" + System.currentTimeMillis() + "_" + file.getName());
    }

    /**
     * 记录一个IO错误，用来处理编译时异常
     */
    public static void loggerError(IOException e) {
        CarpetOrgAddition.LOGGER.error("IO error occurred:", e);
    }
}
