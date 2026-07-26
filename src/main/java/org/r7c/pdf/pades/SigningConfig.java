package org.r7c.pdf.pades;

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
}
