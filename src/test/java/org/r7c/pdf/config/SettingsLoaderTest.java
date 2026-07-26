package org.r7c.pdf.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void load_missingFile_returnsDefaults() throws Exception {
        Settings settings = SettingsLoader.load(tempDir.resolve("does-not-exist.yaml").toFile());

        assertEquals("PKCS12", settings.getKeystore().getType());
        assertEquals("", settings.getKeystore().getPath());
        assertEquals("-signed", settings.getOutput().getSuffix());
    }

    @Test
    void load_parsesFixtureYaml() throws Exception {
        File fixture = tempDir.resolve("settings.yaml").toFile();
        Files.writeString(fixture.toPath(), """
                keystore:
                  path: /path/to/keystore.p12
                  type: PKCS12
                  keyAlias: mykey
                  password: secret
                  keyPassword: secret2
                signature:
                  imagePath: /path/to/sig.png
                  dateTimeFormat: "dd. MM. yyyy HH:mm"
                  defaultSignerName: Jane Doe
                  defaultPurpose: Approval
                  defaultContact: jane@example.org
                output:
                  suffix: "-signed"
                """);

        Settings settings = SettingsLoader.load(fixture);

        assertEquals("/path/to/keystore.p12", settings.getKeystore().getPath());
        assertEquals("PKCS12", settings.getKeystore().getType());
        assertEquals("mykey", settings.getKeystore().getKeyAlias());
        assertEquals("secret", settings.getKeystore().getPassword());
        assertEquals("secret2", settings.getKeystore().getKeyPassword());
        assertEquals("/path/to/sig.png", settings.getSignature().getImagePath());
        assertEquals("Jane Doe", settings.getSignature().getDefaultSignerName());
        assertEquals("Approval", settings.getSignature().getDefaultPurpose());
        assertEquals("jane@example.org", settings.getSignature().getDefaultContact());
        assertEquals("-signed", settings.getOutput().getSuffix());
    }

    @Test
    void saveThenLoad_roundTrips() throws Exception {
        Settings original = new Settings();
        original.getKeystore().setPath("/ks/path.p12");
        original.getKeystore().setType("JKS");
        original.getKeystore().setKeyAlias("alias1");
        original.getKeystore().setPassword("");
        original.getKeystore().setKeyPassword("");
        original.getSignature().setImagePath("/img/sig.png");
        original.getSignature().setDefaultSignerName("John Smith");
        original.getOutput().setSuffix("-final");

        File file = tempDir.resolve("roundtrip.yaml").toFile();
        SettingsLoader.save(file, original);
        assertTrue(file.isFile());

        Settings reloaded = SettingsLoader.load(file);
        assertEquals("/ks/path.p12", reloaded.getKeystore().getPath());
        assertEquals("JKS", reloaded.getKeystore().getType());
        assertEquals("alias1", reloaded.getKeystore().getKeyAlias());
        assertEquals("/img/sig.png", reloaded.getSignature().getImagePath());
        assertEquals("John Smith", reloaded.getSignature().getDefaultSignerName());
        assertEquals("-final", reloaded.getOutput().getSuffix());
    }
}
