package org.santayn.testing.config;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1) // API — stateless и полностью открыто для разработки
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2) // MVC / HTML / форма логина
    public SecurityFilterChain mvc(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Статические ресурсы Spring Boot (/static, /public, /resources, /META-INF/resources)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

                        // HTML-страницы, которые доступны без входа
                        .requestMatchers("/", "/index.html",
                                "/groups.html", "/group-details.html",
                                "/faculties.html", "/subject-faculty.html").permitAll()

                        // Папки со статикой
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()

                        // Страницы логина и регистрации
                        .requestMatchers("/login", "/register").permitAll()

                        // Всё остальное — только после аутентификации
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/kubstuTest", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
