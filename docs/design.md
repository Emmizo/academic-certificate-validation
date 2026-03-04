## System Architecture Description

- Single-tier Spring Boot application serving both backend logic and frontend views.
- Thymeleaf renders HTML server-side; there is no separate frontend server.
- MySQL stores users, key pairs, certificates, and verification logs.
- Spring Security manages session-based authentication and route protection.
- `CryptoService` is isolated and handles all cryptographic operations.

## Component Descriptions

- **CryptoService**
  - RSA key generation (2048-bit), SHA-256 hashing, signing, and verification.
  - Provides canonical string builder for certificate data.
- **CertificateService**
  - Implements certificate issuance and verification flows.
  - Coordinates with `CryptoService` and repositories for persistence.
- **UserService**
  - Loads user entities from the database for Spring Security authentication.
  - Applies role-based authorities for admin access.
- **AdminController**
  - Handles all protected admin routes: dashboard, keys, issuing certificates, and certificate listing/detail.
- **PublicController**
  - Handles public landing page and certificate verification form and result.
- **DataInitializer**
  - Seeds default admin user on first startup with configured credentials.

## Data Flow for Certificate Issuance

1. Admin fills form on `/admin/issue`.
2. Browser sends `POST /admin/issue` with CSRF token.
3. `AdminController` receives the request and delegates to `CertificateService.issueCertificate()`.
4. `CertificateService` uses `CryptoService.buildCanonicalString()` to build canonical string.
5. `CertificateService` uses `CryptoService.hashWithSHA256()` to hash the canonical string.
6. `CertificateService` uses `CryptoService.signData()` with the active private key to sign the hash.
7. Certificate, hash, signature, and key pair reference are saved to the database.
8. User is redirected to the certificate detail page for the newly created certificate.

## Data Flow for Verification

1. Public user enters certificate ID on `/verify`.
2. Browser sends `POST /verify` with the certificate ID and CSRF token.
3. `PublicController` delegates to `CertificateService.verifyCertificate()`.
4. Service fetches certificate from DB by certificate ID; if not found, returns invalid result.
5. Associated key pair is loaded.
6. `CryptoService.buildCanonicalString()` rebuilds canonical string from stored certificate.
7. `CryptoService.hashWithSHA256()` computes hash; service compares rebuilt hash with stored hash.
8. If hashes differ, result is invalid due to tampering.
9. If hashes match, `CryptoService.verifySignature()` verifies the stored digital signature using the stored public key.
10. Result is logged in `verification_logs` with IP, outcome, and optional failure reason.
11. `VerificationResult` is passed back to the template for display.

## UML Class Diagram Description (Textual)

Classes: `User`, `KeyPair`, `Certificate`, `VerificationLog`, `CryptoService`, `CertificateService`, `UserService`

Relationships:

- `Certificate` has a many-to-one relationship with `KeyPair` (`key_pair_id` foreign key).
- `Certificate` has a many-to-one relationship with `User` (`issued_by` foreign key).
- `VerificationLog` references `certificate_id` as a string.
- `CertificateService` depends on `CryptoService`.
- `AdminController` depends on `CertificateService`.
- `PublicController` depends on `CertificateService`.

## Database ER Diagram Description (Textual)

- **users**
  - Columns: `id` (PK), `username` (UNIQUE), `password_hash`, `role`, `created_at`
- **key_pairs**
  - Columns: `id` (PK), `public_key`, `private_key_encrypted`, `created_at`, `is_active`
- **certificates**
  - Columns: `id` (PK), `certificate_id` (UNIQUE), `student_name`, `student_id`, `degree`, `institution`, `issue_date`, `document_hash`, `digital_signature`, `key_pair_id` (FK), `issued_by` (FK), `created_at`
- **verification_logs**
  - Columns: `id` (PK), `certificate_id`, `verifier_ip`, `result`, `failure_reason`, `verified_at`

