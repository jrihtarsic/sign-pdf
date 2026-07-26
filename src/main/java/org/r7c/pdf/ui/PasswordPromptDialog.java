package org.r7c.pdf.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Modal prompt for whichever of the keystore/key passwords {@code settings.yaml} left blank.
 * Passwords entered here are used for this signing operation only and are never persisted.
 */
public class PasswordPromptDialog extends JDialog {

    private final JPasswordField keystorePasswordField;
    private final JPasswordField keyPasswordField;
    private boolean confirmed;

    public PasswordPromptDialog(Frame owner, boolean needKeystorePassword, boolean needKeyPassword) {
        super(owner, "Password required", true);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACING_LG, UiTheme.SPACING_LG, UiTheme.SPACING_MD, UiTheme.SPACING_LG));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        if (needKeystorePassword) {
            keystorePasswordField = new JPasswordField(20);
            addRow(form, gbc, row++, "Keystore password:", keystorePasswordField);
        } else {
            keystorePasswordField = null;
        }

        if (needKeyPassword) {
            keyPasswordField = new JPasswordField(20);
            addRow(form, gbc, row++, "Key password:", keyPasswordField);
        } else {
            keyPasswordField = null;
        }

        JButton okButton = new JButton("OK");
        okButton.setFont(okButton.getFont().deriveFont(Font.BOLD));
        okButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACING_SM, UiTheme.SPACING_SM));
        buttons.setBackground(Color.WHITE);
        buttons.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_LG, UiTheme.SPACING_SM, UiTheme.SPACING_LG)));
        buttons.add(cancelButton);
        buttons.add(okButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JPasswordField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public char[] getKeystorePassword() {
        return keystorePasswordField != null ? keystorePasswordField.getPassword() : new char[0];
    }

    public char[] getKeyPassword() {
        return keyPasswordField != null ? keyPasswordField.getPassword() : new char[0];
    }
}
