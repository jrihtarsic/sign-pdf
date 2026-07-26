package org.r7c.pdf.ui;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

/**
 * Reskins the JDK-bundled Nimbus look and feel into a flatter, lighter theme (Nimbus derives its whole
 * palette from a handful of base colors, so overriding those few keys restyles every component
 * consistently) and exposes the same palette/spacing constants to the rest of {@code org.r7c.pdf.ui}.
 * Deliberately no third-party L&F library: Nimbus already ships with the JDK.
 */
final class UiTheme {

    static final Color ACCENT = new Color(0x2F6FED);
    static final Color SURFACE = new Color(0xF3F4F6);
    static final Color BORDER = new Color(0xD7DAE0);
    static final Color TEXT_MUTED = new Color(0x6B7280);

    static final int SPACING_SM = 6;
    static final int SPACING_MD = 12;
    static final int SPACING_LG = 20;

    private UiTheme() {
    }

    static void install() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // fall back to whatever the platform default is
        }

        Font baseFont = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        UIManager.put("defaultFont", baseFont);
        UIManager.put("control", SURFACE);
        UIManager.put("info", SURFACE);
        UIManager.put("nimbusLightBackground", Color.WHITE);
        UIManager.put("text", new Color(0x1F2430));
        UIManager.put("nimbusBase", ACCENT);
        UIManager.put("nimbusBlueGrey", new Color(0xE4E7EC));
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", ACCENT);
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusBorder", BORDER);
    }
}
