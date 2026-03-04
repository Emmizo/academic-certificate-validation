// SDLC Phase: Implementation
// Component: CryptoService
// Requirements covered: FR-01, FR-03, FR-04, FR-07, FR-09, NFR-01, NFR-05
// Description: Handles all RSA and SHA-256 cryptographic operations
package com.certsign.service;

import com.certsign.model.Certificate;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    /**
     * Generates a new 2048‑bit RSA key pair and returns both keys Base64‑encoded.
     */
    public Map<String, String> generateRSAKeyPair() {
        try {
            var gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048, SecureRandom.getInstanceStrong());
            java.security.KeyPair kp = gen.generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());

            Map<String, String> out = new HashMap<>();
            out.put("publicKey", publicKey);
            out.put("privateKey", privateKey);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }

    /**
     * Computes a SHA‑256 hash of the provided content and returns it as a lowercase hex string.
     */
    public String hashWithSHA256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash content with SHA-256", e);
        }
    }

    /**
     * Signs a pre‑computed data hash using the given Base64‑encoded RSA private key.
     */
    public String signData(String dataHash, String privateKeyBase64) {
        try {
            PrivateKey privateKey = parsePrivateKey(privateKeyBase64);
            // "SHA256withRSA" = hash data with SHA-256, then sign the hash using RSA
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(dataHash.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign data hash", e);
        }
    }

    /**
     * Verifies that the Base64‑encoded RSA signature matches the given data hash and public key.
     */
    public boolean verifySignature(String dataHash, String signatureBase64, String publicKeyBase64) {
        try {
            PublicKey publicKey = parsePublicKey(publicKeyBase64);
            // Verify a SHA-256 hash signed with an RSA private key ("SHA256withRSA")
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(dataHash.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Builds the canonical string representation of a certificate used for hashing and signing.
     */
    public String buildCanonicalString(Certificate cert) {
        return "CERT|"
                + cert.getCertificateId()
                + "|"
                + cert.getStudentName()
                + "|"
                + cert.getStudentId()
                + "|"
                + cert.getDegree()
                + "|"
                + cert.getInstitution()
                + "|"
                + cert.getIssueDate();
    }

    /**
     * Parses a Base64‑encoded X.509 RSA public key into a {@link PublicKey} instance.
     */
    private PublicKey parsePublicKey(String publicKeyBase64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    /**
     * Parses a Base64‑encoded PKCS#8 RSA private key into a {@link PrivateKey} instance.
     */
    private PrivateKey parsePrivateKey(String privateKeyBase64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(privateKeyBase64);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    /**
     * Converts a byte array into a lowercase hexadecimal string without separators.
     */
    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}

