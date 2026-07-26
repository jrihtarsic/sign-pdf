package org.r7c.pdf.ui;

import org.r7c.pdf.config.Settings;
import org.r7c.pdf.config.SettingsLoader;
import org.r7c.pdf.pades.PadesUtils;
import org.r7c.pdf.pades.SignatureFieldSpec;
import org.r7c.pdf.pades.SigningConfig;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

/**
 * Top-level window: open a PDF, page through it, mark where the visible signature goes (a freshly
 * drawn rectangle, or an existing empty signature field already on the PDF), then sign and save.
 * Keystore/signature-image configuration lives in {@code settings.yaml}, editable via the Settings menu.
 */
public class MainFrame extends JFrame {

    private final PadesUtils padesUtils = new PadesUtils();
    private final File settingsFile = SettingsLoader.defaultSettingsFile();
    private Settings settings;

    private File currentPdfFile;

    private final PdfViewerPanel viewerPanel = new PdfViewerPanel();
    private final JCheckBox showSignatureCheckBox = new JCheckBox("Show signature", true);
    private final JButton openButton = new JButton("Open PDF...");
    private final JLabel pageLabel = new JLabel("-");
    private final JButton prevPageButton = new JButton("‹ Prev");
    private final JButton nextPageButton = new JButton("Next ›");
    private final JButton zoomOutButton = new JButton("−");
    private final JButton zoomInButton = new JButton("+");
    private final JButton fitWidthButton = new JButton("Fit Width");
    private final JButton fitPageButton = new JButton("Fit Page");
    private final JTextField signerNameField = new JTextField(18);
    private final JTextField purposeField = new JTextField(18);
    private final JTextField contactField = new JTextField(18);
    private final JButton signButton = new JButton("Sign & Save...");
    private final JLabel statusBarLabel = new JLabel("No file open.");
    private final JTextArea logArea = new JTextArea(6, 40);
    private final JButton logToggleButton = new JButton();
    private JScrollPane logScrollPane;
    private boolean logExpanded = true;

    public MainFrame() throws IOException {
        super("sign-pdf");
        settings = SettingsLoader.load(settingsFile);

        setJMenuBar(buildMenuBar());
        JScrollPane viewerScroll = new JScrollPane(viewerPanel);
        viewerScroll.setBorder(BorderFactory.createEmptyBorder());
        viewerScroll.getViewport().setBackground(UiTheme.SURFACE);
        // Reserve the vertical scrollbar's width up front: pages are usually taller than the viewport,
        // so the scrollbar would otherwise appear only *after* fitWidth() has already measured the
        // (temporarily wider) extent, leaving the fitted page just slightly too wide.
        viewerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buildLogPanel(), BorderLayout.CENTER);
        southPanel.add(buildStatusBar(), BorderLayout.SOUTH);

        getContentPane().add(buildToolBar(), BorderLayout.NORTH);
        getContentPane().add(viewerScroll, BorderLayout.CENTER);
        getContentPane().add(buildSidebar(), BorderLayout.EAST);
        getContentPane().add(southPanel, BorderLayout.SOUTH);

