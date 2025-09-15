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

    /** API использует ту же сессию, что и MVC (НЕ stateless), CSRF для API игнорируем */
    @Bean
    @Order(1)
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**")) // POST/DELETE из JS без токена
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)) // <-- убрали STATELESS
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/**").authenticated() // API требует аутентификацию
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    /** MVC / HTML / форма логина */
    @Bean
    @Order(2)
    public SecurityFilterChain mvc(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/index.html",
                                "/groups.html", "/group-details.html",
                                "/faculties.html", "/subject-faculty.html").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/login", "/register").permitAll()
                        .anyRequest().authenticated() // <-- страницы под /kubstuTest/** теперь требуют логин
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
