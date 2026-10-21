package org.santayn.testing.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiCompatibleChatClientTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsYandexModelUriAndHeadersAndReadsUsage() throws Exception {
        try (LoopbackAiServer server = LoopbackAiServer.start()) {
            server.respond(request -> LoopbackAiServer.Response.ok(LoopbackAiServer.chat("Привет")));
            AiProperties properties = properties(AiProperties.PROVIDER_YANDEX, server.baseUrl());
            properties.setFolderId("b1gfolder");
            OpenAiCompatibleChatClient client = client(properties);

            AiChatClient.AiChatResult result = client.ask("aliceai-llm-flash", "system", "user", 0.0, 100);

            assertThat(result.text()).isEqualTo("Привет");
            assertThat(result.inputTokens()).isEqualTo(120);
            assertThat(result.outputTokens()).isEqualTo(30);
            LoopbackAiServer.Request request = server.requests().get(0);
            assertThat(request.path()).isEqualTo("/v1/chat/completions");
            assertThat(request.body()).contains("\"model\":\"gpt://b1gfolder/aliceai-llm-flash\"");
            assertThat(request.headers().get("authorization")).isEqualTo("Api-Key secret-key");
            assertThat(request.headers().get("openai-project")).isEqualTo("b1gfolder");
            assertThat(request.headers().get("x-data-logging-enabled")).isEqualTo("false");
        }
    }

    @Test
    void openAiProviderKeepsModelNameAndUsesBearer() throws Exception {
        try (LoopbackAiServer server = LoopbackAiServer.start()) {
            server.respond(request -> LoopbackAiServer.Response.ok(LoopbackAiServer.chat("ok")));
            OpenAiCompatibleChatClient client = client(properties(AiProperties.PROVIDER_OPENAI, server.baseUrl()));

            client.ask("qwen2.5:7b", "system", "user", 0.0, 100);

            LoopbackAiServer.Request request = server.requests().get(0);
            assertThat(request.body()).contains("\"model\":\"qwen2.5:7b\"");
            assertThat(request.headers().get("authorization")).isEqualTo("Bearer secret-key");
        }
    }

    @Test
    void reportsRejectedKeyInRussianWithoutLeakingIt() throws Exception {
        try (LoopbackAiServer server = LoopbackAiServer.start()) {
            server.respond(request -> new LoopbackAiServer.Response(401, "{\"error\":\"bad key\"}"));
            OpenAiCompatibleChatClient client = client(properties(AiProperties.PROVIDER_OPENAI, server.baseUrl()));

            assertThatThrownBy(() -> client.ask("model", "system", "user", 0.0, 100))
                    .isInstanceOf(AiUnavailableException.class)
                    .hasMessageContaining("ключ")
                    .hasMessageNotContaining("secret-key");
        }
    }

    @Test
    void refusesToCallWhenProviderIsDisabled() {
        OpenAiCompatibleChatClient client = client(properties(AiProperties.PROVIDER_NONE, "http://127.0.0.1:1/v1"));

        assertThatThrownBy(() -> client.ask("model", "system", "user", 0.0, 100))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessageContaining("APP_AI_PROVIDER");
    }

    private AiProperties properties(String provider, String baseUrl) {
        AiProperties properties = new AiProperties();
        properties.setProvider(provider);
        properties.setBaseUrl(baseUrl);
        properties.setApiKey("secret-key");
        properties.setTimeoutSeconds(5);
        return properties;
    }

    private OpenAiCompatibleChatClient client(AiProperties properties) {
        HttpClient httpClient = HttpClient.newHttpClient();
        AiProviderEndpoints endpoints = new AiProviderEndpoints(
                properties,
                new GigaChatTokenProvider(properties, httpClient, objectMapper)
        );
        return new OpenAiCompatibleChatClient(properties, endpoints, httpClient, objectMapper);
    }
}
