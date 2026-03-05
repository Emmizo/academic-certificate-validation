package com.certsign.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CryptoService} to verify that:
 *  - RSA key pairs can be generated.
 *  - SHA-256 hashes are stable for the same input.
 *  - Digital signatures created with the private key verify with the public key.
 *  - Tampered data or wrong keys fail verification.
 */
class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService();
    }

    @Test
    void generateRSAKeyPair_shouldReturnPublicAndPrivateKey() {
        Map<String, String> keys = cryptoService.generateRSAKeyPair();

        assertThat(keys).containsKeys("publicKey", "privateKey");
        assertThat(keys.get("publicKey")).isNotBlank();
        assertThat(keys.get("privateKey")).isNotBlank();
    }

    @Test
    void signAndVerify_shouldSucceedForUntamperedData() {
        String content = "studentId=MSWE02423|degree=PM|institution=CISCO";
        String hash = cryptoService.hashWithSHA256(content);
        Map<String, String> keys = cryptoService.generateRSAKeyPair();

        String signature = cryptoService.signData(hash, keys.get("privateKey"));

        boolean ok = cryptoService.verifySignature(hash, signature, keys.get("publicKey"));

        assertThat(ok).isTrue();
    }

    @Test
    void verifySignature_shouldFailWhenDataIsTampered() {
        String content = "studentId=MSWE02423|degree=PM|institution=CISCO";
        String hash = cryptoService.hashWithSHA256(content);
        Map<String, String> keys = cryptoService.generateRSAKeyPair();

        String signature = cryptoService.signData(hash, keys.get("privateKey"));

        // Tamper with the hash (simulates changed canonical certificate data)
        String tamperedHash = hash.substring(0, hash.length() - 1) + "0";

        boolean ok = cryptoService.verifySignature(tamperedHash, signature, keys.get("publicKey"));

        assertThat(ok).isFalse();
    }

    @Test
    void verifySignature_shouldFailWithDifferentKeyPair() {
        String content = "studentId=MSWE02423|degree=PM|institution=CISCO";
        String hash = cryptoService.hashWithSHA256(content);

        Map<String, String> signingKeys = cryptoService.generateRSAKeyPair();
        Map<String, String> otherKeys = cryptoService.generateRSAKeyPair();

        String signature = cryptoService.signData(hash, signingKeys.get("privateKey"));

        // Verify with a different public key – should not validate
        boolean ok = cryptoService.verifySignature(hash, signature, otherKeys.get("publicKey"));

        assertThat(ok).isFalse();
    }
}

