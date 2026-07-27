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
package org.r7c.pdf.pades;

import org.r7c.pdf.config.Settings;

/**
 * Keystore, signing key, and visible-signature-appearance configuration needed to sign a PDF.
 * Replaces the static mutable fields {@code PadesUtils} used to expose for this purpose.
 */
public class SigningConfig {

    private String keystoreFilepath = "";
    private String keystorePassword = "";
    private String keystoreType = "PKCS12";
    private String keyAlias = "";
    private String keyPassword = "";
    private String signatureImageFile = "";
    private String dateTimeFormat = "dd. MM. yyyy HH:mm";
    private String signatureTextTemplate = Settings.Signature.DEFAULT_TEXT_TEMPLATE;

    public String getKeystoreFilepath() {
        return keystoreFilepath;
    }

    public SigningConfig setKeystoreFilepath(String keystoreFilepath) {
        this.keystoreFilepath = keystoreFilepath;
        return this;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public SigningConfig setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public SigningConfig setKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
        return this;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public SigningConfig setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
        return this;
    }

    public String getKeyPassword() {
        return keyPassword;
    }

    public SigningConfig setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public String getSignatureImageFile() {
        return signatureImageFile;
    }

    public SigningConfig setSignatureImageFile(String signatureImageFile) {
        this.signatureImageFile = signatureImageFile;
        return this;
    }

    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    public SigningConfig setDateTimeFormat(String dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
        return this;
    }

    /**
     * Visible-signature text template; blank/null is treated as
     * {@link Settings.Signature#DEFAULT_TEXT_TEMPLATE} at signing time.
     */
    public String getSignatureTextTemplate() {
        return signatureTextTemplate;
    }

    public SigningConfig setSignatureTextTemplate(String signatureTextTemplate) {
        this.signatureTextTemplate = signatureTextTemplate;
        return this;
    }
}
