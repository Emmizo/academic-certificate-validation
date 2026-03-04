## Project Title

Academic Certificate Digital Signature System

## Scope

Web-based system for issuing and verifying digitally signed academic certificates using RSA-2048 and SHA-256, built as a single Spring Boot application serving both backend APIs and Thymeleaf-rendered HTML pages.

## Objectives

- Implement RSA key generation for 2048-bit key pairs.
- Implement certificate signing using SHA-256 and RSA private keys.
- Implement signature verification using stored public keys.
- Provide an admin portal for key management and certificate issuance.
- Provide a public verification page for anyone to verify certificates by ID.

## Feasibility

Technical feasibility is confirmed using:

- Java Spring Boot for the application framework.
- MySQL for relational data storage.
- Thymeleaf for server-side rendered HTML views.
- Standard `java.security` cryptographic library for RSA and SHA-256 operations.

These technologies are mature, well-documented, and interoperable, making the solution feasible on common Java hosting environments.

## Timeline Estimate

- Planning → Completed in this document.
- Requirements → Captured in `docs/requirements.md`.
- Design → Captured in `docs/design.md`.
- Implementation → Spring Boot codebase with services, controllers, views, and security.
- Testing → Unit tests for cryptographic operations and manual end-to-end scenarios.
- Deployment → Maven build and Spring Boot runtime deployment to a Java 17+ environment.

## Risks and Mitigations

- **Private key compromise**
  - Mitigations: Restrict database and server access, run application on hardened hosts, plan for future HSM or KMS integration, rotate keys periodically via the Keys page.
- **Database tampering**
  - Mitigations: Use least-privilege DB credentials, enable database-level access controls and auditing, rely on hash and signature verification to detect data manipulation.
- **Brute force login attempts**
  - Mitigations: Use BCrypt password hashing with strength 12, enforce strong default admin password, recommend adding rate limiting and account lockout in production, and monitor authentication logs for suspicious activity.

