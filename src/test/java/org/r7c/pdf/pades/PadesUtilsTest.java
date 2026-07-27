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
/*
 * sign-pdf
 *
 * Copyright (C) 2025 - 2026  Warpsource d.o.o.
 *
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
 */
package org.r7c.pdf.pades;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.r7c.pdf.pades.testutils.SelfSignedCertGenerator;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PadesUtilsTest {

    private static final String KEYSTORE_ALIAS = "test-signer";
    private static final char[] KEYSTORE_PASSWORD = "changeit".toCharArray();
    private static final char[] KEY_PASSWORD = "changeit".toCharArray();

    @TempDir
    Path tempDir;

    private X509Certificate signingCertificate;
    private File fileToBeSigned;
    private File signedFile;
    private SigningConfig config;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        signingCertificate = SelfSignedCertGenerator.generate(
                keyPair, "SHA256withRSA", "CN=Test Signer,O=Test Org,C=SI", 365);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry(KEYSTORE_ALIAS, keyPair.getPrivate(), KEY_PASSWORD,
                new X509Certificate[]{signingCertificate});
        File keystoreFile = tempDir.resolve("test-signer.p12").toFile();
        try (OutputStream os = new FileOutputStream(keystoreFile)) {
            keyStore.store(os, KEYSTORE_PASSWORD);
        }

        File imageFile = tempDir.resolve("signature.png").toFile();
        ImageIO.write(new BufferedImage(200, 80, BufferedImage.TYPE_INT_ARGB), "png", imageFile);

        config = new SigningConfig()
                .setKeystoreFilepath(keystoreFile.getAbsolutePath())
                .setKeystorePassword(new String(KEYSTORE_PASSWORD))
                .setKeystoreType("PKCS12")
                .setKeyAlias(KEYSTORE_ALIAS)
                .setKeyPassword(new String(KEY_PASSWORD))
                .setSignatureImageFile(imageFile.getAbsolutePath());

        fileToBeSigned = new File(getClass().getResource("/gridtest.pdf").toURI());
        signedFile = tempDir.resolve("gridtest-signed.pdf").toFile();
    }

    @Test
    void signTestFile_embedsCryptographicallyValidPAdESSignature() throws Exception {
        new PadesUtils().signTestFile(fileToBeSigned, signedFile, config,
                SignatureFieldSpec.newField(1, 50, 50, 200, 80),
                "Test Signer", "Testing", "test@example.org");

        assertTrue(signedFile.isFile());
        assertTrue(signedFile.length() > 0);

        byte[] signedBytes = Files.readAllBytes(signedFile.toPath());
        PDSignature signature;
        try (PDDocument document = Loader.loadPDF(signedBytes)) {
            signature = document.getLastSignatureDictionary();
        }
        assertNotNull(signature, "signed PDF must contain a signature dictionary");
        assertEquals("Testing", signature.getReason());
        assertEquals("test@example.org", signature.getContactInfo());

        verifyCryptographically(signedBytes, signature);
    }

    @Test
    void signTestFile_signsIntoExistingEmptySignatureField() throws Exception {
        // Pre-create an empty signature field on the PDF, as if it were a form template.
        PAdESService service = new PAdESService(new CommonCertificateVerifier());
        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
        fieldParameters.setFieldId("Signature1");
        fieldParameters.setPage(1);
        fieldParameters.setOriginX(50);
        fieldParameters.setOriginY(50);
        fieldParameters.setWidth(200);
        fieldParameters.setHeight(80);
        DSSDocument withEmptyField = service.addNewSignatureField(
                new FileDocument(fileToBeSigned), fieldParameters);
        File fileWithEmptyField = tempDir.resolve("gridtest-with-field.pdf").toFile();
        withEmptyField.save(fileWithEmptyField.getAbsolutePath());

        PadesUtils padesUtils = new PadesUtils();
        List<String> availableFields = padesUtils.listAvailableSignatureFields(fileWithEmptyField);
        assertEquals(List.of("Signature1"), availableFields);

        padesUtils.signTestFile(fileWithEmptyField, signedFile, config,
                SignatureFieldSpec.existingField("Signature1"),
                "Test Signer", "Testing", "test@example.org");

        byte[] signedBytes = Files.readAllBytes(signedFile.toPath());
        PDSignature signature;
        try (PDDocument document = Loader.loadPDF(signedBytes)) {
            signature = document.getLastSignatureDictionary();
        }
        assertNotNull(signature, "signed PDF must contain a signature dictionary");

        verifyCryptographically(signedBytes, signature);
        assertTrue(padesUtils.listAvailableSignatureFields(signedFile).isEmpty(),
                "the now-signed field must no longer be reported as available");
    }

    @Test
    void signTestFile_withNullFieldSpec_signsInvisibly() throws Exception {
        new PadesUtils().signTestFile(fileToBeSigned, signedFile, config,
                null, "Test Signer", "Testing", "test@example.org");

        assertTrue(signedFile.isFile());
        byte[] signedBytes = Files.readAllBytes(signedFile.toPath());
        PDSignature signature;
        try (PDDocument document = Loader.loadPDF(signedBytes)) {
            signature = document.getLastSignatureDictionary();
        }
        assertNotNull(signature, "signed PDF must still contain a signature dictionary");
        verifyCryptographically(signedBytes, signature);
    }

    @Test
    void listAvailableSignatureFields_isEmptyForPdfWithoutSignatureFields() throws Exception {
        assertTrue(new PadesUtils().listAvailableSignatureFields(fileToBeSigned).isEmpty());
    }

    @Test
    void validatePDFAStructure_reportsDeterministically() throws Exception {
        new PadesUtils().signTestFile(fileToBeSigned, signedFile, config,
                SignatureFieldSpec.newField(1, 50, 50, 200, 80),
                "Test Signer", "Testing", "test@example.org");

        PadesUtils padesUtils = new PadesUtils();
        boolean result = padesUtils.validatePDFAStructure(signedFile.getAbsolutePath());
        assertEquals(result, padesUtils.validatePDFAStructure(signedFile.getAbsolutePath()),
                "PDF/A structural validation must be deterministic across repeated runs");
    }

    @Test
    void renderSignatureText_substitutesOnlyPlaceholdersPresentInTemplate() {
        String rendered = PadesUtils.renderSignatureText("Signer: ${NAME}\nDate: ${DATETIME}",
                "Jane Doe", "27. 07. 2026 10:00", "CN=Issuer", "12345");

        assertEquals("Signer: Jane Doe\nDate: 27. 07. 2026 10:00", rendered);
    }

    @Test
    void renderSignatureText_blankTemplateFallsBackToDefault() {
        String rendered = PadesUtils.renderSignatureText("   ",
                "Jane Doe", "27. 07. 2026 10:00", "CN=Issuer", "12345");

        assertEquals("Signer: Jane Doe\nDatum: 27. 07. 2026 10:00\nCert. Izd.: CN=Issuer\nSer. st.: 12345",
                rendered);
    }

    private void verifyCryptographically(byte[] signedBytes, PDSignature signature) throws Exception {
        byte[] signedContent = signature.getSignedContent(signedBytes);
        byte[] cmsContents = signature.getContents(signedBytes);

        CMSSignedData cms = new CMSSignedData(new CMSProcessableByteArray(signedContent), cmsContents);
        SignerInformation signerInformation = cms.getSignerInfos().getSigners().iterator().next();
        SignerInformationVerifier verifier = new JcaSimpleSignerInfoVerifierBuilder().build(signingCertificate);

        assertTrue(signerInformation.verify(verifier),
                "PAdES signature must cryptographically verify against the self-signed signer certificate");
    }
}
