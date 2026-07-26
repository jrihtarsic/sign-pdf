package org.r7c.pdf.ui;

import javax.swing.SwingUtilities;

/** Swing UI entry point (see {@code PadesUtils.main} for the CLI entry point). */
public final class SignPdfApp {

    private SignPdfApp() {
    }

    public static void main(String[] args) {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            try {
                new MainFrame().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
