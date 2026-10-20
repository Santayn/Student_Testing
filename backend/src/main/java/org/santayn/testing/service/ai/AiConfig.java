package org.santayn.testing.service.ai;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    @Bean
    public HttpClient aiHttpClient(AiProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(20, Math.max(5, properties.getTimeoutSeconds()))))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
