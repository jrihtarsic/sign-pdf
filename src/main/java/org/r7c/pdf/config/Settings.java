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

/**
 * Maps to {@code settings.yaml}: keystore/key location, default signature appearance, and output
 * naming. Loaded/saved by {@link SettingsLoader}.
 */
public class Settings {

    private Keystore keystore = new Keystore();
    private Signature signature = new Signature();
    private Output output = new Output();
    private Ui ui = new Ui();

    public Keystore getKeystore() {
        return keystore;
    }

    public void setKeystore(Keystore keystore) {
        this.keystore = keystore;
    }

    public Signature getSignature() {
        return signature;
    }

    public void setSignature(Signature signature) {
        this.signature = signature;
    }

    public Output getOutput() {
        return output;
    }

    public void setOutput(Output output) {
        this.output = output;
    }

    public Ui getUi() {
        return ui;
    }

    public void setUi(Ui ui) {
        this.ui = ui;
    }

    public static class Keystore {
        private String path = "";
        private String type = "PKCS12";
        private String keyAlias = "";
        /** Optional, plaintext. Left blank means: prompt for it in the UI at sign time. */
        private String password = "";
        /** Optional, plaintext. Left blank means: prompt for it in the UI at sign time. */
        private String keyPassword = "";

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getKeyAlias() {
            return keyAlias;
        }

        public void setKeyAlias(String keyAlias) {
            this.keyAlias = keyAlias;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getKeyPassword() {
            return keyPassword;
        }

        public void setKeyPassword(String keyPassword) {
            this.keyPassword = keyPassword;
        }
    }

    public static class Signature {
        /**
         * Default visible-signature text, using named placeholders {@code ${NAME}}, {@code ${DATETIME}},
         * {@code ${ISSUER}}, {@code ${SERIAL}}. Any placeholder omitted from a custom template is simply
         * not written; unknown {@code ${...}} tokens are left as-is.
         */
        public static final String DEFAULT_TEXT_TEMPLATE =
                "Signer: ${NAME}\nDatum: ${DATETIME}\nCert. Izd.: ${ISSUER}\nSer. st.: ${SERIAL}";

        private String imagePath = "";
        private String dateTimeFormat = "dd. MM. yyyy HH:mm";
        private String textTemplate = DEFAULT_TEXT_TEMPLATE;
        private String defaultSignerName = "";
        private String defaultPurpose = "";
        private String defaultContact = "";

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getDateTimeFormat() {
            return dateTimeFormat;
        }

        public void setDateTimeFormat(String dateTimeFormat) {
            this.dateTimeFormat = dateTimeFormat;
        }

        /**
         * Blank/null means: fall back to {@link #DEFAULT_TEXT_TEMPLATE}.
         */
        public String getTextTemplate() {
            return textTemplate;
        }

        public void setTextTemplate(String textTemplate) {
            this.textTemplate = textTemplate;
        }

        public String getDefaultSignerName() {
            return defaultSignerName;
        }

        public void setDefaultSignerName(String defaultSignerName) {
            this.defaultSignerName = defaultSignerName;
        }

        public String getDefaultPurpose() {
            return defaultPurpose;
        }

        public void setDefaultPurpose(String defaultPurpose) {
            this.defaultPurpose = defaultPurpose;
        }

        public String getDefaultContact() {
            return defaultContact;
        }

        public void setDefaultContact(String defaultContact) {
            this.defaultContact = defaultContact;
        }
    }

    public static class Output {
        private String suffix = "-signed";

        public String getSuffix() {
            return suffix;
        }

        public void setSuffix(String suffix) {
            this.suffix = suffix;
        }
    }

    /** UI state persisted purely for convenience, not user-facing in the Settings dialog. */
    public static class Ui {
        /** Folder the "Open PDF..." dialog last opened a file from; blank means use the JVM default. */
        private String lastOpenDir = "";

        public String getLastOpenDir() {
            return lastOpenDir;
        }

        public void setLastOpenDir(String lastOpenDir) {
            this.lastOpenDir = lastOpenDir;
        }
    }
}
