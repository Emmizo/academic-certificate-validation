// SDLC Phase: Testing
// Test Type: Unit Testing
// Component under test: CryptoService
// Coverage: key generation, signing, verification, hash consistency, tamper detection
package com.certsign;

import static org.junit.jupiter.api.Assertions.*;

import com.certsign.model.Certificate;
import com.certsign.model.KeyPair;
import com.certsign.model.User;
import com.certsign.model.UserRole;
import com.certsign.service.CryptoService;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class CryptoServiceTest {

    private final CryptoService cryptoService = new CryptoService();

    @Test
    void testKeyGeneration() throws Exception {
        var keys = cryptoService.generateRSAKeyPair();
        assertNotNull(keys);
        assertNotNull(keys.get("publicKey"));
        assertNotNull(keys.get("privateKey"));
        assertFalse(keys.get("publicKey").isBlank());
        assertFalse(keys.get("privateKey").isBlank());

        RSAPublicKey pub = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(Base64.getDecoder().decode(keys.get("publicKey")))
        );
        RSAPrivateKey priv = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(Base64.getDecoder().decode(keys.get("privateKey")))
        );

        assertEquals(2048, pub.getModulus().bitLength());
        assertEquals(2048, priv.getModulus().bitLength());
    }

    @Test
    void testSignAndVerifySuccess() {
        var keys = cryptoService.generateRSAKeyPair();
        String hash = cryptoService.hashWithSHA256("hello-world");
        String sig = cryptoService.signData(hash, keys.get("privateKey"));
        assertTrue(cryptoService.verifySignature(hash, sig, keys.get("publicKey")));
    }

    @Test
    void testTamperedDataFails() {
        var keys = cryptoService.generateRSAKeyPair();
        String hash = cryptoService.hashWithSHA256("original");
        String sig = cryptoService.signData(hash, keys.get("privateKey"));

        String tamperedHash = cryptoService.hashWithSHA256("original-but-changed");
        assertFalse(cryptoService.verifySignature(tamperedHash, sig, keys.get("publicKey")));
    }

    @Test
    void testWrongPublicKeyFails() {
        var keys1 = cryptoService.generateRSAKeyPair();
        var keys2 = cryptoService.generateRSAKeyPair();

        String hash = cryptoService.hashWithSHA256("data");
        String sig = cryptoService.signData(hash, keys1.get("privateKey"));

        assertFalse(cryptoService.verifySignature(hash, sig, keys2.get("publicKey")));
    }

    @Test
    void testHashConsistency() {
        String a = cryptoService.hashWithSHA256("same-input");
        String b = cryptoService.hashWithSHA256("same-input");
        assertEquals(a, b);
    }

    @Test
    void testHashDifferentInputs() {
        String a = cryptoService.hashWithSHA256("input-a");
        String b = cryptoService.hashWithSHA256("input-b");
        assertNotEquals(a, b);
    }

    @Test
    void testCanonicalStringFormat() {
        Certificate cert = Certificate.builder()
                .certificateId("CERT-ABCDEFGH")
                .studentName("Jane Doe")
                .studentId("S12345")
                .degree("BSc Computer Science")
                .institution("Example University")
                .issueDate(LocalDate.of(2026, 3, 4))
                .keyPair(KeyPair.builder().id(1L).publicKey("PUB").privateKeyEncrypted("PRIV").active(true).build())
                .issuedBy(User.builder().id(1L).username("admin").passwordHash("x").role(UserRole.ADMIN).build())
                .documentHash("h")
                .digitalSignature("s")
                .build();

        String canonical = cryptoService.buildCanonicalString(cert);
        assertTrue(canonical.startsWith("CERT|"));
        assertTrue(canonical.contains("|CERT-ABCDEFGH|"));
        assertTrue(canonical.contains("|Jane Doe|"));
        assertTrue(canonical.contains("|S12345|"));
        assertTrue(canonical.contains("|BSc Computer Science|"));
        assertTrue(canonical.contains("|Example University|"));
        assertTrue(canonical.endsWith("|2026-03-04"));
    }
}

