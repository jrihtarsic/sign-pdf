/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.r7c.pdf.pades;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignerTextPosition;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.pades.*;
import eu.europa.esig.dss.pades.signature.PAdESService;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Date;

import eu.europa.esig.dss.token.JKSSignatureToken;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.token.Pkcs12SignatureToken;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import java.awt.Color;
import java.awt.Font;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.ValidationResult;
import org.apache.pdfbox.preflight.ValidationResult.ValidationError;
import org.apache.pdfbox.preflight.parser.PreflightParser;
import org.apache.pdfbox.preflight.utils.ByteArrayDataSource;

/**
 *
 * @author Joze Rihtarsic
 */
public class PadesUtils {

    public static String FILE_TO_BE_SIGNED = "";
    public static String FILE_SIGNED = "";
    public static String KEYSTORE_FILEPATH = "";
    public static String KEYSTORE_PASWORD = "";
    public static String KEYSTORE_TYPE = "PKCS12";
    public static String SIG_KEY_ALIAS = "";
    public static String SIG_KEY_PASSWD = "";
    public static String SIG_IMAGE_FILE = "";
    public static String SIG_DATETIME_FORMAT = "dd. MM. yyyy HH:mm";

    SimpleDateFormat msdf = new SimpleDateFormat(SIG_DATETIME_FORMAT);

    public static void main(String... args) throws IOException {
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");
        PadesUtils test = new PadesUtils();
        System.out.println("Sign test file");
        File fSigned = new File(FILE_SIGNED);

        if (fSigned.exists()) {
            fSigned.delete();
        }

        String signerName = args[0];
        String purpose = args[1];
        String contact = args[2];
        int page = Integer.parseInt(args[3]);
        int x = Integer.parseInt(args[4]);
        int y = Integer.parseInt(args[5]);
        int w = Integer.parseInt(args[6]);
        int h = Integer.parseInt(args[7]);

        test.signTestFile(new File(FILE_TO_BE_SIGNED), fSigned,
                page,x,y,w,h,
                signerName,
                purpose,
                contact);

        // validate signature
        System.out.println("Test signature");
        test.validateSignedFile(FILE_SIGNED);
        // validate init document
        System.out.println("Validate pdf/a: signed file");
        test.validatePDFAStructure(FILE_SIGNED);

    }


    public void signTestFile(File fToBeSigned, File fSigned,
                             int page, int x, int y, int widh, int height,
                             String signerName,
                             String purpose,
                             String contact ) throws IOException {

        KeyStore.PasswordProtection pswdKeystore = new KeyStore.PasswordProtection(KEYSTORE_PASWORD.toCharArray());
        KeyStore.PasswordProtection pswdKey = new KeyStore.PasswordProtection(SIG_KEY_PASSWD.toCharArray());

        // -------------------------------
        // document to be signed


        PDDocument document = PDDocument.load(fToBeSigned);
        // lock acroForm
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm != null) {
            for (PDField field : acroForm.getFields()) {
                field.setReadOnly(true);   // Lock the field
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        document.save(baos);
        InMemoryDocument documentToSign = new InMemoryDocument(baos.toByteArray());


        // signature key
        KeyStoreSignatureTokenConnection token;
        if (KEYSTORE_TYPE.equals("JKS")) {
            token = new JKSSignatureToken(KEYSTORE_FILEPATH, pswdKeystore);
        } else {
            token = new Pkcs12SignatureToken(KEYSTORE_FILEPATH, pswdKeystore);
        }

        DSSPrivateKeyEntry signatureKey = token.getKey(SIG_KEY_ALIAS, pswdKey);

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

        imageParameters.setImage(new FileDocument(new File(SIG_IMAGE_FILE)));


        SignatureFieldParameters fieldParameters = new SignatureFieldParameters();

        fieldParameters.setOriginX(x);
        fieldParameters.setOriginY(y);
        fieldParameters.setWidth(widh);
        fieldParameters.setHeight(height);
        fieldParameters.setPage(page);
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

        SignatureValue signatureValue = jksToken.sign(dataToSign,
                signatureParameters.getDigestAlgorithm(),
                signatureKey);

        DSSDocument signedDocument = service.signDocument(documentToSign, signatureParameters, signatureValue);
        signedDocument.save(fSigned.getAbsolutePath());

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

        DSSDocument doc = new FileDocument(new File(file));

        try (InputStream is = doc.openStream()) {
            PreflightParser parser = new PreflightParser(new ByteArrayDataSource(is));
            parser.parse();
            PreflightDocument preflightDocument = parser.getPreflightDocument();
            preflightDocument.validate();
            ValidationResult result = preflightDocument.getResult();
            List<ValidationError> errorsList = result.getErrorsList();
            errorsList.forEach((validationError) -> {
                System.out.println("validationError: " + validationError.getDetails());
            });
            return result.isValid();
        }
    }

}
