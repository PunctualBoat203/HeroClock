package com.heroclock.runtime;

import java.util.UUID;

public final class ScalarChanges {
    private ScalarChanges() {}

    public static boolean unchanged(Object before, Object after) {
        if (before == null) return after == null;
        return (before instanceof String || before instanceof Boolean || before instanceof Integer
                || before instanceof Long || before instanceof Double || before instanceof Float
                || before instanceof Short || before instanceof Byte || before instanceof Character
                || before instanceof Enum<?> || before instanceof UUID) && before.equals(after);
    }
}
