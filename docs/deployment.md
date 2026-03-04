## Prerequisites

- Java 21+
- Maven 3.8+
- MySQL 8+

## Deployment Steps

1. Clone the project repository to your server or development machine.
2. Create a MySQL database named `certsign_db`.
3. Configure `src/main/resources/application.properties` with your MySQL username and password.
4. From the project root, run `mvn clean install` to build the application.
5. Run the application with `mvn spring-boot:run` or by executing the built JAR.
6. Open `http://localhost:8080` in a browser to access the system.

## Default Credentials

- Default admin username: `admin`  
- Default admin password: `Admin@123`  

Change the default password immediately after first login in a production environment.

## Maintenance Notes

- Rotate RSA key pairs periodically using the Keys page in the admin portal.
- Old certificates remain verifiable because the public key used for signing is stored with the key pair referenced by each certificate.
- Monitor the `verification_logs` table for suspicious activity, such as repeated failures or unusual IP addresses.

## Known Limitations

- Private key is stored in the database unencrypted in this prototype; in production, a Hardware Security Module (HSM) or cloud KMS service should be used.
- No email notification is sent on certificate issuance.
- No certificate revocation mechanism is implemented yet.

## Future Improvements

- Add a certificate revocation list (CRL) and revocation status checks during verification.
- Add email delivery of certificate ID and verification link to the student.
- Encrypt the private key at rest using AES-256 or integrate with HSM/KMS.
- Add ECDSA support as an alternative to RSA.
- Add a QR code on the certificate detail page linking directly to the verification URL.

