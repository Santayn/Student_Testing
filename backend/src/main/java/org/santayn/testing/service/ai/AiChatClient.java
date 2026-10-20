package org.santayn.testing.service.ai;

import java.util.List;

/** Запрос к языковой модели в формате чата. */
public interface AiChatClient {

    AiChatResult complete(AiChatRequest request);

    record Message(String role, String content) {
        public static Message system(String content) {
            return new Message("system", content);
        }

        public static Message user(String content) {
            return new Message("user", content);
        }
    }

    record AiChatRequest(String model, List<Message> messages, double temperature, int maxTokens) {
    }

    record AiChatResult(String text, long inputTokens, long outputTokens) {
    }
}
