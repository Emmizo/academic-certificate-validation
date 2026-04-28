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

1. Create the MySQL database. The name **must match** the one used in `spring.datasource.url` inside `src/main/resources/application.properties` (by default we use `certsign_db`):

```sql
CREATE DATABASE certsign_db;
```

You do **not** need any absolute path from this README; just run the SQL in your own MySQL server.

You can optionally import the prepared schema + sample data from:

- `src/main/resources/database/certsign_db.sql`

using your preferred MySQL tool (MySQL Workbench, CLI, etc.).

2. Update your MySQL password in `src/main/resources/application.properties`:

- `spring.datasource.password=yourpassword`

If you changed the database name, also update:

- `spring.datasource.url=jdbc:mysql://localhost:3306/<certsign_db>?useSSL=false&serverTimezone=UTC`

3. Run the application:

```bash
mvn spring-boot:run
```

4. Open the app:

- `http://localhost:8080`

5. Login with default accounts:

- **Admin** – full access to keys, issuing, and students  
  - Username: `admin`  
  - Password: `Admin@123`
- **Signer** – user responsible for issuing/signing certificates (same UI as admin today)  
  - Username: `signer`  
  - Password: `Signer@123`
- **Verifier (staff)** – read-only reviewer/verification user inside the institution  
  - Username: `verifier`  
  - Password: `Verifier@123`

On first startup, the app seeds these users and prints their credentials to the console.

## User Management and Password Reset

- Admins can list users, create users, and assign roles in the admin console.
- Users with `USER_MANAGER` role can only access the "Create User" page.
- On user creation, the app sends the username and temporary password by email.
- Login page supports "Forgot password" and reset-by-token flow.

### SMTP Configuration (required for emails)

Add the following to your local `application.properties`:

```properties
spring.mail.host=smtp.example.com
spring.mail.port=587
spring.mail.username=your-smtp-user
spring.mail.password=your-smtp-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.base-url=http://localhost:8000
```

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

