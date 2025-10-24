package org.carpetorgaddition.network.c2s;

import com.google.gson.JsonObject;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import org.carpetorgaddition.network.PacketUtils;
import org.carpetorgaddition.util.IOUtils;

public record ObjectSearchTaskC2SPacket(Type type, JsonObject json) implements CustomPayload {
    public static final int CURRENT_VERSION = 1;
    public static final Id<ObjectSearchTaskC2SPacket> ID = PacketUtils.createId("object_search_task");
    public static final PacketCodec<RegistryByteBuf, ObjectSearchTaskC2SPacket> CODEC = new PacketCodec<>() {
        @Override
        public void encode(RegistryByteBuf buf, ObjectSearchTaskC2SPacket value) {
            // 数据包版本
            buf.writeInt(CURRENT_VERSION);
            // 搜索类型
            buf.writeString(value.type.toString());
            buf.writeString(IOUtils.jsonAsString(value.json));
        }

        @Override
        public ObjectSearchTaskC2SPacket decode(RegistryByteBuf buf) {
            int version = buf.readInt();
            if (version > CURRENT_VERSION) {
                return new ObjectSearchTaskC2SPacket(Type.INVALID, null);
            }
            Type type = switch (buf.readString()) {
                case "item" -> Type.ITEM;
                case "offline_player_item" -> Type.OFFLINE_PLAYER_ITEM;
                case "block" -> Type.BLOCK;
                case "trade_item" -> Type.TRADE_ITEM;
                case "trade_enchanted_book" -> Type.TRADE_ENCHANTED_BOOK;
                default -> Type.INVALID;
            };
            String jsonString = buf.readString();
            JsonObject json = IOUtils.stringAsJson(jsonString);
            return new ObjectSearchTaskC2SPacket(type, json);
        }
    };

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public enum Type {
        ITEM,
        OFFLINE_PLAYER_ITEM,
        BLOCK,
        TRADE_ITEM,
        TRADE_ENCHANTED_BOOK,
        INVALID;

        @Override
        public String toString() {
            return switch (this) {
                case ITEM -> "item";
                case OFFLINE_PLAYER_ITEM -> "offline_player_item";
                case BLOCK -> "block";
                case TRADE_ITEM -> "trade_item";
                case TRADE_ENCHANTED_BOOK -> "trade_enchanted_book";
                case INVALID -> "invalid";
            };
        }
    }
}
