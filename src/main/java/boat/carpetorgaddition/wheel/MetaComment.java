package boat.carpetorgaddition.wheel;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * 用来给一些功能添加注释
 */
public class MetaComment {
    @NotNull
    private String comment = "";

    public MetaComment() {
    }

    public MetaComment(@NotNull String comment) {
        this.comment = comment;
    }

    /**
     * @return 此注释是否有内容
     */
    public boolean hasContent() {
        return !this.isEmpty();
    }

    public boolean isEmpty() {
        return this.comment.isBlank();
    }

    public @NotNull String getComment() {
        if (this.isEmpty()) {
            return "";
        }
        return this.comment;
    }

    public Component getText() {
        return TextBuilder.create(this.comment);
    }

    @Override
    public String toString() {
        return this.comment;
    }
}
