package com.heroclock;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

final class CompanionBridgeTest {
    @Test
    void reflectionSurfaceIsPublicForOptionalCompanions() throws Exception {
        assertTrue(Modifier.isPublic(CompanionBridge.class.getModifiers()),
                "CompanionBridge must stay public for Class.forName/getMethod callers");

        Method handshake = CompanionBridge.class.getMethod("handshake");
        assertTrue(Modifier.isPublic(handshake.getModifiers()),
                "handshake must stay public for Class.getMethod callers");
        assertTrue(Modifier.isStatic(handshake.getModifiers()),
                "handshake must stay static for reflective null-target invocation");
    }
}
