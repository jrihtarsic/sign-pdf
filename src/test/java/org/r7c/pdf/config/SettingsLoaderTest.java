/*-
 * #START_LICENSE#
 * sign-pdf
 * %%
 * Copyright (C) 2017 - 2026 org.r7c | sign-pdf
 * %%
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * 
 * [PROJECT_HOME]\license\eupl-1.2\license.txt or https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 * #END_LICENSE#
 */
/*
 * sign-pdf
 *
 * Copyright (C) 2025 - 2026  Owner of the domain org.r7c
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * [PROJECT_HOME]\license\eupl-1.2\license.txt or https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package org.r7c.pdf.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(Settings.Signature.DEFAULT_TEXT_TEMPLATE, settings.getSignature().getTextTemplate(),
                "textTemplate omitted from the fixture should fall back to the built-in default");
    }

    @Test
    void load_parsesCustomTextTemplate() throws Exception {
        File fixture = tempDir.resolve("settings.yaml").toFile();
        Files.writeString(fixture.toPath(), """
                signature:
                  textTemplate: "Signer: ${NAME}\\nDate: ${DATETIME}"
                """);

        Settings settings = SettingsLoader.load(fixture);

        assertEquals("Signer: ${NAME}\nDate: ${DATETIME}", settings.getSignature().getTextTemplate());
    }

    // --- resolveExistingSettingsFile / resolveSettingsFileForSave -----------------------------

    private String savedProperty;
    private String savedUserHome;
    private String savedUserDir;

    private void captureSystemProperties() {
        savedProperty = System.getProperty("spdf.settings");
        savedUserHome = System.getProperty("user.home");
        savedUserDir = System.getProperty("user.dir");
    }

    private void restoreSystemProperties() {
        restoreProperty("spdf.settings", savedProperty);
        restoreProperty("user.home", savedUserHome);
        restoreProperty("user.dir", savedUserDir);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    @Test
    void resolveExistingSettingsFile_findsViaProperty() throws Exception {
        captureSystemProperties();
        try {
            Path propertyDir = Files.createDirectory(tempDir.resolve("from-property"));
            Files.writeString(propertyDir.resolve("settings.yaml"), "output:\n  suffix: from-property\n");
            System.setProperty("spdf.settings", propertyDir.toString());

            File resolved = SettingsLoader.resolveExistingSettingsFile("/some/cli/dir/that/does/not/matter");

            assertEquals(propertyDir.resolve("settings.yaml").toFile(), resolved);
        } finally {
            restoreSystemProperties();
        }
    }

    @Test
    void resolveExistingSettingsFile_findsViaCliOverride_whenPropertyUnset() throws Exception {
        captureSystemProperties();
        try {
            System.clearProperty("spdf.settings");
            Path cliDir = Files.createDirectory(tempDir.resolve("from-cli"));
            Files.writeString(cliDir.resolve("settings.yaml"), "output:\n  suffix: from-cli\n");

            File resolved = SettingsLoader.resolveExistingSettingsFile(cliDir.toString());

            assertEquals(cliDir.resolve("settings.yaml").toFile(), resolved);
        } finally {
            restoreSystemProperties();
        }
    }

    @Test
    void resolveExistingSettingsFile_findsInUserFolder_whenNeitherPropertyNorCliSet() throws Exception {
        captureSystemProperties();
        try {
            System.clearProperty("spdf.settings");
            Path userHome = Files.createDirectory(tempDir.resolve("home"));
            Path userDir = Files.createDirectory(tempDir.resolve("cwd"));
            Path userFolder = Files.createDirectories(userHome.resolve(".warp/sign-pdf"));
            Files.writeString(userFolder.resolve("settings.yaml"), "output:\n  suffix: from-user-folder\n");
            System.setProperty("user.home", userHome.toString());
            System.setProperty("user.dir", userDir.toString());

            File resolved = SettingsLoader.resolveExistingSettingsFile(null);

            assertEquals(userFolder.resolve("settings.yaml").toFile(), resolved);
        } finally {
            restoreSystemProperties();
        }
    }

    @Test
    void resolveExistingSettingsFile_fallsBackToWorkDir_whenNothingEarlierMatches() throws Exception {
        captureSystemProperties();
        try {
            System.clearProperty("spdf.settings");
            Path userHome = Files.createDirectory(tempDir.resolve("home"));
            Path userDir = Files.createDirectory(tempDir.resolve("cwd"));
            Files.writeString(userDir.resolve("settings.yaml"), "output:\n  suffix: from-workdir\n");
            System.setProperty("user.home", userHome.toString());
            System.setProperty("user.dir", userDir.toString());

            File resolved = SettingsLoader.resolveExistingSettingsFile(null);

            assertEquals(userDir.resolve("settings.yaml").toFile(), resolved);
        } finally {
            restoreSystemProperties();
        }
    }

    @Test
    void resolveExistingSettingsFile_returnsNull_whenNothingExistsAnywhere() throws Exception {
        captureSystemProperties();
        try {
            System.clearProperty("spdf.settings");
            Path userHome = Files.createDirectory(tempDir.resolve("home"));
            Path userDir = Files.createDirectory(tempDir.resolve("cwd"));
            System.setProperty("user.home", userHome.toString());
            System.setProperty("user.dir", userDir.toString());

            File resolved = SettingsLoader.resolveExistingSettingsFile(null);

            assertNull(resolved);
        } finally {
            restoreSystemProperties();
        }
    }

    @Test
    void resolveSettingsFileForSave_picksPropertyDir_whenSet() throws Exception {
        captureSystemProperties();
        try {
            Path propertyDir = tempDir.resolve("save-to-property");
            System.setProperty("spdf.settings", propertyDir.toString());

            File target = SettingsLoader.resolveSettingsFileForSave();

            assertEquals(propertyDir.resolve("settings.yaml").toFile(), target);
        } finally {
            restoreSystemProperties();
        }
    }

    @Test
    void resolveSettingsFileForSave_createsAndPicksUserFolder_whenPropertyUnset() throws Exception {
        captureSystemProperties();
        try {
            System.clearProperty("spdf.settings");
            Path userHome = Files.createDirectory(tempDir.resolve("home"));
            System.setProperty("user.home", userHome.toString());
            Path expectedUserFolder = userHome.resolve(".warp/sign-pdf");
            assertTrue(Files.notExists(expectedUserFolder));

            File target = SettingsLoader.resolveSettingsFileForSave();

            assertEquals(expectedUserFolder.resolve("settings.yaml").toFile(), target);
            assertTrue(Files.isDirectory(expectedUserFolder));
        } finally {
            restoreSystemProperties();
        }
    }

    @Test
    void resolveSettingsFileForSave_fallsBackToWorkDir_whenUserFolderCannotBeCreated() throws Exception {
        captureSystemProperties();
        try {
            System.clearProperty("spdf.settings");
            // ".warp" is a plain file here, not a directory, so mkdirs() for "<home>/.warp/sign-pdf"
            // is guaranteed to fail on every platform.
            Path userHome = Files.createDirectory(tempDir.resolve("home"));
            Files.writeString(userHome.resolve(".warp"), "blocking file");
            Path userDir = Files.createDirectory(tempDir.resolve("cwd"));
            System.setProperty("user.home", userHome.toString());
            System.setProperty("user.dir", userDir.toString());

            File target = SettingsLoader.resolveSettingsFileForSave();

            assertEquals(userDir.resolve("settings.yaml").toFile(), target);
        } finally {
            restoreSystemProperties();
        }
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
        original.getSignature().setTextTemplate("Signer: ${NAME}\nDate: ${DATETIME}");
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
        assertEquals("Signer: ${NAME}\nDate: ${DATETIME}", reloaded.getSignature().getTextTemplate());
        assertEquals("-final", reloaded.getOutput().getSuffix());
    }
}
