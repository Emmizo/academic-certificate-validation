package com.certsign.tools;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Small standalone helper you can run from your IDE to
 * manually verify a digital signature using the current public key,
 * a document hash, and the full Base64 signature.
 *
 * Usage:
 *  1) Copy values from the UI:
 *     - Document Hash (SHA-256)
 *     - Digital Signature (full, from "Show full signature")
 *     - Active Public Key (from the Keys page)
 *  2) Paste them into the three placeholders below.
 *  3) Run this class' main method.
 */
public class SignatureCheckExample {

    public static void main(String[] args) throws Exception {
        // 1) Paste the values you want to test here:
        String documentHash = "PASTE_DOCUMENT_HASH_HERE";
        String signatureBase64 = "PASTE_FULL_SIGNATURE_BASE64_HERE";
        String publicKeyBase64 = "PASTE_PUBLIC_KEY_BASE64_HERE";

        // 2) Parse the Base64-encoded RSA public key (X.509 format)
        byte[] publicBytes = Base64.getDecoder().decode(publicKeyBase64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicBytes);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);

        // 3) Verify the signature with the "SHA256withRSA" algorithm:
        //    - Recomputes SHA-256 over the supplied documentHash string bytes
        //    - Verifies that the RSA signature comes from the holder of the private key
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(documentHash.getBytes(StandardCharsets.UTF_8));

        byte[] sigBytes = Base64.getDecoder().decode(signatureBase64);
        boolean valid = sig.verify(sigBytes);

        System.out.println("Document hash:       " + documentHash);
        System.out.println("Signature length:    " + sigBytes.length + " bytes");
        System.out.println("Signature valid?     " + valid);
    }
}

