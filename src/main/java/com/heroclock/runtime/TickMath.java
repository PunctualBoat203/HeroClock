package com.heroclock.runtime;

public final class TickMath {
    private TickMath() {}

    public static long add(long base, long delta) {
        if (delta > 0 && base > Long.MAX_VALUE - delta) return Long.MAX_VALUE;
        if (delta < 0 && base < Long.MIN_VALUE - delta) return Long.MIN_VALUE;
        return base + delta;
    }

    public static String key(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Timer key must not be blank");
        String trimmed = key.trim();
        int length = Math.min(96, trimmed.length());
        boolean simple = true;
        for (int i = 0; i < length; i++) {
            if (!valid(trimmed.charAt(i))) { simple = false; break; }
        }
        if (simple) return length == trimmed.length() ? trimmed : trimmed.substring(0, length);
        StringBuilder normalized = new StringBuilder(length);
        for (int i = 0; i < trimmed.length() && normalized.length() < 96;) {
            int c = trimmed.codePointAt(i);
            normalized.append(valid(c) ? (char) c : '_');
            i += Character.charCount(c);
        }
        return normalized.toString();
    }

    private static boolean valid(int c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                || c >= '0' && c <= '9' || c == '_' || c == '.' || c == ':' || c == '-';
    }
}
