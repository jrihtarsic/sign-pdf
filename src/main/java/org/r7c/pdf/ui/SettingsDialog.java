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

import org.r7c.pdf.config.Settings;
import org.r7c.pdf.config.SettingsLoader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

/**
 * Edits the keystore/signature/output settings persisted in {@code settings.yaml}. Keystore and key
 * passwords are optional: left blank, the main window prompts for them at sign time instead of
 * persisting them to disk.
 */
public class SettingsDialog extends JDialog {

    private final File settingsFile;
    private final Settings settings;
    private boolean saved;

    private final JTextField keystorePathField = new JTextField(28);
    private final JComboBox<String> keystoreTypeCombo = new JComboBox<>(new String[]{"PKCS12", "JKS"});
    private final JTextField keyAliasField = new JTextField(28);
    private final JPasswordField keystorePasswordField = new JPasswordField(28);
    private final JPasswordField keyPasswordField = new JPasswordField(28);
    private final JTextField imagePathField = new JTextField(28);
    private final JTextField dateTimeFormatField = new JTextField(28);
    private final JTextArea textTemplateField = new JTextArea(4, 28);
    private final JTextField defaultSignerNameField = new JTextField(28);
    private final JTextField defaultPurposeField = new JTextField(28);
    private final JTextField defaultContactField = new JTextField(28);
    private final JTextField outputSuffixField = new JTextField(28);

    public SettingsDialog(Frame owner, Settings settings, File settingsFile) {
        super(owner, "Settings", true);
        this.settings = settings;
        this.settingsFile = settingsFile;

        loadFieldsFromSettings();

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACING_LG, UiTheme.SPACING_LG, UiTheme.SPACING_MD, UiTheme.SPACING_LG));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        row = addSectionLabel(form, gbc, row, "Keystore", true);
        row = addRowWithBrowse(form, gbc, row, "Path:", keystorePathField, this::browseForKeystore);
        row = addRow(form, gbc, row, "Type:", keystoreTypeCombo);
        row = addRow(form, gbc, row, "Key alias:", keyAliasField);
        row = addRow(form, gbc, row, "Keystore password (optional):", keystorePasswordField);
        row = addRow(form, gbc, row, "Key password (optional):", keyPasswordField);

        row = addSectionLabel(form, gbc, row, "Signature appearance", false);
        row = addRowWithBrowse(form, gbc, row, "Image path:", imagePathField, this::browseForImage);
        row = addRow(form, gbc, row, "Date/time format:", dateTimeFormatField);
        textTemplateField.setLineWrap(true);
        textTemplateField.setWrapStyleWord(true);
        row = addRow(form, gbc, row, "Text template:", new JScrollPane(textTemplateField));
        row = addHint(form, gbc, row, "Placeholders: ${NAME}, ${DATETIME}, ${ISSUER}, ${SERIAL}");
        row = addRow(form, gbc, row, "Default signer name:", defaultSignerNameField);
        row = addRow(form, gbc, row, "Default purpose:", defaultPurposeField);
        row = addRow(form, gbc, row, "Default contact:", defaultContactField);

        row = addSectionLabel(form, gbc, row, "Output", false);
        addRow(form, gbc, row, "Filename suffix:", outputSuffixField);

        JButton saveButton = new JButton("Save");
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD));
        saveButton.addActionListener(e -> onSave());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, UiTheme.SPACING_SM, UiTheme.SPACING_SM));
        buttons.setBackground(Color.WHITE);
        buttons.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_LG, UiTheme.SPACING_SM, UiTheme.SPACING_LG)));
        buttons.add(cancelButton);
        buttons.add(saveButton);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(saveButton);

        pack();
        setLocationRelativeTo(owner);
    }

    private void loadFieldsFromSettings() {
        keystorePathField.setText(settings.getKeystore().getPath());
        keystoreTypeCombo.setSelectedItem(settings.getKeystore().getType());
        keyAliasField.setText(settings.getKeystore().getKeyAlias());
        keystorePasswordField.setText(settings.getKeystore().getPassword());
        keyPasswordField.setText(settings.getKeystore().getKeyPassword());
        imagePathField.setText(settings.getSignature().getImagePath());
        dateTimeFormatField.setText(settings.getSignature().getDateTimeFormat());
        textTemplateField.setText(settings.getSignature().getTextTemplate());
        defaultSignerNameField.setText(settings.getSignature().getDefaultSignerName());
        defaultPurposeField.setText(settings.getSignature().getDefaultPurpose());
        defaultContactField.setText(settings.getSignature().getDefaultContact());
        outputSuffixField.setText(settings.getOutput().getSuffix());
    }

    private void onSave() {
        settings.getKeystore().setPath(keystorePathField.getText().trim());
        settings.getKeystore().setType((String) keystoreTypeCombo.getSelectedItem());
        settings.getKeystore().setKeyAlias(keyAliasField.getText().trim());
        settings.getKeystore().setPassword(new String(keystorePasswordField.getPassword()));
        settings.getKeystore().setKeyPassword(new String(keyPasswordField.getPassword()));
        settings.getSignature().setImagePath(imagePathField.getText().trim());
        settings.getSignature().setDateTimeFormat(dateTimeFormatField.getText().trim());
        settings.getSignature().setTextTemplate(textTemplateField.getText());
        settings.getSignature().setDefaultSignerName(defaultSignerNameField.getText().trim());
        settings.getSignature().setDefaultPurpose(defaultPurposeField.getText().trim());
        settings.getSignature().setDefaultContact(defaultContactField.getText().trim());
        settings.getOutput().setSuffix(outputSuffixField.getText().trim());

        try {
            SettingsLoader.save(settingsFile, settings);
            saved = true;
            dispose();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to save settings.yaml: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void browseForKeystore() {
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter pkcs12Filter = new FileNameExtensionFilter(
                "PKCS12 keystore (*.p12, *.pfx)", "p12", "pfx");
        FileNameExtensionFilter jksFilter = new FileNameExtensionFilter(
                "JKS keystore (*.jks, *.keystore)", "jks", "keystore");
        chooser.addChoosableFileFilter(pkcs12Filter);
        chooser.addChoosableFileFilter(jksFilter);
        chooser.setFileFilter("JKS".equals(keystoreTypeCombo.getSelectedItem()) ? jksFilter : pkcs12Filter);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            keystorePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseForImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Image files (*.png, *.jpg, *.jpeg, *.gif, *.bmp)", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            imagePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public boolean isSaved() {
        return saved;
    }

    public Settings getSettings() {
        return settings;
    }

    private static int addSectionLabel(JPanel panel, GridBagConstraints gbc, int row, String text, boolean first) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.insets = new Insets(first ? 0 : UiTheme.SPACING_LG, 4, UiTheme.SPACING_SM, 4);
        JLabel label = new JLabel(text.toUpperCase());
        label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
        label.setForeground(UiTheme.ACCENT);
        panel.add(label, gbc);
        gbc.gridwidth = 1;
        gbc.insets = new Insets(4, 4, 4, 4);
        return row + 1;
    }

    private static int addHint(JPanel panel, GridBagConstraints gbc, int row, String text) {
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.ITALIC, 11f));
        label.setForeground(Color.GRAY);
        panel.add(label, gbc);
        gbc.gridwidth = 1;
        return row + 1;
    }

    private static int addRow(JPanel panel, GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        panel.add(field, gbc);
        return row + 1;
    }

    private static int addRowWithBrowse(JPanel panel, GridBagConstraints gbc, int row, String label,
                                        JTextField field, Runnable onBrowse) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> onBrowse.run());
        panel.add(browse, gbc);
        return row + 1;
    }
}
