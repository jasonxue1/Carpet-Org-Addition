package boat.carpetorgaddition.wheel.text;

public class LocalizationKeys {
    public static final LocalizationKey COMMAND = LocalizationKey.of("command");
    public static final LocalizationKey OPERATION = LocalizationKey.of("operation");
    public static final LocalizationKey GENERIC = LocalizationKey.of("generic");
    public static final LocalizationKey RULE = LocalizationKey.of("rule");
    private static final LocalizationKey TIME = LocalizationKey.of("time");
    private static final LocalizationKey ARGUMENT = LocalizationKey.of("argument");
    private static final LocalizationKey ITEM = LocalizationKey.of("item");
    private static final LocalizationKey BUTTON = LocalizationKey.of("button");
    private static final LocalizationKey DIMENSION = LocalizationKey.of("dimension");
    private static final LocalizationKey LITERAL = LocalizationKey.of("literal");
    private static final LocalizationKey FILE = LocalizationKey.of("file");
    private static final LocalizationKey MISC = LocalizationKey.of("misc");

    public static class Operation {
        public static final LocalizationKey OFFLINE_PLAYER_NAME = OPERATION.then("offline_player_name");
    }

    public static class Rule {
        public static final LocalizationKey MESSAGE = RULE.then("message");
        public static final LocalizationKey VALIDATE = RULE.then("validate");
    }

    public static class Argument {
        private static final LocalizationKey PLAYER = ARGUMENT.then("player");
        private static final LocalizationKey OBJECT = ARGUMENT.then("object");

        public static class Player {
            public static final LocalizationKey TOOMANY = PLAYER.then("toomany");
        }

        public static class Object {
            public static final LocalizationKey INVALID_PATTERN = OBJECT.then("invalid_pattern");
            public static final LocalizationKey BROAD = OBJECT.then("broad");
            public static final LocalizationKey MISMATCH = OBJECT.then("mismatch");
        }
    }

    public static class Time {
        public static final LocalizationKey TICK = TIME.then("tick");
        public static final LocalizationKey SECOND = TIME.then("second");
        public static final LocalizationKey MINUTE = TIME.then("minute");
        public static final LocalizationKey HOUR = TIME.then("hour");
        public static final LocalizationKey MINUTE_SECOND = TIME.then("minute_second");
        public static final LocalizationKey HOUR_MINUTE = TIME.then("hour_minute");
        public static final LocalizationKey FORMAT = TIME.then("format");
    }

    public static class Item {
        public static final LocalizationKey ITEM = LocalizationKeys.ITEM.then("item");
        public static final LocalizationKey REMAINDER = LocalizationKeys.ITEM.then("remainder");
        public static final LocalizationKey GROUP = LocalizationKeys.ITEM.then("group");
        public static final LocalizationKey COUNT = LocalizationKeys.ITEM.then("count");
        public static final LocalizationKey ANY_ITEM = LocalizationKeys.ITEM.then("any_item");
    }

    public static class Literal {
        public static final LocalizationKey TRUE = LITERAL.then("true");
        public static final LocalizationKey FALSE = LITERAL.then("false");
    }

    public static class Button {
        public static final LocalizationKey HERE = BUTTON.then("here");
        public static final LocalizationKey INPUT = BUTTON.then("input");
        public static final LocalizationKey RUN_COMMAND = BUTTON.then("run_command");
        public static final LocalizationKey LOGIN = BUTTON.then("login");
        public static final LocalizationKey LOGOUT = BUTTON.then("logout");
        public static final LocalizationKey HIGHLIGHT = BUTTON.then("highlight");
    }

    public static class Dimension {
        public static final LocalizationKey OVERWORLD = DIMENSION.then("overworld");
        public static final LocalizationKey THE_NETHER = DIMENSION.then("the_nether");
        public static final LocalizationKey THE_END = DIMENSION.then("the_end");
    }

    public static class File {
        public static final LocalizationKey INVALID_NAME = FILE.then("invalid_name");
    }

    public static class Misc {
        public static final LocalizationKey OPERATOR = MISC.then("operator");
        public static final LocalizationKey INVENTORY = MISC.then("inventory");
        public static final LocalizationKey ENDER_CHEST = MISC.then("ender_chest");
        public static final LocalizationKey SELF = MISC.then("self");
    }
}
