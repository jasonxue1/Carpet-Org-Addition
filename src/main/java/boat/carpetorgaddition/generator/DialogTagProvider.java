package boat.carpetorgaddition.generator;

import boat.carpetorgaddition.dialog.DialogProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.tags.DialogTags;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class DialogTagProvider extends FabricTagProvider<Dialog> {
    public DialogTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.DIALOG, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider wrapperLookup) {
        this.getOrCreateRawBuilder(DialogTags.PAUSE_SCREEN_ADDITIONS).addOptionalElement(DialogProvider.START);
    }
}
