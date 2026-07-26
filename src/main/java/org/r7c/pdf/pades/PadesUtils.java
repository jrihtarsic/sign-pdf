package org.r7c.pdf.pades;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignerTextPosition;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.pades.*;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.spi.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;

import java.io.File;
import java.security.KeyStore;
import java.util.Date;

import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.ValidationResult.ValidationError;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.r7c.pdf.config.Settings;
import org.r7c.pdf.config.SettingsLoader;


/**
 *
 * @author Joze Rihtarsic
 * @since 0.1
 */
public class PadesUtils {

    public static void main(String... args) throws IOException {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");

        if (args.length < 10) {
            System.err.println("Usage: <fileToBeSigned> <fileSigned> signerName purpose contact page x y width height");
            System.err.println("Keystore/signature-image configuration is read from settings.yaml next to the jar; "
                    + "keystore/key passwords fall back to the KEYSTORE_PASSWORD/KEY_PASSWORD environment variables "
                    + "when settings.yaml leaves them blank.");
            return;
        }

        String fileToBeSigned = args[0];
        String fileSigned = args[1];
        String signerName = args[2];
        String purpose = args[3];
        String contact = args[4];
        int page = Integer.parseInt(args[5]);
        int x = Integer.parseInt(args[6]);
        int y = Integer.parseInt(args[7]);
        int w = Integer.parseInt(args[8]);
        int h = Integer.parseInt(args[9]);

        Settings settings = SettingsLoader.load(SettingsLoader.defaultSettingsFile());
        SigningConfig config = new SigningConfig()
                .setKeystoreFilepath(settings.getKeystore().getPath())
                .setKeystoreType(settings.getKeystore().getType())
                .setKeyAlias(settings.getKeystore().getKeyAlias())
                .setSignatureImageFile(settings.getSignature().getImagePath())
                .setDateTimeFormat(settings.getSignature().getDateTimeFormat());

        String keystorePassword = settings.getKeystore().getPassword();
        if (keystorePassword == null || keystorePassword.isBlank()) {
            keystorePassword = System.getenv("KEYSTORE_PASSWORD");
        }
        String keyPassword = settings.getKeystore().getKeyPassword();
        if (keyPassword == null || keyPassword.isBlank()) {
            keyPassword = System.getenv("KEY_PASSWORD");
        }
        if (keystorePassword == null || keyPassword == null) {
            System.err.println("Keystore/key password not set in settings.yaml; "
                    + "set KEYSTORE_PASSWORD and KEY_PASSWORD environment variables.");
            return;
        }
        config.setKeystorePassword(keystorePassword).setKeyPassword(keyPassword);

        File fSigned = new File(fileSigned);
        if (fSigned.exists()) {
            fSigned.delete();
        }

        PadesUtils padesUtils = new PadesUtils();
        System.out.println("Sign test file");
        padesUtils.signTestFile(new File(fileToBeSigned), fSigned, config,
                SignatureFieldSpec.newField(page, x, y, w, h),
                signerName, purpose, contact);

        System.out.println("Test signature");
        padesUtils.validateSignedFile(fileSigned);

        System.out.println("Validate pdf/a: signed file");
        padesUtils.validatePDFAStructure(fileSigned);
    }


    public void signTestFile(File fToBeSigned, File fSigned,
                              SigningConfig config,
                              SignatureFieldSpec fieldSpec,
                              String signerName,
                              String purpose,
                              String contact) throws IOException {

        SimpleDateFormat msdf = new SimpleDateFormat(config.getDateTimeFormat());
        KeyStore.PasswordProtection pswdKeystore = new KeyStore.PasswordProtection(config.getKeystorePassword().toCharArray());
        KeyStore.PasswordProtection pswdKey = new KeyStore.PasswordProtection(config.getKeyPassword().toCharArray());

        // -------------------------------
        // document to be signed

        PDFParser pdfParser = new PDFParser(new RandomAccessReadBufferedFile(fToBeSigned));
        ByteArrayOutputStream baos;
        try (PDDocument document = pdfParser.parse()) {
            // lock acroForm
            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm != null) {
                for (PDField field : acroForm.getFields()) {
                    field.setReadOnly(true);   // Lock the field
                }
            }

            baos = new ByteArrayOutputStream();
            document.save(baos);
        }
        InMemoryDocument documentToSign = new InMemoryDocument(baos.toByteArray());


