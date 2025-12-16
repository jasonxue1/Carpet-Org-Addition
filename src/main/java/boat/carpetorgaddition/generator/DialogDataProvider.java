package boat.carpetorgaddition.generator;

import boat.carpetorgaddition.dialog.DialogProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.dialog.Dialog;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@NullMarked
public class DialogDataProvider implements DataProvider {
    private final PackOutput.PathProvider pathResolver;
    private final CompletableFuture<HolderLookup.Provider> registryLookup;

    public DialogDataProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registryLookup) {
        this.pathResolver = output.createRegistryElementsPathProvider(Registries.DIALOG);
        this.registryLookup = registryLookup;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return this.registryLookup.thenCompose(lookup -> {
            RegistryOps<JsonElement> context = lookup.createSerializationContext(JsonOps.INSTANCE);
            ArrayList<CompletableFuture<?>> futures = new ArrayList<>();
            for (Map.Entry<Identifier, Dialog> entry : DialogProvider.entrySet()) {
                JsonObject json = Dialog.CODEC.encodeStart(context, Holder.direct(entry.getValue())).getOrThrow().getAsJsonObject();
                futures.add(DataProvider.saveStable(output, json, this.pathResolver.json(entry.getKey())));
            }
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        });
    }

    @Override
    public String getName() {
        return "Dialog";
    }
}
