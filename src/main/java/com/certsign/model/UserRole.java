// SDLC Phase: Implementation
// Component: UserRole Model
// Requirements covered: NFR-04
// Description: Defines application user roles for authorization
package com.certsign.model;

public enum UserRole {
    SUPER_ADMIN,
    ADMIN,
    USER_MANAGER,
    SIGNER,
    VERIFIER,
    SECRETARY,
    PRINCIPAL
}
