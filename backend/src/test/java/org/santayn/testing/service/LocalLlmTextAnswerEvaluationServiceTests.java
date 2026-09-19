package org.santayn.testing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalLlmTextAnswerEvaluationServiceTests {

    @Test
    void acceptsSemanticAnswerApprovedByLocalLlm() throws Exception {
        try (LoopbackServer server = LoopbackServer.responding("{\"response\":\"true\"}")) {
            TextAnswerEvaluationService service = new LocalLlmTextAnswerEvaluationService(
                    new ObjectMapper(),
                    server.endpoint(),
                    "test-model",
                    3
            );

            assertThat(service.isCorrect(
                    "What is SQL?",
                    "SQL is a declarative query language for relational databases",
                    "It lets you read and change data in database tables"
            )).isTrue();
            assertThat(server.requestCount()).isEqualTo(1);
            assertThat(server.lastRequestBody()).contains("\"model\":\"test-model\"");
        }
    }

    @Test
    void doesNotCallLocalLlmWhenLocalEvaluatorAcceptsAnswer() throws Exception {
        try (LoopbackServer server = LoopbackServer.responding("{\"response\":\"false\"}")) {
            TextAnswerEvaluationService service = new LocalLlmTextAnswerEvaluationService(
                    new ObjectMapper(),
                    server.endpoint(),
                    "test-model",
                    3
            );

            assertThat(service.isCorrect("What is RAM?", "оперативная память", "оперитивная паметь"))
                    .isTrue();
            assertThat(server.requestCount()).isZero();
        }
    }

    @Test
    void returnsPartialCreditApprovedByLocalLlm() throws Exception {
        try (LoopbackServer server = LoopbackServer.responding("{\"response\":\"partial\"}")) {
            TextAnswerEvaluationService service = new LocalLlmTextAnswerEvaluationService(
                    new ObjectMapper(),
                    server.endpoint(),
                    "test-model",
                    3
            );

            TextAnswerEvaluationResult result = service.evaluate(
                    "What is SQL?",
                    "SQL is a declarative query language for relational databases",
                    "It is a language for database tables"
            );

            assertThat(result.correct()).isFalse();
            assertThat(result.scoreRatio()).isEqualByComparingTo(new BigDecimal("0.5"));
            assertThat(server.requestCount()).isEqualTo(1);
        }
    }

    @Test
    void rejectsNonLocalEndpoint() {
        assertThatThrownBy(() -> new LocalLlmTextAnswerEvaluationService(
                new ObjectMapper(),
                "http://example.com/api/generate",
                "test-model",
                3
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("localhost");
    }

    private static final class LoopbackServer implements AutoCloseable {

        private final HttpServer server;
        private final AtomicInteger requestCount = new AtomicInteger();
        private volatile String lastRequestBody = "";

        private LoopbackServer(HttpServer server) {
            this.server = server;
        }

        static LoopbackServer responding(String responseBody) throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            LoopbackServer wrapper = new LoopbackServer(server);
            server.createContext("/api/generate", exchange -> {
                wrapper.requestCount.incrementAndGet();
                wrapper.lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, responseBytes.length);
                exchange.getResponseBody().write(responseBytes);
                exchange.close();
            });
            server.start();
            return wrapper;
        }

        String endpoint() {
            return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/generate";
        }

        int requestCount() {
            return requestCount.get();
        }

        String lastRequestBody() {
            return lastRequestBody;
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
