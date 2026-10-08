package com.heroclock.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ResourceFingerprintTest {
    @TempDir Path directory;

    @Test
    void onlyExactBoundedResourcesMatch() throws Exception {
        String sha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        Path resource = directory.resolve("power.json");
        Files.writeString(resource, "abc");
        assertTrue(ResourceFingerprint.matches(resource, sha256));
        Files.writeString(resource, "abc\n");
        assertFalse(ResourceFingerprint.matches(resource, sha256));
        assertFalse(ResourceFingerprint.matches(directory.resolve("missing"), sha256));
        assertFalse(ResourceFingerprint.matches(directory, sha256));
        Files.write(resource, new byte[1_048_577]);
        assertFalse(ResourceFingerprint.matches(resource, sha256));
    }
}
