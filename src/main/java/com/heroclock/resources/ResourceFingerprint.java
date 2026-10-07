package com.heroclock.resources;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class ResourceFingerprint {
    private ResourceFingerprint() {}

    static boolean matches(Path path, String expected) throws IOException {
        if (!Files.isRegularFile(path)) return false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > 1_048_576) return false;
                    digest.update(buffer, 0, count);
                }
            }
            return expected.equals(HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
