package org.carpetorgaddition.network.codec;

import com.google.gson.JsonObject;

public interface JsonObjectCodec<T> {
    JsonObject encode(T value);

    T decode(JsonObject json);
}
