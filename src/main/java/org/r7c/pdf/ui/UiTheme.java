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

    /** Darker, compact toolbar sitting directly above the PDF viewer (page nav / zoom / fit controls). */
    static final Color VIEWER_TOOLBAR_BG = new Color(0x32353C);
    static final Color VIEWER_TOOLBAR_BORDER = new Color(0x22242A);
    static final Color VIEWER_TOOLBAR_TEXT_MUTED = new Color(0xAAAFBC);

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
