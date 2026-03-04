## Test Environment

- Java 21
- Spring Boot
- JUnit 5

## Automated Test Cases

- **testKeyGeneration()**
  - Expected: Generated keys are non-null, non-empty, and valid RSA 2048-bit.
  - Status: PASS
- **testSignAndVerifySuccess()**
  - Expected: Signing a known string and verifying with the correct public key returns `true`.
  - Status: PASS
- **testTamperedDataFails()**
  - Expected: Modifying data after signing causes verification to return `false`.
  - Status: PASS
- **testWrongPublicKeyFails()**
  - Expected: Verifying a signature with a different public key returns `false`.
  - Status: PASS
- **testHashConsistency()**
  - Expected: Hashing the same input twice produces identical SHA-256 hashes.
  - Status: PASS
- **testHashDifferentInputs()**
  - Expected: Hashing two different strings produces different SHA-256 hashes.
  - Status: PASS
- **testCanonicalStringFormat()**
  - Expected: `buildCanonicalString` output starts with `CERT|` and contains all required fields.
  - Status: PASS

## Test Coverage Summary

- `CryptoService` is fully covered by unit tests for key generation, signing, verification, hash consistency, tamper detection, and canonical string building.
- Certificate issuance flow is exercised through integration of hashing and signing logic during unit tests.
- Verification flow logic is validated through signature checking and hash comparison tests.

## Manual Test Scenarios

- **Scenario 1**: Issue a certificate and verify it  
  Expected: Verification result is VALID and certificate details are shown.

- **Scenario 2**: Verify a certificate ID that does not exist  
  Expected: Verification result is INVALID with reason "Certificate not found".

- **Scenario 3**: Issue a certificate, manually change a field in DB, verify  
  Expected: Verification result is INVALID with reason indicating tampering.

- **Scenario 4**: Login with wrong password  
  Expected: Error message is shown on login page; access to admin pages is denied.

- **Scenario 5**: Access /admin/dashboard without login  
  Expected: Request is redirected to /login due to authentication requirement.

