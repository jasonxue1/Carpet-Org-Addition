package boat.carpetorgaddition.wheel.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * @param major NBT的主版本号
 * @param minor NBT的次版本号，保留字段，没有实际作用
 */
public record NbtVersion(int major, int minor) implements Comparable<NbtVersion> {
    public static final NbtVersion ZERO = new NbtVersion(0);
    public static final NbtVersion VERSION_3 = new NbtVersion(3);
    public static final Codec<NbtVersion> CODEC = Codec.STRING.comapFlatMap(NbtVersion::parse, NbtVersion::toString).stable();

    public NbtVersion(int major) {
        this(major, 0);
    }

    private static DataResult<NbtVersion> parse(String version) {
        String[] versions = version.split("\\.");
        try {
            return switch (versions.length) {
                case 1 -> DataResult.success(new NbtVersion(Integer.parseInt(versions[0])));
                case 2 ->
                        DataResult.success(new NbtVersion(Integer.parseInt(versions[0]), Integer.parseInt(versions[1])));
                default -> DataResult.error(() -> "String cannot be parsed as version number: " + version);
            };
        } catch (NumberFormatException e) {
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
