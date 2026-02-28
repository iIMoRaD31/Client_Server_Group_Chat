package com.server.util;

import java.util.Random;

/**
 * Generates soft random colors so the user list stays readable.
 */
public final class ColorUtil {
    private static final Random RANDOM = new Random();

    private ColorUtil() {}

    /**
     * @return hex color string (e.g. #aabbcc) biased toward pastel shades.
     */
    public static String randomPastelHex() {
        int r = 150 + RANDOM.nextInt(106);
        int g = 150 + RANDOM.nextInt(106);
        int b = 150 + RANDOM.nextInt(106);
        return String.format("#%02x%02x%02x", r, g, b);
    }
}
