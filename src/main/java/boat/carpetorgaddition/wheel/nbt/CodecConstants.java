package boat.carpetorgaddition.wheel.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.IntStream;

public class CodecConstants {
    public static final Codec<LocalDateTime> TIME_CODEC = Codec.INT_STREAM.comapFlatMap(it -> {
        int[] times = it.toArray();
        if (times.length == 6) {
            return DataResult.success(LocalDateTime.of(times[0], times[1], times[2], times[3], times[4], times[5]));
        } else {
            return DataResult.error(() -> "Unable to resolve to LocalDateTime: " + Arrays.toString(times));
        }
    }, time -> IntStream.of(
            time.getYear(),
            time.getMonthValue(),
            time.getDayOfMonth(),
            time.getHour(),
            time.getMinute(),
            time.getSecond()
    )).stable();
    public static final LocalDateTime TIME_DEFAULT_VALUE = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
}
