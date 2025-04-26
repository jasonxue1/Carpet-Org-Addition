package org.carpetorgaddition.util.wheel;

import net.minecraft.text.Text;
import org.carpetorgaddition.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    public void setComment(@Nullable String comment) {
        this.comment = comment == null ? "" : comment;
    }

    public Text getText() {
        return TextUtils.createText(this.comment);
    }

    @Override
    public String toString() {
        return this.comment;
    }
}
