// SDLC Phase: Implementation
// Component: AuthController
// Requirements covered: NFR-04
// Description: Serves the login page for session-based authentication
package com.certsign.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    /**
     * Serves the login page used by Spring Security's form‑login flow.
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

