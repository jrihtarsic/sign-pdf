package org.r7c.pdf.pades.testutils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Generates minimal self-signed X.509 v3 certificates using only public JDK APIs.
 *
 * <p>The certificate's DER structure is constructed directly from ASN.1/DER primitives
 * and then parsed using CertificateFactory. No BouncyCastle, no sun.security.* internals,
 * and no --add-opens flags are required.
 * This class is designed to eliminate the need for storing test certificates in a keystore
 * or truststore. Instead, the certificates are generated dynamically during test execution.
 * </p>
 * <h3>Supported signature algorithms</h3>
 * <ul>
 *   <li>RSA — {@code SHA256withRSA}, {@code SHA384withRSA}, {@code SHA512withRSA}</li>
 *   <li>ECDSA — {@code SHA256withECDSA}, {@code SHA384withECDSA}, {@code SHA512withECDSA}</li>
 *   <li>EdDSA — {@code Ed25519}, {@code Ed448} (requires Java 15+)</li>
 * </ul>
 *
 * <h3>Supported DN attributes</h3>
 * <ul>
 *   <li>{@code CN} — commonName (UTF8String)</li>
 *   <li>{@code C}  — countryName (PrintableString, two-letter ISO 3166 code)</li>
 *   <li>{@code O}  — organizationName (UTF8String)</li>
 *   <li>{@code OU} — organizationalUnitName (UTF8String)</li>
 * </ul>
 *
 * <h3>Limitations</h3>
 * <ul>
 *   <li>No X.509 extensions are added (basic-constraints, key-usage, etc.).</li>
 *   <li>Validity dates use UTCTime, which covers years 2000–2049.</li>
 * </ul>
 *
 * <p>These are acceptable constraints for unit and integration tests.
 */
public final class SelfSignedCertGenerator {

    private SelfSignedCertGenerator() {
    }

    // -------------------------------------------------------------------------
    // ASN.1 universal tag constants (ITU-T X.690)
    // -------------------------------------------------------------------------

    private static final int TAG_INTEGER          = 0x02;
    private static final int TAG_BIT_STRING       = 0x03;
    private static final int TAG_OID              = 0x06;
    private static final int TAG_UTF8_STRING      = 0x0C;
    private static final int TAG_PRINTABLE_STRING = 0x13;
    private static final int TAG_UTC_TIME         = 0x17;
    private static final int TAG_SEQUENCE         = 0x30;
    private static final int TAG_SET              = 0x31;
    /** Context-specific constructed [0] tag — used for the TBSCertificate version field. */
    private static final int TAG_CONTEXT_0        = 0xA0;

    // -------------------------------------------------------------------------
    // Pre-built DER encoding constants
    // -------------------------------------------------------------------------

    /** DER encoding of ASN.1 NULL (05 00). */
    private static final byte[] DER_NULL = {0x05, 0x00};

    /**
     * DER encoding of TBSCertificate {@code version} field set to v3 (INTEGER value 2)
     * wrapped in an [0] EXPLICIT context tag.
     */
    private static final byte[] TBS_VERSION_V3 = {
        (byte) TAG_CONTEXT_0, 0x03, (byte) TAG_INTEGER, 0x01, 0x02
    };

    // -------------------------------------------------------------------------
    // Signature algorithm OID strings
    // -------------------------------------------------------------------------

    /** SHA-256 with RSA Encryption — RFC 4055, OID 1.2.840.113549.1.1.11 */
    private static final String OID_SHA256_WITH_RSA        = "1.2.840.113549.1.1.11";
    /** SHA-384 with RSA Encryption — RFC 4055, OID 1.2.840.113549.1.1.12 */
    private static final String OID_SHA384_WITH_RSA        = "1.2.840.113549.1.1.12";
    /** SHA-512 with RSA Encryption — RFC 4055, OID 1.2.840.113549.1.1.13 */
    private static final String OID_SHA512_WITH_RSA        = "1.2.840.113549.1.1.13";
    /** ECDSA with SHA-256 — RFC 5758, OID 1.2.840.10045.4.3.2 */
    private static final String OID_SHA256_WITH_ECDSA      = "1.2.840.10045.4.3.2";
    /** ECDSA with SHA-384 — RFC 5758, OID 1.2.840.10045.4.3.3 */
    private static final String OID_SHA384_WITH_ECDSA      = "1.2.840.10045.4.3.3";
    /** ECDSA with SHA-512 — RFC 5758, OID 1.2.840.10045.4.3.4 */
    private static final String OID_SHA512_WITH_ECDSA      = "1.2.840.10045.4.3.4";
    /** Ed25519 — RFC 8410, OID 1.3.101.112 */
    private static final String OID_ED25519                = "1.3.101.112";
    /** Ed448 — RFC 8410, OID 1.3.101.113 */
    private static final String OID_ED448                  = "1.3.101.113";

