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
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads/saves {@link Settings} from/to a YAML file. The default location is {@code settings.yaml}
 * next to the running jar, so both the CLI entry point and the UI share one file.
 */
public final class SettingsLoader {

    private SettingsLoader() {
    }

    public static File defaultSettingsFile() {
        try {
            File location = new File(SettingsLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            File dir = location.isFile() ? location.getParentFile() : location;
            return new File(dir, "settings.yaml");
        } catch (URISyntaxException | NullPointerException e) {
            return new File("settings.yaml");
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
