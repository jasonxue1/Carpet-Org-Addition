package org.carpetorgaddition.translate;

import org.carpetorgaddition.CarpetOrgAdditionSettings;
import org.carpetorgaddition.rule.RuleContext;
import org.carpetorgaddition.translate.TranslateParser.Entry;
import org.carpetorgaddition.wheel.Counter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateTest {
    private static final String ZH_CN = "zh_cn";
    private static final String EN_US = "en_us";
    private static final String PLACEHOLDER = "%(?:(\\d+)\\$)?([A-Za-z%]|$)";
    private final HashMap<String, TranslateParser> parsers = new HashMap<>();

    public TranslateTest() throws FileNotFoundException {
        for (String lang : List.of(ZH_CN, EN_US)) {
            parsers.put(lang, new TranslateParser(lang));
        }
    }

    /**
     * 比较中英文翻译的键的内容和顺序是否完全相同
     */
    @Test
    public void testKey() {
        List<String> zhKey = this.parsers.get(ZH_CN).listOtherTranslate().stream().map(Entry::key).toList();
        List<String> enKey = this.parsers.get(EN_US).listOtherTranslate().stream().map(Entry::key).toList();
        // 中英文翻译的差异
        boolean identical = true;
        StringJoiner error = new StringJoiner("\n", "中英文翻译键不匹配：\n", "");
        // 包含在中文翻译中，但在英文翻译中没有
        for (String key : zhKey) {
            if (enKey.contains(key)) {
                continue;
            }
            error.add("en_us缺失：" + key);
            identical = false;
        }
        // 包含在英文翻译中，但在中文翻译中没有
        for (String key : enKey) {
            if (zhKey.contains(key)) {
                continue;
            }
            error.add("zh_cn缺失：" + key);
            identical = false;
        }
        // 比较中英文规则翻译键
        List<Entry> zhRuleKey = this.parsers.get(ZH_CN).listRuleTranslate().stream().filter(Entry::isRuleNonName).toList();
        List<Entry> enRuleKey = this.parsers.get(EN_US).listRuleTranslate().stream().filter(Entry::isRuleNonName).toList();
        for (Entry entry : zhRuleKey) {
            if (enRuleKey.contains(entry)) {
                continue;
            }
            error.add("en_us缺失：" + entry.key());
            identical = false;
        }
        for (Entry entry : enRuleKey) {
            if (zhRuleKey.contains(entry)) {
                continue;
            }
            error.add("en_us缺失：" + entry.key());
            identical = false;
        }
        Assertions.assertTrue(identical, error.toString());
    }

    /**
     * 检查中英文翻译值占位符的个数是否一致
     */
    @Test
    public void testValue() {
        List<String> zhValue = this.parsers.get(ZH_CN).listOtherTranslate().stream().map(Entry::value).toList();
        List<String> enValue = this.parsers.get(EN_US).listOtherTranslate().stream().map(Entry::value).toList();
        int size = zhValue.size();
        if (size != enValue.size()) {
            Assertions.fail("中英文翻译键不相同");
        }
        boolean perfectMatch = true;
        StringJoiner error = new StringJoiner("\n", "中英文翻译值不匹配：\n", "");
        for (int index = 0; index < size; index++) {
            String zhTranslate = zhValue.get(index);
            String enTranslate = enValue.get(index);
            if (substringCount(zhTranslate) == substringCount(enValue.get(index))) {
                continue;
            }
            error.add(zhTranslate + " --- " + enTranslate);
            perfectMatch = false;
        }
        Assertions.assertTrue(perfectMatch, error.toString());
    }

    /**
     * @return 翻译中的占位符个数
     */
    private int substringCount(String value) {
        Matcher matcher = Pattern.compile(TranslateTest.PLACEHOLDER).matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 检查是否包含未使用的翻译键
     */
    @Test
    public void testUsage() throws IOException {
        File rootPath = new File("src/main/java/org/carpetorgaddition");
        Counter<String> counter = new Counter<>();
        List<String> keys = this.parsers.get(ZH_CN)
                .listOtherTranslate()
                .stream()
                .filter(entry -> !entry.isCategory())
                .map(Entry::key)
                .toList();
        keys.forEach(key -> counter.set(key, 0));
        checkUsage(rootPath, keys, counter);
        // 所有未被使用的翻译键
        List<String> list = counter.stream().map(Map.Entry::getKey).filter(s -> counter.getCount(s) == 0).toList();
        StringJoiner errorReport = new StringJoiner("\n", "包含未使用的翻译键：\n", "");
        for (String key : list) {
            errorReport.add(key);
        }
        Assertions.assertTrue(list.isEmpty(), errorReport.toString());
    }

    // 递归遍历所有源代码文件
    private void checkUsage(File root, List<String> notRule, Counter<String> counter) throws IOException {
        // 检查当前文件是否是文件夹
        File[] files = root.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isFile()) {
                // 检查当前文件是否是Java文件
                if (file.getName().endsWith(".java")) {
                    readFile(file, notRule, counter);
                }
            } else if (file.isDirectory()) {
                // 递归遍历所有Java文件
                checkUsage(file, notRule, counter);
            }
        }
    }

    private void readFile(File java, List<String> notRule, Counter<String> counter) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(java));
        try (reader) {
            String line;
            while ((line = reader.readLine()) != null) {
                for (String key : notRule) {
                    int index = 0;
                    while ((line.indexOf("\"" + key + "\"", index)) != -1) {
                        index++;
                        counter.add(key);
                    }
                }
            }
        }
    }

    /**
     * 检查是否有未使用的规则翻译键
     */
    @Test
    public void testRuleKeyUsage() {
        Set<String> set = this.parsers.get(ZH_CN).listAllRule();
        List<String> list = CarpetOrgAdditionSettings.listRules().stream().map(RuleContext::getName).toList();
        if (set.size() == list.size()) {
            return;
        }
        StringJoiner sj = new StringJoiner("\n", "包含未定义的规则翻译：\n", "");
        boolean unused = false;
        for (String str : set) {
            if (list.contains(str)) {
                continue;
            }
            unused = true;
            sj.add(str);
        }
        Assertions.assertFalse(unused, sj.toString());
    }

    /**
     * 检查英文翻译中是否包含中文字符
     */
    @Test
    public void testChineseCharacter() {
        List<Entry> list = this.parsers.get(EN_US).listAll();
        StringJoiner sj = new StringJoiner("\n", "英文翻译中不应包含中文字符：\n", "");
        boolean hasChineseCharacter = false;
        for (Entry entry : list) {
            String value = entry.value();
            if (value.contains("更新抑制器")) {
                continue;
            }
            Matcher matcher = Pattern.compile("[\\u4e00-\\u9fa5]|[，：（）]").matcher(value);
            if (matcher.find()) {
                hasChineseCharacter = true;
                sj.add(entry.toString());
            }
        }
        Assertions.assertFalse(hasChineseCharacter, sj.toString());
    }

    /**
     * 检查中文翻译中是否包含英文字符
     */
    @Test
    public void testEnglishCharacter() {
        List<Entry> list = this.parsers.get(ZH_CN).listAll();
        StringJoiner sj = new StringJoiner("\n", "中文翻译中不应包含英文字符：\n", "");
        boolean hasChineseCharacter = false;
        for (Entry entry : list) {
            String value = entry.value();
            if (value.contains("Mismatch in destroy block pos: {} {}") || value.contains("Carpet Mod中符合“%s”的选项：:") || value.matches("%s \\([+\\-]%s\\) \\[%s->%s]")) {
                continue;
            }
            Matcher matcher = Pattern.compile("[,:()]").matcher(value);
            // 中文冒号后不加空格
            if (matcher.find() || value.contains("： ")) {
                hasChineseCharacter = true;
                sj.add(entry.toString());
            }
        }
        Assertions.assertFalse(hasChineseCharacter, sj.toString());
    }
}
