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
package org.r7c.pdf.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads/saves {@link Settings} from/to a YAML file. The file is always named {@code settings.yaml};
 * which directory it lives in is resolved via {@link #resolveExistingSettingsFile(String)} /
 * {@link #resolveSettingsFileForSave()} so the CLI entry point and the UI can share one file even
 * when it isn't sitting next to the jar. See {@code settings.md} for the full design.
 */
public final class SettingsLoader {

    private static final String SETTINGS_FILE_NAME = "settings.yaml";
    private static final String SPDF_SETTINGS_PROPERTY = "spdf.settings";
    private static final String USER_FOLDER_RELATIVE_PATH = ".warp/sign-pdf";

    private SettingsLoader() {
    }

    /**
     * Search order for an already-existing {@code settings.yaml}: {@code spdf.settings} system
     * property, then {@code cliDirOverride} (the CLI's {@code -s <dir>} flag; {@code null} from the
     * UI, which has no argv), then {@code ~/.warp/sign-pdf/}, then the current working directory,
     * then the folder containing the running jar. Returns {@code null} if none of them have one.
     */
    public static File resolveExistingSettingsFile(String cliDirOverride) {
        for (File dir : candidateDirsForLoad(cliDirOverride)) {
            File file = new File(dir, SETTINGS_FILE_NAME);
            if (file.isFile()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Where a brand-new {@code settings.yaml} should be written when none exists yet:
     * {@code spdf.settings} system property (if set), else {@code ~/.warp/sign-pdf/} (created if
     * missing), else the current working directory, else the folder containing the running jar.
     */
    public static File resolveSettingsFileForSave() {
        File propertyDir = propertyDir();
        if (propertyDir != null) {
            return new File(propertyDir, SETTINGS_FILE_NAME);
        }
        File userFolder = userFolderDir();
        if (userFolder.isDirectory() || userFolder.mkdirs()) {
            return new File(userFolder, SETTINGS_FILE_NAME);
        }
        File workDir = workDir();
        if (workDir.isDirectory()) {
            return new File(workDir, SETTINGS_FILE_NAME);
        }
        return new File(jarDir(), SETTINGS_FILE_NAME);
    }

    /**
     * The file to use for the rest of a run: an existing {@code settings.yaml} if one is found via
     * {@link #resolveExistingSettingsFile(String)}, otherwise the save target chosen by
     * {@link #resolveSettingsFileForSave()} (not yet created on disk).
     */
    public static File resolveSettingsFile(String cliDirOverride) {
        File existing = resolveExistingSettingsFile(cliDirOverride);
        return existing != null ? existing : resolveSettingsFileForSave();
    }

    private static List<File> candidateDirsForLoad(String cliDirOverride) {
        List<File> candidates = new ArrayList<>();
        File propertyDir = propertyDir();
        if (propertyDir != null) {
            candidates.add(propertyDir);
        }
        if (cliDirOverride != null && !cliDirOverride.isBlank()) {
            candidates.add(new File(cliDirOverride));
        }
        candidates.add(userFolderDir());
        candidates.add(workDir());
        candidates.add(jarDir());
        return candidates;
    }

    private static File propertyDir() {
        String property = System.getProperty(SPDF_SETTINGS_PROPERTY);
        return (property != null && !property.isBlank()) ? new File(property) : null;
    }

    private static File userFolderDir() {
        return new File(System.getProperty("user.home"), USER_FOLDER_RELATIVE_PATH);
    }

    private static File workDir() {
        return new File(System.getProperty("user.dir"));
    }

    private static File jarDir() {
        try {
            File location = new File(SettingsLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            return location.isFile() ? location.getParentFile() : location;
        } catch (URISyntaxException | NullPointerException e) {
            return workDir();
        }
    }

    public static Settings load(File file) throws IOException {
        if (!file.isFile()) {
            return new Settings();
        }
        Yaml yaml = new Yaml(new Constructor(Settings.class, new LoaderOptions()));
        try (InputStream in = new FileInputStream(file)) {
            Settings settings = yaml.load(in);
            return settings != null ? settings : new Settings();
        }
    }

    public static void save(File file, Settings settings) throws IOException {
        Map<String, Object> keystore = new LinkedHashMap<>();
        keystore.put("path", settings.getKeystore().getPath());
        keystore.put("type", settings.getKeystore().getType());
        keystore.put("keyAlias", settings.getKeystore().getKeyAlias());
        keystore.put("password", settings.getKeystore().getPassword());
        keystore.put("keyPassword", settings.getKeystore().getKeyPassword());

        Map<String, Object> signature = new LinkedHashMap<>();
        signature.put("imagePath", settings.getSignature().getImagePath());
        signature.put("dateTimeFormat", settings.getSignature().getDateTimeFormat());
        signature.put("textTemplate", settings.getSignature().getTextTemplate());
        signature.put("defaultSignerName", settings.getSignature().getDefaultSignerName());
        signature.put("defaultPurpose", settings.getSignature().getDefaultPurpose());
        signature.put("defaultContact", settings.getSignature().getDefaultContact());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("suffix", settings.getOutput().getSuffix());

        Map<String, Object> ui = new LinkedHashMap<>();
        ui.put("lastOpenDir", settings.getUi().getLastOpenDir());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("keystore", keystore);
        root.put("signature", signature);
        root.put("output", output);
        root.put("ui", ui);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (Writer writer = new FileWriter(file)) {
            yaml.dump(root, writer);
        }
    }
}
