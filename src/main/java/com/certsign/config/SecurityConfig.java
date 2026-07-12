// SDLC Phase: Implementation
// Component: SecurityConfig
// Requirements covered: NFR-02, NFR-04
// Description: Configures session-based authentication and route protection
package com.certsign.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/verify",
                                "/verify/result",
                                "/contact",
                                "/forgot-password",
                                "/reset-password"
                        ).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/dashboard").hasAnyRole("SUPER_ADMIN", "ADMIN", "SIGNER", "PRINCIPAL", "SECRETARY", "USER_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/admin/users").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/users/*/edit").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/role").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        // Admin-only actions
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/enabled").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/delete").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/impersonate").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/programs").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/edit").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/activate").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/deactivate").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/delete").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/licence-types").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/licence-types/*/activate").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/licence-types/*/deactivate").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers("/admin/users/new").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/admin/users").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER")
                        // Approve / reject / bulk — PRINCIPAL or ADMIN
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/bulk-approve").hasAnyRole("SUPER_ADMIN", "ADMIN", "PRINCIPAL")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/approve").hasAnyRole("SUPER_ADMIN", "ADMIN", "PRINCIPAL")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/reject").hasAnyRole("SUPER_ADMIN", "ADMIN", "PRINCIPAL")
                        // Secretary sends certificates to students (with or without principal signature)
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/bulk-send").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/bulk-notify-principal").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/send").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/resend-email").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY", "PRINCIPAL")
                        // Secretary can issue (access issue form and POST)
                        .requestMatchers(HttpMethod.GET, "/admin/issue").hasAnyRole("SUPER_ADMIN", "ADMIN", "SIGNER", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/issue").hasAnyRole("SUPER_ADMIN", "ADMIN", "SIGNER", "SECRETARY")
                        .requestMatchers(HttpMethod.GET, "/admin/reports").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.GET, "/admin/reports/pdf").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        // All authenticated staff can reach admin area
                        .requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "SIGNER", "PRINCIPAL", "SECRETARY", "USER_MANAGER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> response.sendRedirect("/admin/dashboard"))
                        .failureHandler((request, response, exception) -> {
                            if (exception instanceof DisabledException) {
                                response.sendRedirect("/login?disabled=1");
                                return;
                            }
                            response.sendRedirect("/login?error=1");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new RequestMatcher() {
                            @Override
                            public boolean matches(HttpServletRequest request) {
                                return "GET".equalsIgnoreCase(request.getMethod())
                                        && "/logout".equals(request.getRequestURI());
                            }
                        })
                        .logoutSuccessUrl("/login")
                )
                ;

        return http.build();
    }
}
