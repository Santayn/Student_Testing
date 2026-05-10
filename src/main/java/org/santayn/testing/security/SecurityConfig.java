package org.santayn.testing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] ADMIN_AUTHORITIES = {
            "ROLE_ADMIN", "ADMIN", "roles.manage", "ROLES.MANAGE"
    };
    private static final String[] USER_READ_AUTHORITIES = {
            "ROLE_ADMIN", "ADMIN", "users.read", "USERS.READ"
    };
    private static final String[] USER_WRITE_AUTHORITIES = {
            "ROLE_ADMIN", "ADMIN", "users.write", "USERS.WRITE"
    };
    private static final String[] PEOPLE_READ_AUTHORITIES = {
            "ROLE_ADMIN", "ADMIN", "ROLE_TEACHER", "TEACHER", "people.read", "PEOPLE.READ", "users.read", "USERS.READ"
    };
    private static final String[] PEOPLE_WRITE_AUTHORITIES = {
            "ROLE_ADMIN", "ADMIN", "ROLE_TEACHER", "TEACHER", "people.write", "PEOPLE.WRITE", "users.write", "USERS.WRITE"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      JwtAuthenticationFilter jwtAuthenticationFilter,
                                                      AuthenticationProvider authenticationProvider,
                                                      ObjectMapper objectMapper) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(response, request, objectMapper,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "unauthorized",
                                        "Authentication is required."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeSecurityError(response, request, objectMapper,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "forbidden",
                                        "Not enough permissions."))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/api/v1/public/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyAuthority(ADMIN_AUTHORITIES)
                        .requestMatchers("/api/v1/roles/**", "/api/roles/**").hasAnyAuthority(ADMIN_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, "/api/users/me", "/api/v1/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/people/**", "/api/v1/users/people/**").hasAnyAuthority(PEOPLE_READ_AUTHORITIES)
                        .requestMatchers(HttpMethod.POST, "/api/users/people/**", "/api/v1/users/people/**").hasAnyAuthority(PEOPLE_WRITE_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, "/api/users/people/**", "/api/v1/users/people/**").hasAnyAuthority(PEOPLE_WRITE_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/roles", "/api/v1/users/*/roles").hasAnyAuthority(ADMIN_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/permissions", "/api/v1/users/*/permissions").hasAnyAuthority(ADMIN_AUTHORITIES)
                        .requestMatchers(HttpMethod.PUT, "/api/users/*/active", "/api/v1/users/*/active").hasAnyAuthority(USER_WRITE_AUTHORITIES)
                        .requestMatchers(HttpMethod.GET, "/api/users/**", "/api/v1/users/**").hasAnyAuthority(USER_READ_AUTHORITIES)
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/kubstuTest/**",
                                "/index.html",
                                "/login.html",
                                "/register.html",
                                "/main.html",
                                "/about.html",
                                "/students.html",
                                "/student-details.html",
                                "/profile.html",
                                "/subjects.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/favicon.ico"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserRegisterService userRegisterService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userRegisterService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private void writeSecurityError(HttpServletResponse response,
                                    HttpServletRequest request,
                                    ObjectMapper objectMapper,
                                    int status,
                                    String error,
                                    String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", error,
                "message", message,
                "path", request.getRequestURI()
        );

        objectMapper.writeValue(response.getWriter(), body);
    }
}
