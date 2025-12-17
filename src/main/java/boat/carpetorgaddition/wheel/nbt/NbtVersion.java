package boat.carpetorgaddition.wheel.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public record NbtVersion(int major, int minor) implements Comparable<NbtVersion> {
    public static final Codec<NbtVersion> CODEC = Codec.STRING.comapFlatMap(NbtVersion::parse, NbtVersion::toString).stable();

    private static DataResult<NbtVersion> parse(String version) {
        try {
            String[] split = version.split("\\.");
            if (split.length != 2) {
                throw new IllegalArgumentException("String cannot be parsed as version number: " + version);
            }
            return DataResult.success(new NbtVersion(Integer.parseInt(split[0]), Integer.parseInt(split[1])));
        } catch (RuntimeException e) {
            return DataResult.error(() -> "Not a valid nbt version:" + version + " " + e.getMessage());
        }
    }

    @Override
    public int compareTo(NbtVersion version) {
        int compare = Integer.compare(this.major, version.major);
        return compare == 0 ? Integer.compare(this.minor, version.minor) : compare;
    }

    @Override
    public String toString() {
        return "%d.%d".formatted(this.major, this.minor);
    }
}
