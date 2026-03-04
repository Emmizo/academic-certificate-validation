## Functional Requirements

- **FR-01**: System shall allow admin to generate RSA-2048 key pairs.
- **FR-02**: System shall allow admin to issue certificates with student details.
- **FR-03**: System shall hash certificate data using SHA-256 before signing.
- **FR-04**: System shall sign certificate hash using RSA private key.
- **FR-05**: System shall store certificates and signatures in MySQL database.
- **FR-06**: System shall allow public users to verify any certificate by ID.
- **FR-07**: System shall rebuild and rehash certificate data during verification.
- **FR-08**: System shall compare rebuilt hash against stored hash to detect tampering.
- **FR-09**: System shall verify RSA signature using stored public key.
- **FR-10**: System shall log every verification attempt with IP and result.

## Non-Functional Requirements

- **NFR-01**: RSA key size must be minimum 2048 bits.
- **NFR-02**: Password must be hashed using BCrypt strength 12.
- **NFR-03**: System must handle invalid inputs gracefully with clear error messages.
- **NFR-04**: All admin routes must be protected and require authentication.
- **NFR-05**: Private key must never be exposed through any UI or API.
- **NFR-06**: System must run on a single Spring Boot instance with no external services.

