// SDLC Phase: Implementation
// Component: SecurityConfig
// Requirements covered: NFR-02, NFR-04
// Description: Configures session-based authentication and route protection
package com.certsign.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
                        .requestMatchers("/", "/login", "/verify", "/verify/result").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // Allow both ADMIN and SIGNER roles to access admin console
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SIGNER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/admin/dashboard", true)
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