        // signature key
        KeyStoreSignatureTokenConnection token;
        if ("JKS".equals(config.getKeystoreType())) {
            token = new JKSSignatureToken(config.getKeystoreFilepath(), pswdKeystore);
        } else {
            token = new Pkcs12SignatureToken(config.getKeystoreFilepath(), pswdKeystore);
        }

        DSSPrivateKeyEntry signatureKey = token.getKey(config.getKeyAlias(), pswdKey);

        Date signDate = Calendar.getInstance().getTime();
        // -------------------------------
        // create signature parameters
        PAdESSignatureParameters signatureParameters = new PAdESSignatureParameters();
        signatureParameters.bLevel().setSigningDate(signDate);
        signatureParameters.setSigningCertificate(signatureKey.getCertificate());
        signatureParameters.setCertificateChain(signatureKey.getCertificateChain());
        signatureParameters.setSignatureLevel(SignatureLevel.PAdES_BASELINE_B);
        signatureParameters.setLocation("EU");
        signatureParameters.setReason(purpose);
        signatureParameters.setContactInfo(contact);

        // -------------------------------
        // set signature image

        SignatureImageParameters imageParameters = new SignatureImageParameters();
        signatureParameters.setImageParameters(imageParameters);

        imageParameters.setImage(new FileDocument(new File(config.getSignatureImageFile())));


        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();
        if (fieldSpec.isExistingField()) {
            fieldParameters.setFieldId(fieldSpec.getExistingFieldId());
        } else {
            fieldParameters.setOriginX(fieldSpec.getOriginX());
            fieldParameters.setOriginY(fieldSpec.getOriginY());
            fieldParameters.setWidth(fieldSpec.getWidth());
            fieldParameters.setHeight(fieldSpec.getHeight());
            fieldParameters.setPage(fieldSpec.getPage());
        }
        imageParameters.setFieldParameters(fieldParameters);


        // ---------------------------

        // set signature text
        SignatureImageTextParameters textParameters = new SignatureImageTextParameters();
        textParameters.setText(String.format("Signer: %s\nDatum: %s\nCert. Izd.: %s\nSer. st.: %s",
                signerName,
                msdf.format(signDate),
                signatureKey.getCertificate().getIssuerX500Principal().toString(),
                signatureKey.getCertificate().getSerialNumber().toString()
                ));
        textParameters.setTextColor(Color.black);
        textParameters.setFont(new DSSJavaFont(Font.SANS_SERIF,Font.PLAIN, 6));
        textParameters.setSignerTextPosition(SignerTextPosition.TOP);
        imageParameters.setTextParameters(textParameters);

        // create signature service
        PAdESService service = new PAdESService(new CommonCertificateVerifier());
        ToBeSigned dataToSign = service.getDataToSign(documentToSign, signatureParameters);

        SignatureValue signatureValue = token.sign(dataToSign,
                signatureParameters.getDigestAlgorithm(),
                signatureKey);

        DSSDocument signedDocument = service.signDocument(documentToSign, signatureParameters, signatureValue);
        signedDocument.save(fSigned.getAbsolutePath());

    }

    /**
     * Lists the names of signature fields already present on the PDF that are not yet signed
     * (i.e. eligible targets for {@link SignatureFieldSpec#existingField(String)}).
     */
    public List<String> listAvailableSignatureFields(File file) throws IOException {
        PAdESService service = new PAdESService(new CommonCertificateVerifier());
        return service.getAvailableSignatureFields(new FileDocument(file));
    }

    public void validateSignedFile(String file) throws IOException {

        SignedDocumentValidator validator = SignedDocumentValidator
                .fromDocument(new FileDocument(new File(file)));
        validator.setCertificateVerifier(new CommonCertificateVerifier());
        Reports reports = validator.validateDocument();
        DiagnosticData diagnosticData = reports.getDiagnosticData();
        diagnosticData.getAllSignatures().forEach((sig) -> {
            System.out.println("sig : " + sig.getId() + " is valid: "
                    + (diagnosticData.isBLevelTechnicallyValid(sig.getId()) ? "true" : false));
        });

    }

    public boolean validatePDFAStructure(String file) throws IOException {

        File f = new File(file);
        PreflightParser parser = new PreflightParser(f);
        try (PreflightDocument preflightDocument = (PreflightDocument) parser.parse()){
            ValidationResult result = preflightDocument.validate();;
            List<ValidationError> errorsList = result.getErrorsList();
            errorsList.forEach((validationError) -> {
                System.out.println("validationError: " + validationError.getDetails());
            });
            return result.isValid();
        }
    }

}
