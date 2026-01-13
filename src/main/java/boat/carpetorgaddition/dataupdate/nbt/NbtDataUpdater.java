package boat.carpetorgaddition.dataupdate.nbt;

import boat.carpetorgaddition.util.ServerUtils;
import boat.carpetorgaddition.wheel.nbt.NbtVersion;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.fixes.References;

public abstract class NbtDataUpdater {
    protected final MinecraftServer server;
    protected final DataFixer fixerUpper;
    /**
     * {@code Carpet Org Addition}的数据版本
     */
    public static final String DATA_VERSION = "data_version";
    /**
     * {@code Minecraft}的数据版本
     */
    public static final String VANILLA_DATA_VERSION = "vanilla_data_version";
    /**
     * 当前{@code Minecraft}的NBT数据版本
     */
    public static final int CURRENT_VANILLA_DATA_VERSION = ServerUtils.getVanillaDataVersion();

    public NbtDataUpdater(MinecraftServer server) {
        this.server = server;
        this.fixerUpper = this.server.getFixerUpper();
    }

    public CompoundTag update(CompoundTag nbt, NbtVersion version, int vanillaVersion) {
        CompoundTag newNbt = this.updateDataFormat(nbt, version);
        return this.updateVanillaDataFormat(newNbt, vanillaVersion);
    }

    protected abstract CompoundTag updateDataFormat(CompoundTag old, NbtVersion version);

    protected abstract CompoundTag updateVanillaDataFormat(CompoundTag old, int version);

    protected Tag updateItemStack(Tag nbt, int version) {
        Dynamic<Tag> input = new Dynamic<>(NbtOps.INSTANCE, nbt);
        Dynamic<Tag> result = this.fixerUpper.update(References.ITEM_STACK, input, version, CURRENT_VANILLA_DATA_VERSION);
        return result.getValue();
    }
}
