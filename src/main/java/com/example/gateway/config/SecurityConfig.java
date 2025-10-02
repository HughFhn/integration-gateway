//package com.example.gateway.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.http.SessionCreationPolicy;
//import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
//import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
//import org.springframework.security.web.SecurityFilterChain;
//
//// Tells bean the config is for web security
//@Configuration
//@EnableWebSecurity
//public class SecurityConfig {
//
//    @Bean
//    // Intercept request
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                // Disables cross site request forgery
//                .csrf(csrf -> csrf.disable()) // Disable CSRF for REST API
//
//                // Defines which endpoints can be accessed by certain roles
//                .authorizeHttpRequests(auth -> auth
//
//                        // Public endpoints (if needed)
//                        .requestMatchers("/actuator/health").permitAll()
//
//                        // Secure FHIR endpoints - require authentication
//                        .requestMatchers("/fhir/**").authenticated()
//
//                        // Require specific roles for conversion operations
//                        .requestMatchers("/fhir/convert/**").hasAnyRole("USER", "ADMIN", "CONVERTER")
//
//                        // All other requests require authentication
//                        .anyRequest().authenticated()
//                )
//                // OAuth 2.0 process so must validate JWT token from keycloak
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt
//                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
//                        )
//                )
//                // Ensures no sessions per request
//                .sessionManagement(session -> session
//                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                );
//
//        return http.build();
//    }
//
//    @Bean
//    // This points where the roles live
//    public JwtAuthenticationConverter jwtAuthenticationConverter() {
//        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
//
//        // Extract roles from Keycloak's realm_access.roles claim
//        grantedAuthoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
//        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
//
//        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
//        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
//
//        return jwtAuthenticationConverter;
//    }
//}