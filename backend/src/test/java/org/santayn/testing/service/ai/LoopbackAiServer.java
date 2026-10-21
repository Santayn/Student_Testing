package org.santayn.testing.service.ai;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/** Заглушка OpenAI-совместимого API на 127.0.0.1: отвечает тем, что вернёт handler. */
final class LoopbackAiServer implements AutoCloseable {

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private volatile Function<Request, Response> handler = request -> Response.ok(chat("{}"));

    private LoopbackAiServer(HttpServer server) {
        this.server = server;
    }

    static LoopbackAiServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        LoopbackAiServer loopback = new LoopbackAiServer(server);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Request request = new Request(
                    exchange.getRequestURI().getPath(),
                    body,
                    exchange.getRequestHeaders().entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    entry -> entry.getKey().toLowerCase(java.util.Locale.ROOT),
                                    entry -> String.join(",", entry.getValue())
                            ))
            );
            loopback.requests.add(request);
            Response response = loopback.handler.apply(request);
            byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(response.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return loopback;
    }

    String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    void respond(Function<Request, Response> handler) {
        this.handler = handler;
    }

    List<Request> requests() {
        return requests;
    }

    void reset() {
        requests.clear();
    }

    /** Тело ответа Chat Completions с заданным текстом модели. */
    static String chat(String content) {
        String escaped = content
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"" + escaped + "\"}}],"
                + "\"usage\":{\"prompt_tokens\":120,\"completion_tokens\":30}}";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    record Request(String path, String body, Map<String, String> headers) {
    }

    record Response(int status, String body) {
        static Response ok(String body) {
            return new Response(200, body);
        }
    }
}
