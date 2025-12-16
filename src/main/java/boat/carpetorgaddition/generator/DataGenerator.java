package boat.carpetorgaddition.generator;

import boat.carpetorgaddition.CarpetOrgAddition;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        if (CarpetOrgAddition.DIALOG_DATA_GENERATOR) {
            FabricDataGenerator.Pack pack = generator.createPack();
            pack.addProvider(DialogDataProvider::new);
            if (CarpetOrgAddition.DIALOG_PAUSE_ADDITIONS) {
                pack.addProvider(DialogTagProvider::new);
            }
        }
    }
}