    // -------------------------------------------------------------------------
    // X.500 attribute type OID strings (RFC 4519)
    // -------------------------------------------------------------------------

    /** commonName — OID 2.5.4.3 */
    private static final String OID_COMMON_NAME              = "2.5.4.3";
    /** countryName — OID 2.5.4.6 */
    private static final String OID_COUNTRY_NAME             = "2.5.4.6";
    /** organizationName — OID 2.5.4.10 */
    private static final String OID_ORGANIZATION_NAME        = "2.5.4.10";
    /** organizationalUnitName — OID 2.5.4.11 */
    private static final String OID_ORGANIZATIONAL_UNIT_NAME = "2.5.4.11";

    // -------------------------------------------------------------------------
    // Pre-encoded DER OID bytes for RDN attribute types
    // -------------------------------------------------------------------------

    private static final byte[] OID_BYTES_CN = encodeOid(OID_COMMON_NAME);
    private static final byte[] OID_BYTES_C  = encodeOid(OID_COUNTRY_NAME);
    private static final byte[] OID_BYTES_O  = encodeOid(OID_ORGANIZATION_NAME);
    private static final byte[] OID_BYTES_OU = encodeOid(OID_ORGANIZATIONAL_UNIT_NAME);

    /**
     * Pre-encoded DER bytes for the {@code AlgorithmIdentifier} of each supported
     * signature algorithm.  Values are constant per the relevant RFCs; they do not
     * depend on the key size or curve, only on the algorithm name.
     *
     * <p>RSA algorithms include a trailing {@code NULL} parameters element (RFC 4055 §3.2).
     * ECDSA and EdDSA algorithms omit parameters entirely (RFC 5758, RFC 8410).
     */
    private static final Map<String, byte[]> ALG_IDS = Map.of(
            "SHA256withRSA",   encodeAlgorithmIdentifier(OID_SHA256_WITH_RSA),
            "SHA384withRSA",   encodeAlgorithmIdentifier(OID_SHA384_WITH_RSA),
            "SHA512withRSA",   encodeAlgorithmIdentifier(OID_SHA512_WITH_RSA),
            "SHA256withECDSA", encodeAlgorithmIdentifier(OID_SHA256_WITH_ECDSA),
            "SHA384withECDSA", encodeAlgorithmIdentifier(OID_SHA384_WITH_ECDSA),
            "SHA512withECDSA", encodeAlgorithmIdentifier(OID_SHA512_WITH_ECDSA),
            "Ed25519",         encodeAlgorithmIdentifier(OID_ED25519),
            "Ed448",           encodeAlgorithmIdentifier(OID_ED448));

