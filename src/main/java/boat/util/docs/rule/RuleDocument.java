package boat.util.docs.rule;

import boat.carpetorgaddition.CarpetOrgAdditionSettings;
import boat.carpetorgaddition.rule.RuleContext;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RuleDocument {
    private final Set<RuleContext<?>> rules;
    private final JsonObject json;

    public static void main(String[] args) throws IOException {
        // 生成文档前备份旧的文件
        String time = DateTimeFormatter.ofPattern("yyMMddHHmmss").format(LocalDateTime.now());
        FileInputStream fileInputStream = new FileInputStream("docs/rules.md");
        Files.copy(fileInputStream, Path.of("docs/backups/rules/" + time + ".md"));
        RuleDocument ruleDocument = new RuleDocument();
        BufferedWriter writer = new BufferedWriter(new FileWriter("docs/rules.md"));
        writer.write("## 所有规则");
        writer.newLine();
        writer.newLine();
        writer.write("**提示：可以使用`Ctrl+F`快速查找自己想要的规则**");
        writer.newLine();
        writer.newLine();
        ruleDocument.write(writer);
        writer.close();
    }

    private void write(BufferedWriter writer) throws IOException {
        List<RuleInformation> list = this.rules.stream()
                .filter(context -> !context.isHidden())
                .filter(context -> !context.isRemove())
                .map(this::pause)
                .toList();
        for (RuleInformation ruleInfo : list) {
            writer.write(ruleInfo.toString());
            writer.newLine();
        }
    }

    /*package-private*/ RuleDocument() throws FileNotFoundException {
        BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/assets/carpet-org-addition/lang/zh_cn.json"));
        Gson gson = new Gson();
        this.json = gson.fromJson(reader, JsonObject.class);
        this.rules = CarpetOrgAdditionSettings.listRules();
    }

    // 读取字节码信息
    @Nullable
    RuleInformation pause(RuleContext<?> context) {
        String rule = context.getName();
        return new RuleInformation(context, readRuleName(rule), readRuleDesc(rule), readRuleExtra(rule));
    }

    // 读取规则名称
    private String readRuleName(String rule) {
        return json.get("carpet.rule." + rule + ".name").getAsString();
    }

    // 读取规则描述
    private String readRuleDesc(String rule) {
        return json.get("carpet.rule." + rule + ".desc").getAsString();
    }

    // 读取规则扩展描述
    private String[] readRuleExtra(String rule) {
        int number = 0;
        ArrayList<String> list = new ArrayList<>();
        while (true) {
            String extra = "carpet.rule." + rule + ".extra." + number;
            if (this.json.has(extra)) {
                list.add(this.json.get(extra).getAsString());
                number++;
            } else {
                break;
            }
        }
        return list.toArray(new String[0]);
    }
}
