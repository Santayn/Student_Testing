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
    @Order(1) // API — stateless
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // позже закроем JWT
        return http.build();
    }

    @Bean
    @Order(2) // MVC/статика/форма логина
    public SecurityFilterChain mvc(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Общие статические ресурсы Spring Boot (/static, /public, /resources, /META-INF/resources)
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        // Явные HTML-страницы, лежащие в /static
                        .requestMatchers("/", "/index.html", "/groups.html", "/group-details.html", "/faculties.html").permitAll()

                        // Статика по папкам
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        // Страницы аутентификации тоже откроем
                        .requestMatchers("/login", "/register").permitAll()
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
