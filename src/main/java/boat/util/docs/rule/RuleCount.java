import boat.carpetorgaddition.CarpetOrgAdditionSettings;

void main() {
    long count = CarpetOrgAdditionSettings.listRules()
            .stream()
            .filter(context -> !context.isRemove())
            .filter(context -> !context.isHidden())
            .count();
    PrintWriter console = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
    console.println("当前一共有" + count + "条规则。");
    console.close();
}
