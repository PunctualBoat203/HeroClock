package com.heroclock.runtime;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TickMathTest {
    @Test void saturatesInsteadOfWrapping() {
        assertEquals(Long.MAX_VALUE, TickMath.add(Long.MAX_VALUE - 1, 20));
        assertEquals(Long.MIN_VALUE, TickMath.add(Long.MIN_VALUE + 1, -20));
        assertEquals(5, TickMath.add(10, -5));
        assertEquals(Long.MIN_VALUE + 10, TickMath.add(10, Long.MIN_VALUE));
    }

    @Test void retainsLegacySavedKeyMapping() {
        for (String input : new String[]{"omni_evo.recal_failsafe", " x y/z ", "a".repeat(110), "hello🌍"}) {
            String legacy = input.trim().replaceAll("[^A-Za-z0-9_.:-]", "_");
            legacy = legacy.substring(0, Math.min(96, legacy.length()));
            assertEquals(legacy, TickMath.key(input));
        }
        assertThrows(IllegalArgumentException.class, () -> TickMath.key(" "));
    }

    @Test void onlySuppressesImmutableUnchangedValues() {
        assertTrue(ScalarChanges.unchanged("value", new String("value")));
        assertTrue(ScalarChanges.unchanged(1000, 1000));
        assertFalse(ScalarChanges.unchanged(1, 2));
        assertFalse(ScalarChanges.unchanged(0.0, -0.0));
        int[] mutable = {1};
        assertFalse(ScalarChanges.unchanged(mutable, mutable));
        assertTrue(ScalarChanges.unchanged(null, null));
        assertFalse(ScalarChanges.unchanged(null, "x"));
    }
}
