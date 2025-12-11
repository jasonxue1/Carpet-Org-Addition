package org.util;

import net.minecraft.core.UUIDUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * @author DeepSeek
 */
public class OfflinePlayerUuidBruteForce implements Iterator<String> {
    private final char[] chars;
    private final int maxLength;
    private int currentLength = 1;
    private int[] indices;

    public OfflinePlayerUuidBruteForce(char[] chars, int maxLength) {
        this.chars = chars;
        this.maxLength = maxLength;
        this.indices = new int[currentLength];
    }

    @Override
    public boolean hasNext() {
        return currentLength <= maxLength;
    }

    @Override
    public String next() {
        if (hasNext()) {// 生成当前索引对应的字符串
            StringBuilder sb = new StringBuilder();
            for (int i : indices) {
                sb.append(chars[i]);
            }

            // 递增索引
            increment();
            return sb.toString();
        }
        return null;
    }

    private void increment() {
        int carry = 1;
        for (int i = indices.length - 1; i >= 0; i--) {
            int newVal = indices[i] + carry;
            if (newVal < chars.length) {
                indices[i] = newVal;
                carry = 0;
                break;
            } else {
                indices[i] = 0;
            }
        }
        // 处理进位溢出，进入下一长度
        if (carry == 1) {
            currentLength++;
            if (currentLength > maxLength) {
                return;
            }
            indices = new int[currentLength];
        }
    }

    public static void main(String[] args) {
        ArrayList<Character> characters = new ArrayList<>();
        for (char c = '0'; c <= '9'; c++) {
            characters.add(c);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            characters.add(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            characters.add(c);
        }
        characters.add('_');
        char[] chars = new char[characters.size()];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = characters.get(i);
        }
        OfflinePlayerUuidBruteForce iterator = new OfflinePlayerUuidBruteForce(chars, 16);
        ArrayList<UUID> list = new ArrayList<>(Stream.of(new String[]{}).map(UUID::fromString).toList());
        while (iterator.hasNext()) {
            if (list.isEmpty()) {
                break;
            }
            String name = iterator.next();
            UUID uuid = UUIDUtil.createOfflinePlayerUUID(name);
            if (list.contains(uuid)) {
                System.out.println(name + ",   " + uuid);
                list.remove(uuid);
            }
        }
    }
}