/*
 * Decompiled with CFR 0.152.
 */
package com.heroclock;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class VersionGuard {
    private static final Map<String, String> SUPPORTED = Map.of("alienevo", "1.1.3", "infinity", "7.2", "omni_evo", "1.0.6", "satsu_iron_man_addon", "3.5.3", "celestialsapien", "1.0.6.1");
    private static final Pattern MODID = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*version\\s*=\\s*\\\"([^\\\"]+)\\\"");

    VersionGuard() {
    }

    static void scan() {
        Path path = Paths.get(System.getProperty("user.dir", "."), new String[0]).resolve("mods");
        if (!Files.isDirectory(path, new LinkOption[0])) {
            return;
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, "*.jar");){
            for (Path path2 : directoryStream) {
                String string;
                String string2;
                String string3 = VersionGuard.read(path2);
                if (string3 == null) continue;
                Matcher matcher = MODID.matcher(string3);
                Matcher matcher2 = VERSION.matcher(string3);
                if (!matcher.find() || !matcher2.find() || (string2 = SUPPORTED.get(matcher.group(1))) == null || (string = matcher2.group(1)).contains(string2)) continue;
                System.err.println("[HeroClock] WARN: " + matcher.group(1) + " " + string + " detected; this release is validated for " + string2 + ". Compatibility remains fail-open.");
            }
        }
        catch (Exception exception) {
            System.err.println("[HeroClock] WARN: compatibility scan unavailable: " + exception.getClass().getSimpleName());
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static String read(Path path) {
        try (ZipFile zipFile = new ZipFile(path.toFile());){
            String string;
            block15: {
                ZipEntry zipEntry = zipFile.getEntry("META-INF/mods.toml");
                if (zipEntry == null) {
                    String string2 = null;
                    return string2;
                }
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                try {
                    string = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    if (inputStream == null) break block15;
                }
                catch (Throwable throwable) {
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                inputStream.close();
            }
            return string;
        }
        catch (Exception exception) {
            return null;
        }
    }
}

