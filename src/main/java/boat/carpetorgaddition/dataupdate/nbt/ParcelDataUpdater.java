package boat.carpetorgaddition.dataupdate.nbt;

import boat.carpetorgaddition.wheel.nbt.NbtVersion;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;

import java.util.Map;
import java.util.Optional;

public class ParcelDataUpdater extends NbtDataUpdater {
    public ParcelDataUpdater(MinecraftServer server) {
        super(server);
    }

    @Override
    protected CompoundTag updateDataFormat(CompoundTag old, NbtVersion version) {
        if (version.compareTo(NbtVersion.VERSION_3) < 0) {
            CompoundTag nbt = new CompoundTag();
            for (Map.Entry<String, Tag> entry : old.entrySet()) {
                String key = entry.getKey();
                Tag value = entry.getValue();
                switch (key) {
                    case "NbtDataVersion" -> nbt.put("vanilla_data_version", value);
                    case "cancel" -> nbt.put("recall", value);
                    case "item" -> {
                        ListTag tags = new ListTag();
                        tags.add(value);
                        nbt.put("items", tags);
                    }
                    default -> nbt.put(key, value);
                }
            }
            return nbt;
        }
        return old;
    }

    @Override
    protected CompoundTag updateVanillaDataFormat(CompoundTag old, int version) {
        if (version < CURRENT_VANILLA_DATA_VERSION) {
            CompoundTag nbt = new CompoundTag();
            for (Map.Entry<String, Tag> entry : old.entrySet()) {
                String key = entry.getKey();
                switch (key) {
                    case "items" -> {
                        ListTag newTags = new ListTag();
                        if (entry.getValue() instanceof ListTag oldTags) {
                            for (Tag tag : oldTags) {
                                newTags.add(this.updateItemStack(tag, version));
                            }
                        }
                        nbt.put(key, newTags);
                    }
                    case "vanilla_data_version" -> nbt.putInt(key, CURRENT_VANILLA_DATA_VERSION);
                    default -> nbt.put(key, entry.getValue());
                }
            }
            return nbt;
        }
        return old;
    }

    public static NbtVersion getVersion(CompoundTag nbt) {
        return nbt.read("data_version", NbtVersion.CODEC).orElse(NbtVersion.ZERO);
    }

    public static int getVanillaVersion(CompoundTag nbt) {
        Optional<Integer> optional = nbt.getInt("vanilla_data_version").or(() -> nbt.getInt("NbtDataVersion"));
        if (optional.isPresent()) {
            return optional.get();
        }
        if (nbt.get("item") instanceof CompoundTag itemNbt) {
            if (itemNbt.contains("Count") || itemNbt.contains("tag")) {
                // 1.20.1版本
                return 3465;
            }
            if (itemNbt.contains("count") || itemNbt.contains("components")) {
                // 1.20.5，物品堆叠组件加入的版本
                return 3837;
            }
        }
        return -1;
    }
}
