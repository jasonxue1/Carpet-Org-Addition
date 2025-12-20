package boat.carpetorgaddition.wheel.text;

import boat.carpetorgaddition.CarpetOrgAddition;

public class LocalizationKeys {
    public static final LocalizationKey ROOT = LocalizationKey.literal(CarpetOrgAddition.MOD_ID);
    public static final LocalizationKey COMMAND = LocalizationKey.of("command");
    public static final LocalizationKey OPERATION = LocalizationKey.of("operation");
    public static final LocalizationKey GENERIC = LocalizationKey.of("generic");

    public static class Operation {
        public static final LocalizationKey TEXT = OPERATION.then("text");
        public static final LocalizationKey SELF = OPERATION.then("self");
        public static final LocalizationKey ITEM = OPERATION.then("item");

        public static class Click {
            public static final LocalizationKey CLICK = TEXT.then("click");
        }
    }
}
