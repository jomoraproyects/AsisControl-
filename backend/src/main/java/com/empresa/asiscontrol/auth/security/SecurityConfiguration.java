package com.empresa.asiscontrol.auth.security;

import com.empresa.asiscontrol.shared.config.SecurityProperties;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65_536, 3);
    }

    @Bean
    AuthenticationManager authenticationManager(AsisUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RestAuthenticationEntryPoint entryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            AuthorizationVersionFilter authorizationVersionFilter,
            SecurityContextRepository securityContextRepository,
            CorsConfigurationSource corsConfigurationSource) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        csrfRepository.setHeaderName("X-CSRF-TOKEN");
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .securityContext(context -> context.securityContextRepository(securityContextRepository)
                        .requireExplicitSave(true))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/auth/csrf", "/api/v1/auth/login",
                                "/api/v1/auth/mfa/enroll", "/api/v1/auth/mfa/confirm",
                                "/api/v1/auth/mfa/verify", "/error").permitAll()
                        .requestMatchers("/api/v1/auth/session", "/api/v1/auth/logout",
                                "/api/v1/auth/password").authenticated()
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/roles/**",
                                "/api/v1/auditoria/**", "/api/v1/areas/**", "/api/v1/cargos/**",
                                "/api/v1/empleados/**", "/api/v1/supervisores/**",
                                "/api/v1/cuadrillas/**").authenticated()
                        .anyRequest().denyAll())
                .addFilterAfter(authorizationVersionFilter, SecurityContextHolderFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecurityProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.frontendOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN", "X-Correlation-ID"));
        configuration.setExposedHeaders(List.of("X-Correlation-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