        prefillFromSettings();
        signButton.setEnabled(false);
        prevPageButton.setEnabled(false);
        nextPageButton.setEnabled(false);
        getRootPane().setDefaultButton(signButton);
        installZoomShortcuts();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 850);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");

        JMenuItem openItem = new JMenuItem("Open PDF...");
        openItem.addActionListener(e -> openPdf());
        JMenuItem settingsItem = new JMenuItem("Settings...");
        settingsItem.addActionListener(e -> openSettingsDialog());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());

        fileMenu.add(openItem);
        fileMenu.add(settingsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    private JPanel buildToolBar() {
        JPanel toolBar = new JPanel();
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.Y_AXIS));
        toolBar.setBackground(Color.WHITE);
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.BORDER));
        toolBar.add(buildActionsRow());
        toolBar.add(buildNavigationRow());
        return toolBar;
    }

    /** Primary document actions: open a PDF, sign and save it. */
    private JPanel buildActionsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACING_SM, UiTheme.SPACING_SM));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createEmptyBorder(
                UiTheme.SPACING_SM, UiTheme.SPACING_MD, UiTheme.SPACING_SM, UiTheme.SPACING_MD));

        openButton.addActionListener(e -> openPdf());
        signButton.setFont(signButton.getFont().deriveFont(Font.BOLD));
        signButton.addActionListener(e -> signAndSave());

        row.add(openButton);
        row.add(signButton);
        return row;
    }

    /** Smaller, secondary row: page navigation, zoom, and fit controls. */
    private JPanel buildNavigationRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, UiTheme.SPACING_SM, 2));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(2, UiTheme.SPACING_MD, 4, UiTheme.SPACING_MD)));

        for (JButton button : new JButton[]{
                prevPageButton, nextPageButton, zoomOutButton, zoomInButton, fitWidthButton, fitPageButton}) {
            button.setFont(button.getFont().deriveFont(11f));
            button.setMargin(new Insets(1, 7, 1, 7));
        }

        prevPageButton.addActionListener(e -> withIoErrorDialog(viewerPanel::prevPage, this::updatePageLabel));
        nextPageButton.addActionListener(e -> withIoErrorDialog(viewerPanel::nextPage, this::updatePageLabel));
        pageLabel.setForeground(UiTheme.TEXT_MUTED);
        pageLabel.setBorder(BorderFactory.createEmptyBorder(0, UiTheme.SPACING_SM, 0, UiTheme.SPACING_SM));

        zoomOutButton.setToolTipText("Zoom out (Ctrl+-)");
        zoomOutButton.addActionListener(e -> withIoErrorDialog(viewerPanel::zoomOut, () -> {
        }));
        zoomInButton.setToolTipText("Zoom in (Ctrl++)");
        zoomInButton.addActionListener(e -> withIoErrorDialog(viewerPanel::zoomIn, () -> {
        }));
        fitWidthButton.addActionListener(e -> withIoErrorDialog(viewerPanel::fitWidth, () -> {
        }));
        fitPageButton.addActionListener(e -> withIoErrorDialog(viewerPanel::fitPage, () -> {
        }));

        row.add(prevPageButton);
        row.add(nextPageButton);
        row.add(pageLabel);
        row.add(verticalDivider());
        row.add(zoomOutButton);
        row.add(zoomInButton);
        row.add(verticalDivider());
        row.add(fitWidthButton);
        row.add(fitPageButton);
        return row;
    }

    private static JSeparator verticalDivider() {
        JSeparator divider = new JSeparator(SwingConstants.VERTICAL);
        divider.setPreferredSize(new Dimension(1, 20));
        return divider;
    }

    /** Ctrl+/Ctrl- (and the numpad/= variants) zoom the page in/out, regardless of which component has focus. */
    private void installZoomShortcuts() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        bindShortcut(inputMap, "zoomIn",
                KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK));
        actionMap.put("zoomIn", actionOf(() -> withIoErrorDialog(viewerPanel::zoomIn, () -> {
        })));

        bindShortcut(inputMap, "zoomOut",
                KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK),
                KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK));
        actionMap.put("zoomOut", actionOf(() -> withIoErrorDialog(viewerPanel::zoomOut, () -> {
        })));
    }

    private static void bindShortcut(InputMap inputMap, String name, KeyStroke... keyStrokes) {
        for (KeyStroke keyStroke : keyStrokes) {
            inputMap.put(keyStroke, name);
        }
    }

    private static Action actionOf(Runnable action) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        };
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new GridBagLayout());
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBackground(Color.WHITE);
        sidebar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(UiTheme.SPACING_LG, UiTheme.SPACING_LG, UiTheme.SPACING_LG, UiTheme.SPACING_LG)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        int row = 0;

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, UiTheme.SPACING_MD, 0);
        JLabel heading = new JLabel("Sign this document");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 16f));
        sidebar.add(heading, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, UiTheme.SPACING_MD, 0);
        showSignatureCheckBox.setOpaque(false);
        showSignatureCheckBox.addActionListener(e -> onShowSignatureChanged());
        sidebar.add(showSignatureCheckBox, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(UiTheme.SPACING_SM, 0, UiTheme.SPACING_MD, 0);
        sidebar.add(new JSeparator(), gbc);

        row = addFieldGroup(sidebar, gbc, row, "Signer name", signerNameField);
        row = addFieldGroup(sidebar, gbc, row, "Purpose", purposeField);
        row = addFieldGroup(sidebar, gbc, row, "Contact", contactField);

        gbc.gridy = row;
        gbc.weighty = 1;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        sidebar.add(filler, gbc); // pushes everything above to the top
        return sidebar;
    }

    private static int addFieldGroup(JPanel sidebar, GridBagConstraints gbc, int row, String labelText,
                                     Component field) {
        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, 3, 0);
        JLabel label = new JLabel(labelText.toUpperCase());
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10.5f));
        label.setForeground(UiTheme.TEXT_MUTED);
        sidebar.add(label, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(0, 0, UiTheme.SPACING_MD, 0);
        sidebar.add(field, gbc);
        return row;
    }

    private JPanel buildLogPanel() {
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setBackground(Color.WHITE);
        logArea.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_MD, UiTheme.SPACING_SM, UiTheme.SPACING_MD));

        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setPreferredSize(new Dimension(0, 140));
        logScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JLabel title = new JLabel("ACTIVITY LOG");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 10.5f));
        title.setForeground(UiTheme.TEXT_MUTED);

        logToggleButton.setFont(logToggleButton.getFont().deriveFont(10.5f));
        logToggleButton.setFocusable(false);
        logToggleButton.setMargin(new Insets(0, 6, 0, 6));
        logToggleButton.addActionListener(e -> setLogExpanded(!logExpanded));
        updateLogToggleButton();

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setOpaque(true);
        titleBar.setBackground(Color.WHITE);
        titleBar.setBorder(BorderFactory.createEmptyBorder(UiTheme.SPACING_SM, UiTheme.SPACING_MD, UiTheme.SPACING_SM, UiTheme.SPACING_MD));
        titleBar.add(title, BorderLayout.WEST);
        titleBar.add(logToggleButton, BorderLayout.EAST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(titleBar, BorderLayout.NORTH);
        wrapper.add(logScrollPane, BorderLayout.CENTER);
        wrapper.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER));
        return wrapper;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                BorderFactory.createEmptyBorder(4, UiTheme.SPACING_MD, 4, UiTheme.SPACING_MD)));
        statusBarLabel.setFont(statusBarLabel.getFont().deriveFont(11f));
        statusBarLabel.setForeground(UiTheme.TEXT_MUTED);
        bar.add(statusBarLabel, BorderLayout.WEST);
        return bar;
    }

    private void setLogExpanded(boolean expanded) {
        logExpanded = expanded;
        logScrollPane.setVisible(expanded);
        updateLogToggleButton();
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void updateLogToggleButton() {
        logToggleButton.setText(logExpanded ? "Hide ▾" : "Show ▸");
    }

    private void prefillFromSettings() {
        signerNameField.setText(settings.getSignature().getDefaultSignerName());
        purposeField.setText(settings.getSignature().getDefaultPurpose());
        contactField.setText(settings.getSignature().getDefaultContact());
    }

    private void onShowSignatureChanged() {
        viewerPanel.setSelectionEnabled(showSignatureCheckBox.isSelected());
    }

    private void openPdf() {
        String lastOpenDir = settings.getUi().getLastOpenDir();
        File startDir = !lastOpenDir.isBlank() && new File(lastOpenDir).isDirectory()
                ? new File(lastOpenDir) : null;
        JFileChooser chooser = new JFileChooser(startDir);
        chooser.setFileFilter(new FileNameExtensionFilter("PDF files", "pdf"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = chooser.getSelectedFile();
        try {
            viewerPanel.open(file);
            viewerPanel.setSelectionEnabled(showSignatureCheckBox.isSelected());
            currentPdfFile = file;
            updatePageLabel();
            signButton.setEnabled(true);
            prevPageButton.setEnabled(true);
            nextPageButton.setEnabled(true);
            statusBarLabel.setText(file.getAbsolutePath());
            log("Opened " + file.getName() + " (" + viewerPanel.getPageCount() + " page(s))");
            rememberOpenDir(file.getParentFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to open PDF: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rememberOpenDir(File dir) {
        if (dir == null || dir.getAbsolutePath().equals(settings.getUi().getLastOpenDir())) {
            return;
        }
        settings.getUi().setLastOpenDir(dir.getAbsolutePath());
        try {
            SettingsLoader.save(settingsFile, settings);
        } catch (IOException e) {
            log("Could not save last-used folder to settings.yaml: " + e.getMessage());
        }
    }

    private void updatePageLabel() {
        pageLabel.setText(viewerPanel.getCurrentPageNumber() + " / " + viewerPanel.getPageCount());
    }

    private void openSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this, settings, settingsFile);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            settings = dialog.getSettings();
            prefillFromSettings();
            log("Settings saved to " + settingsFile.getAbsolutePath());
        }
    }

    private void signAndSave() {
        if (currentPdfFile == null) {
            return;
        }
        String signerName = signerNameField.getText().trim();
        if (signerName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Signer name is required.", "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SignatureFieldSpec resolvedFieldSpec = null;
        if (showSignatureCheckBox.isSelected()) {
            if (!viewerPanel.hasSelection()) {
                JOptionPane.showMessageDialog(this,
                        "Draw a rectangle on the page first, or uncheck \"Show signature\" to sign without "
                                + "a visible stamp.",
                        "Missing selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            float[] rect = viewerPanel.getSelectionPdfPoints();
            resolvedFieldSpec = SignatureFieldSpec.newField(
                    viewerPanel.getCurrentPageNumber(), rect[0], rect[1], rect[2], rect[3]);
        }
        SignatureFieldSpec fieldSpec = resolvedFieldSpec;

        SigningConfig config = buildSigningConfig();
        if (config == null) {
            return;
        }

        File outputFile = chooseOutputFile();
        if (outputFile == null) {
            return;
        }

        String purpose = purposeField.getText().trim();
        String contact = contactField.getText().trim();

        signButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            private Exception failure;

            @Override
            protected Void doInBackground() {
                try {
                    padesUtils.signTestFile(currentPdfFile, outputFile, config, fieldSpec, signerName, purpose, contact);
                    padesUtils.validateSignedFile(outputFile.getAbsolutePath());
                    padesUtils.validatePDFAStructure(outputFile.getAbsolutePath());
                } catch (Exception e) {
                    failure = e;
                }
                return null;
            }

            @Override
            protected void done() {
                signButton.setEnabled(true);
                if (failure != null) {
                    log("Signing failed: " + failure);
                    JOptionPane.showMessageDialog(MainFrame.this, "Signing failed: " + failure.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    log("Signed successfully: " + outputFile.getAbsolutePath());
                    JOptionPane.showMessageDialog(MainFrame.this, "Signed successfully:\n" + outputFile.getAbsolutePath(),
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    openInSystemViewer(outputFile);
                }
            }
        }.execute();
    }

    /** Best-effort: opens the freshly signed PDF in whatever the OS has registered as its default viewer. */
    private void openInSystemViewer(File file) {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            log("Could not open " + file.getName() + " automatically: no desktop file-opening support here.");
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            log("Could not open " + file.getName() + " automatically: " + e.getMessage());
        }
    }

    private SigningConfig buildSigningConfig() {
        Settings.Keystore keystore = settings.getKeystore();
        if (keystore.getPath().isBlank() || keystore.getKeyAlias().isBlank()) {
            JOptionPane.showMessageDialog(this, "Configure the keystore path and key alias in Settings first.",
                    "Missing configuration", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String keystorePassword = keystore.getPassword();
        String keyPassword = keystore.getKeyPassword();
        boolean needKeystorePassword = keystorePassword == null || keystorePassword.isBlank();
        boolean needKeyPassword = keyPassword == null || keyPassword.isBlank();

        if (needKeystorePassword || needKeyPassword) {
            PasswordPromptDialog dialog = new PasswordPromptDialog(this, needKeystorePassword, needKeyPassword);
            dialog.setVisible(true);
            if (!dialog.isConfirmed()) {
                return null;
            }
            if (needKeystorePassword) {
                keystorePassword = new String(dialog.getKeystorePassword());
            }
            if (needKeyPassword) {
                keyPassword = new String(dialog.getKeyPassword());
            }
        }

        return new SigningConfig()
                .setKeystoreFilepath(keystore.getPath())
                .setKeystoreType(keystore.getType())
                .setKeyAlias(keystore.getKeyAlias())
                .setKeystorePassword(keystorePassword)
                .setKeyPassword(keyPassword)
                .setSignatureImageFile(settings.getSignature().getImagePath())
                .setDateTimeFormat(settings.getSignature().getDateTimeFormat());
    }

    private File chooseOutputFile() {
        JFileChooser chooser = new JFileChooser(currentPdfFile.getParentFile());
        String base = currentPdfFile.getName().replaceFirst("(?i)\\.pdf$", "");
        chooser.setSelectedFile(new File(currentPdfFile.getParentFile(), base + settings.getOutput().getSuffix() + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws IOException;
    }

    private void withIoErrorDialog(IoAction action, Runnable onSuccess) {
        try {
            action.run();
            onSuccess.run();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
