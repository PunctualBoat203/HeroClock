package com.heroclock.runtime;

import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.Entity;

public final class NameMatcher {
    private static final boolean USES_DEFAULT_STRING = usesDefaultString();

    private NameMatcher() {}

    public static boolean matches(Entity entity, String expected) {
        return matches(entity.getName(), expected);
    }

    public static boolean matches(Component component, String expected) {
        if (!USES_DEFAULT_STRING || component.getClass() != MutableComponent.class) {
            return component.getString().equals(expected);
        }
        Comparison comparison = new Comparison(expected);
        component.visit(comparison);
        return comparison.matching && comparison.offset == expected.length();
    }

    private static boolean usesDefaultString() {
        try {
            Class<?> owner = MutableComponent.class.getMethod("getString").getDeclaringClass();
            return owner == Component.class || owner == FormattedText.class;
        } catch (ReflectiveOperationException | SecurityException failure) {
            return false;
        }
    }

    private static final class Comparison implements FormattedText.ContentConsumer<Void> {
        private final String expected;
        private int offset;
        private boolean matching;

        private Comparison(String expected) {
            this.expected = expected;
            matching = expected != null;
        }

        @Override
        public Optional<Void> accept(String fragment) {
            if (matching) {
                String text = String.valueOf(fragment);
                if (text.length() <= expected.length() - offset
                        && expected.regionMatches(offset, text, 0, text.length())) offset += text.length();
                else matching = false;
            }
            return Optional.empty();
        }
    }
}
