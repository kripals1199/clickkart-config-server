// src/main/java/com/clickkart/configserver/config/SecurityConfig.java
package com.clickkart.configserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The environment-lookup endpoints (/{application}/{profile}[/{label}]) serve every other
 * service's configuration, including datasource URLs and internal topology - they are
 * HTTP Basic-protected so only clickkart services holding the shared config credential can
 * read them. CSRF is disabled: config clients fetch over stateless REST calls at boot with
 * no CSRF token support.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
