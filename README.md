## Academic Certificate Digital Signature System (CertSign)

Single Spring Boot application that issues and verifies **tamper-evident academic certificates** using **RSA-2048 digital signatures** and **SHA-256 hashing**. The UI is server-rendered with **Thymeleaf** and styled using **Tailwind CSS (CDN)**.

## Tech Stack

- **Backend**: Java 21, Spring Boot
- **Views**: Thymeleaf (server-side rendered)
- **Styling**: Tailwind CSS via CDN
- **Database**: MySQL
- **Crypto**: Java `java.security` (RSA 2048 + SHA256withRSA)
- **Auth**: Spring Security (session-based)

## Setup

1. Create the MySQL database:

```sql
CREATE DATABASE certsign_db;
```

2. Update your MySQL password in `src/main/resources/application.properties`:

- `spring.datasource.password=yourpassword`

3. Run the application:

```bash
mvn spring-boot:run
```

4. Open the app:

- `http://localhost:8080`

5. Login with default admin:

- Username: `admin`
- Password: `Admin@123`

On first startup, the app seeds the default admin user and prints:

- `Default admin created: username=admin password=Admin@123`

## Workflow

1. Login as admin → `Admin Login`
2. Generate RSA key pair → `Keys` → **Generate New Key Pair**
3. Issue a certificate → `Issue` → fill form → submit
4. Copy the **Certificate ID**
5. Logout
6. Verify publicly → `Verify Certificate` → paste Certificate ID → view result

## Crypto Explanation (Plain English)

- The system takes the certificate fields and builds a **canonical string** (a consistent text representation).
- That string is hashed using **SHA-256** to produce a fixed-length **document hash**.
- The document hash is digitally signed using the university’s **RSA private key** (RSA-2048 with `SHA256withRSA`).
- To verify:
  - The system rebuilds the canonical string from stored certificate fields
  - Re-hashes it with SHA-256
  - Checks the rebuilt hash matches the stored hash (tamper detection)
  - Verifies the RSA signature using the stored **public key**

If the hashes differ or the signature verification fails, the certificate is marked **INVALID**.