    /**
     * Generates a self-signed X.509 v3 certificate.
     *
     * @param keyPair            the key pair to certify; the private key signs the TBS structure
     *                           and the public key is embedded in SubjectPublicKeyInfo
     * @param signatureAlgorithm JCA algorithm name, e.g. {@code "SHA256withRSA"} or {@code "Ed25519"}
     * @param subjectDN          distinguished name with supported attributes: CN, C, O, OU,
     *                           e.g. {@code "CN=Test Certificate,O=Acme,C=US"}
     * @param validityDays       number of days the certificate is valid, starting from now
     * @return the signed X.509 certificate
     * @throws IllegalArgumentException if {@code signatureAlgorithm} is not in the supported set
     */
    public static X509Certificate generate(KeyPair keyPair,
                                           String signatureAlgorithm,
                                           String subjectDN,
                                           int validityDays) throws Exception {
        byte[] algId = ALG_IDS.get(signatureAlgorithm);
        if (algId == null) {
            throw new IllegalArgumentException(
                    "Unsupported signature algorithm: " + signatureAlgorithm
                            + ". Supported: " + ALG_IDS.keySet());
        }

        // publicKey.getEncoded() returns the SubjectPublicKeyInfo in X.509/DER format.
        byte[] spki = keyPair.getPublic().getEncoded();
        byte[] name = encodeName(subjectDN);
        byte[] tbs = buildTbs(algId, name, spki, validityDays);

        Signature signer = Signature.getInstance(signatureAlgorithm);
        signer.initSign(keyPair.getPrivate());
        signer.update(tbs);
        byte[] sigBytes = signer.sign();

        // Certificate ::= SEQUENCE { TBSCertificate, AlgorithmIdentifier, BIT STRING }
        byte[] certDer = seq(cat(tbs, algId, bitString(sigBytes)));

        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(certDer));
    }

    // -------------------------------------------------------------------------
    // TBSCertificate builder
    // -------------------------------------------------------------------------

    /**
     * Builds the DER-encoded TBSCertificate.
     *
     * <pre>
     * TBSCertificate ::= SEQUENCE {
     *   version         [0] EXPLICIT INTEGER DEFAULT v1,
     *   serialNumber    INTEGER,
     *   signature       AlgorithmIdentifier,
     *   issuer          Name,
     *   validity        Validity,
     *   subject         Name,
     *   subjectPublicKeyInfo SubjectPublicKeyInfo
     * }
     * </pre>
     */
    private static byte[] buildTbs(byte[] algId, byte[] name,
                                   byte[] spki, int validityDays) {
        // Serial: milliseconds since epoch — unique enough for test certs
        byte[] serial = integer(BigInteger.valueOf(System.currentTimeMillis()));
        byte[] validity = buildValidity(validityDays);
        // issuer == subject for self-signed
        return seq(cat(TBS_VERSION_V3, serial, algId, name, validity, name, spki));
    }

    private static byte[] buildValidity(int validityDays) {
        Instant notBefore = Instant.now();
        Instant notAfter = notBefore.plusSeconds(validityDays * 86_400L);
        return seq(cat(utcTime(notBefore), utcTime(notAfter)));
    }

    // -------------------------------------------------------------------------
    // DN encoding — CN, C, O, OU attributes (RFC 4519)
    // -------------------------------------------------------------------------

    /**
     * Encodes a Name from a comma-separated DN string.
     *
     * <p>Each recognised attribute ({@code CN}, {@code C}, {@code O}, {@code OU}) is encoded
     * as a single-valued RDN (a SET containing one AttributeTypeAndValue SEQUENCE).
     * Unknown attributes are silently skipped.  If no attributes are recognised the entire
     * string is treated as a CN value.
     *
     * <pre>
     * Name            ::= SEQUENCE OF RelativeDistinguishedName
     * RelativeDistinguishedName ::= SET OF AttributeTypeAndValue
     * AttributeTypeAndValue     ::= SEQUENCE { type OBJECT IDENTIFIER, value ANY }
     * </pre>
     */
    private static byte[] encodeName(String dn) {
        ByteArrayOutputStream rdns = new ByteArrayOutputStream();
        for (String part : dn.split(",")) {
            String trimmed = part.strip();
            int eq = trimmed.indexOf('=');
            if (eq < 0) continue;
            String key = trimmed.substring(0, eq).strip().toUpperCase();
            String val = trimmed.substring(eq + 1).strip();
            byte[] oidBytes;
            byte[] valueBytes;
            switch (key) {
                case "CN":
                    oidBytes   = OID_BYTES_CN;
                    valueBytes = tlv(TAG_UTF8_STRING, val.getBytes(StandardCharsets.UTF_8));
                    break;
                case "C":
                    oidBytes   = OID_BYTES_C;
                    // countryName uses PrintableString; ISO 3166-1 alpha-2 codes are ASCII
                    valueBytes = tlv(TAG_PRINTABLE_STRING, val.getBytes(StandardCharsets.US_ASCII));
                    break;
                case "O":
                    oidBytes   = OID_BYTES_O;
                    valueBytes = tlv(TAG_UTF8_STRING, val.getBytes(StandardCharsets.UTF_8));
                    break;
                case "OU":
                    oidBytes   = OID_BYTES_OU;
                    valueBytes = tlv(TAG_UTF8_STRING, val.getBytes(StandardCharsets.UTF_8));
                    break;
                default:
                    continue; // unsupported attribute — skip
            }
            byte[] rdn = set(seq(cat(oidBytes, valueBytes)));
            rdns.write(rdn, 0, rdn.length);
        }
        if (rdns.size() == 0) {
            // fallback: treat the whole string as a CN value
            byte[] cnValue = tlv(TAG_UTF8_STRING, dn.getBytes(StandardCharsets.UTF_8));
            byte[] rdn = set(seq(cat(OID_BYTES_CN, cnValue)));
            rdns.write(rdn, 0, rdn.length);
        }
        return seq(rdns.toByteArray());
    }

    // -------------------------------------------------------------------------
    // DER / ASN.1 primitives
    // -------------------------------------------------------------------------

    private static byte[] seq(byte[] content) {
        return tlv(TAG_SEQUENCE, content);
    }

    private static byte[] set(byte[] content) {
        return tlv(TAG_SET, content);
    }

    private static byte[] integer(BigInteger value) {
        // toByteArray() produces two's-complement big-endian; positive integers may
        // have a leading 0x00 byte if the MSB would otherwise be set — that is correct
        // DER INTEGER encoding for a non-negative number.
        return tlv(TAG_INTEGER, value.toByteArray());
    }

    private static byte[] bitString(byte[] value) {
        return tlv(TAG_BIT_STRING, cat(new byte[]{0x00}, value)); // 0x00 = zero unused bits
    }

    // UTCTime covers 2000–2049 (yy < 50 → 20yy).  Sufficient for short-lived test certs.
    private static final DateTimeFormatter UTC_TIME_FMT =
            DateTimeFormatter.ofPattern("yyMMddHHmmss'Z'").withZone(ZoneOffset.UTC);

    private static byte[] utcTime(Instant instant) {
        return tlv(TAG_UTC_TIME, UTC_TIME_FMT.format(instant).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Encodes a DER TLV (Tag–Length–Value) triplet.
     * Lengths up to 65535 bytes are supported; that is sufficient for all key types
     * used in practice.
     */
    private static byte[] tlv(int tag, byte[] value) {
        int len = value.length;
        byte[] lenBytes;
        if (len < 128) {
            lenBytes = new byte[]{(byte) len};
        } else if (len < 256) {
            lenBytes = new byte[]{(byte) 0x81, (byte) len};
        } else {
            lenBytes = new byte[]{(byte) 0x82, (byte) (len >> 8), (byte) (len & 0xFF)};
        }
        byte[] out = new byte[1 + lenBytes.length + len];
        out[0] = (byte) tag;
        System.arraycopy(lenBytes, 0, out, 1, lenBytes.length);
        System.arraycopy(value, 0, out, 1 + lenBytes.length, len);
        return out;
    }

    /**
     * Concatenates byte arrays.
     */
    private static byte[] cat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) {
            total += p.length;
        }
        byte[] buf = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, buf, pos, p.length);
            pos += p.length;
        }
        return buf;
    }

    public static byte[] encodeAlgorithmIdentifier(String oid) {
        // RFC 8410 §6: all OIDs under arc 1.3.101 (X25519, X448, Ed25519, Ed448) MUST omit parameters.
        // RFC 4055 §3.2: RSA signature algorithms MUST include a NULL parameters element.
        byte[] params = oid.startsWith("1.3.101.") ? new byte[0] : DER_NULL;
        return seq(cat(encodeOid(oid), params));
    }

    public static byte[] encodeOid(String oid) {
        String[] parts = oid.split("\\.");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(40 * Integer.parseInt(parts[0]) + Integer.parseInt(parts[1]));
        for (int i = 2; i < parts.length; i++) {
            byte[] arc = encodeBase128(Long.parseLong(parts[i]));
            body.write(arc, 0, arc.length);
        }
        return tlv(TAG_OID, body.toByteArray());
    }

    private static byte[] encodeBase128(long value) {
        byte[] stack = new byte[10];
        int count = 0;
        do {
            stack[count++] = (byte) (value & 0x7F);
            value >>= 7;
        } while (value > 0);
        byte[] result = new byte[count];
        for (int i = 0; i < count; i++) {
            result[i] = (byte) (stack[count - 1 - i] | (i < count - 1 ? 0x80 : 0x00));
        }
        return result;
    }
}
