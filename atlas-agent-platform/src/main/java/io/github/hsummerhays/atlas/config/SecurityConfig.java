package io.github.hsummerhays.atlas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/github").permitAll()
                .requestMatchers("/api/v1/agents/**").hasAuthority("ROLE_SHIPPING_ADMIN")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Read "roles", "groups" or "scope" claims from JWT
            Collection<String> authorities = jwt.getClaimAsStringList("roles");
            if (authorities == null) {
                authorities = jwt.getClaimAsStringList("groups");
            }
            if (authorities == null) {
                String scope = jwt.getClaimAsString("scope");
                if (scope != null) {
                    authorities = List.of(scope.split(" "));
                }
            }
            if (authorities == null) {
                return Collections.emptyList();
            }

            return authorities.stream()
                    .map(auth -> {
                        if (auth.startsWith("ROLE_") || auth.startsWith("SCOPE_")) {
                            return new SimpleGrantedAuthority(auth);
                        }
                        // Default prefix for roles
                        return new SimpleGrantedAuthority("ROLE_" + auth.toUpperCase());
                    })
                    .collect(Collectors.toList());
        });
        return converter;
    }
}
