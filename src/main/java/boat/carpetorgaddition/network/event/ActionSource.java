package boat.carpetorgaddition.network.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.Locale;
import java.util.Optional;

/**
 * 自定义单击动作的来源
 */
public enum ActionSource {
    /**
     * 聊天栏
     */
    CHAT,
    /**
     * 对话框
     */
    DIALOG,
    /**
     * 告示牌
     */
    SIGN,
    /**
     * 未知
     */
    UNKNOWN;

    // TODO 测试告示牌
    public static final Codec<ActionSource> CODEC = Codec.STRING
            .comapFlatMap(ActionSource::parse, ActionSource::toString)
            .stable();

    private static DataResult<ActionSource> parse(String type) {
        try {
            ActionSource value = ActionSource.valueOf(type.toUpperCase(Locale.ROOT));
            return DataResult.success(value);
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> Optional.ofNullable(e.getMessage()).orElse(e.getClass().getSimpleName()));
        }
    }
}
