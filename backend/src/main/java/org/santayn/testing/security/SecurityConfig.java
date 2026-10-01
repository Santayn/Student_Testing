package org.santayn.testing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.santayn.testing.service.UserRegisterService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.UUID;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] STRICT_ADMIN_AUTHORITIES = {
            "ROLE_ADMIN", "ADMIN"
    };

    private static final String[] ROLE_MANAGEMENT_AUTHORITIES = {
            "roles.manage", "ROLES.MANAGE"
    };

    private static final String[] USER_READ_AUTHORITIES = {
            "users.read", "USERS.READ"
    };

    private static final String[] USER_WRITE_AUTHORITIES = {
            "users.write", "USERS.WRITE"
    };

    private static final String[] PEOPLE_READ_AUTHORITIES = {
            "people.read", "PEOPLE.READ"
    };

    private static final String[] PEOPLE_WRITE_AUTHORITIES = {
            "people.write", "PEOPLE.WRITE"
    };

    private static final String[] SUBJECT_READ_AUTHORITIES = {
            "courses.manage", "COURSES.MANAGE",
            "teaching.manage", "TEACHING.MANAGE",
            "academic.manage", "ACADEMIC.MANAGE"
    };

    private static final String[] ACADEMIC_WRITE_AUTHORITIES = {
            "academic.manage", "ACADEMIC.MANAGE"
    };

    private static final String[] TEACHING_WRITE_AUTHORITIES = {
            "teaching.manage", "TEACHING.MANAGE"
    };

    private static final String[] COURSE_WRITE_AUTHORITIES = {
            "courses.manage", "COURSES.MANAGE"
    };

    private static final String[] TEST_WRITE_AUTHORITIES = {
            "tests.manage", "TESTS.MANAGE"
    };

    private static final String[] QUESTION_WRITE_AUTHORITIES = {
            "questions.manage", "QUESTIONS.MANAGE"
    };

    private static final String[] TEACHER_CONTEXT_AUTHORITIES = {
            "questions.manage", "QUESTIONS.MANAGE",
            "tests.manage", "TESTS.MANAGE",
            "courses.manage", "COURSES.MANAGE",
            "teaching.manage", "TEACHING.MANAGE",
            "academic.manage", "ACADEMIC.MANAGE"
    };

    private static final String[] TEACHING_READ_AUTHORITIES = {
            "teaching.manage", "TEACHING.MANAGE",
            "academic.manage", "ACADEMIC.MANAGE"
    };

    private static final String[] TEACHING_ASSIGNMENT_READ_AUTHORITIES = {
            "teaching.manage", "TEACHING.MANAGE",
            "tests.manage", "TESTS.MANAGE",
            "academic.manage", "ACADEMIC.MANAGE"
    };

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                                      JwtAuthenticationFilter jwtAuthenticationFilter,
                                                      AuthenticationProvider authenticationProvider,
                                                      ObjectMapper objectMapper) throws Exception {
        http
                .securityMatcher("/api/v1/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sessionManagementConfigurer ->
                        sessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptionHandlingConfigurer -> exceptionHandlingConfigurer
                        .authenticationEntryPoint((request, response, authException) ->
                                writeSecurityError(
                                        response,
                                        request,
                                        objectMapper,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "UNAUTHORIZED",
                                        "Authentication is required."
                                )
                        )
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeSecurityError(
                                        response,
                                        request,
                                        objectMapper,
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "FORBIDDEN",
                                        "Not enough permissions."
                                )
                        )
                )
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry ->
                        authorizationManagerRequestMatcherRegistry
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers(
                                        "/api/v1/auth/login",
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/refresh",
                                        "/api/v1/auth/revoke",
                                        "/api/v1/auth/csrf",
                                        "/api/v1/auth/config",
                                        "/api/v1/status",
                                        "/api/v1/status/**"
                                ).permitAll()
                                .requestMatchers("/api/v1/public/learning/**").authenticated()
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/admin/database-backups",
                                        "/api/v1/admin/database-backups/restore"
                                )
                                .hasAnyAuthority(STRICT_ADMIN_AUTHORITIES)
                                .requestMatchers("/api/v1/roles/**").hasAnyAuthority(ROLE_MANAGEMENT_AUTHORITIES)
                                .requestMatchers("/api/v1/tests/attempts/**", "/api/v1/tests/responses/**")
                                .hasAnyAuthority(STRICT_ADMIN_AUTHORITIES)
                                .requestMatchers(HttpMethod.POST, "/api/v1/tests/assignments/*/attempts")
                                .hasAnyAuthority(STRICT_ADMIN_AUTHORITIES)
                                .requestMatchers("/api/v1/questions/**").hasAnyAuthority(QUESTION_WRITE_AUTHORITIES)
                                .requestMatchers("/api/v1/topics/**").hasAnyAuthority(QUESTION_WRITE_AUTHORITIES)
                                .requestMatchers("/api/v1/tests/**").hasAnyAuthority(TEST_WRITE_AUTHORITIES)
                                .requestMatchers("/api/v1/courses/**").hasAnyAuthority(COURSE_WRITE_AUTHORITIES)
                                .requestMatchers("/api/v1/lectures/**").hasAnyAuthority(COURSE_WRITE_AUTHORITIES)
                                .requestMatchers("/api/v1/results/teacher/**").hasAnyAuthority(TEST_WRITE_AUTHORITIES)
                                .requestMatchers("/api/v1/results/student/**").authenticated()
                                .requestMatchers(HttpMethod.POST, "/api/v1/memberships/**").hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/memberships/**").hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/memberships/**").hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.GET, "/api/v1/subjects", "/api/v1/subjects/**")
                                .hasAnyAuthority(SUBJECT_READ_AUTHORITIES)
                                .requestMatchers(HttpMethod.GET, "/api/v1/faculties", "/api/v1/faculties/**", "/api/v1/groups", "/api/v1/groups/**")
                                .hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.POST, "/api/v1/faculties/**", "/api/v1/groups/**", "/api/v1/subjects/**")
                                .hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/faculties/**", "/api/v1/groups/**", "/api/v1/subjects/**")
                                .hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/faculties/**", "/api/v1/groups/**", "/api/v1/subjects/**")
                                .hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.GET, "/api/v1/teaching/profile-context")
                                .hasAnyAuthority(TEACHER_CONTEXT_AUTHORITIES)
                                .requestMatchers(HttpMethod.GET, "/api/v1/teaching/workload")
                                .hasAnyAuthority(TEACHING_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.GET, "/api/v1/teaching/assignments", "/api/v1/teaching/assignments/*")
                                .hasAnyAuthority(TEACHING_ASSIGNMENT_READ_AUTHORITIES)
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/teaching/load-types",
                                        "/api/v1/teaching/load-types/**",
                                        "/api/v1/teaching/subject-load-types",
                                        "/api/v1/teaching/enrollments",
                                        "/api/v1/teaching/lecture-assignments",
                                        "/api/v1/teaching/lecture-progress"
                                ).hasAnyAuthority(TEACHING_READ_AUTHORITIES)
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/teaching/load-types",
                                        "/api/v1/teaching/load-types/**"
                                ).hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/teaching/load-types",
                                        "/api/v1/teaching/load-types/**"
                                ).hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.POST, "/api/v1/teaching/assignments")
                                .hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/teaching/assignments/*",
                                        "/api/v1/teaching/assignments/*/status"
                                ).hasAnyAuthority(ACADEMIC_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.POST, "/api/v1/teaching/**").hasAnyAuthority(TEACHING_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/teaching/**").hasAnyAuthority(TEACHING_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.DELETE, "/api/v1/teaching/**").hasAnyAuthority(TEACHING_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.GET, "/api/v1/users/me").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/v1/users/people/**").hasAnyAuthority(PEOPLE_READ_AUTHORITIES)
                                .requestMatchers(HttpMethod.POST, "/api/v1/users/people/**").hasAnyAuthority(PEOPLE_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/users/people/**").hasAnyAuthority(PEOPLE_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/person").hasAnyAuthority(USER_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/roles").hasAnyAuthority(ROLE_MANAGEMENT_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/permissions").hasAnyAuthority(ROLE_MANAGEMENT_AUTHORITIES)
                                .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/active").hasAnyAuthority(USER_WRITE_AUTHORITIES)
                                .requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyAuthority(USER_READ_AUTHORITIES)
                                .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain nonApiSecurityFilterChain(
            HttpSecurity http,
            @Value("${app.openapi.public-enabled:false}") boolean openApiPublicEnabled
    ) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> {
                    authorizationManagerRequestMatcherRegistry.requestMatchers("/error").permitAll();
                    if (openApiPublicEnabled) {
                        authorizationManagerRequestMatcherRegistry.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll();
                    } else {
                        authorizationManagerRequestMatcherRegistry.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).denyAll();
                    }
                    authorizationManagerRequestMatcherRegistry.anyRequest().denyAll();
                });

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserRegisterService userRegisterService,
                                                         PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userRegisterService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:[*],http://127.0.0.1:[*]}") List<String> allowedOrigins) {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        corsConfiguration.setAllowedOriginPatterns(allowedOrigins);
        corsConfiguration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        corsConfiguration.setAllowedHeaders(List.of("*"));
        corsConfiguration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Content-Disposition",
                "Content-Length"
        ));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return urlBasedCorsConfigurationSource;
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

        String requestId = UUID.randomUUID().toString();
        response.setHeader("X-Request-Id", requestId);

        org.santayn.testing.web.dto.common.ErrorResponse responseBody =
                org.santayn.testing.web.dto.common.ErrorResponse.of(
                        status,
                        error,
                        message,
                        requestId
                );

        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}
