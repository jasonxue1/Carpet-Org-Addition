package boat.carpetorgaddition.wheel;

import boat.carpetorgaddition.util.MathUtils;
import boat.carpetorgaddition.wheel.text.TextBuilder;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;

public class ProgressBar {
    private final double end;
    private double progress = 0.0;

    public ProgressBar(int end) {
        this.end = end;
    }

    public void setProgress(int current) {
        this.progress = MathUtils.normalize(current, 0.0, this.end);
    }

    public Component getDisplay() {
        DecimalFormat formatter = new DecimalFormat("#.##");
        String format = formatter.format(this.progress * 100.0);
        return TextBuilder.create("%s%%".formatted(format));
    }
}
