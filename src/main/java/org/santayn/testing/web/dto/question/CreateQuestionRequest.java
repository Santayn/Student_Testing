// src/main/java/org/santayn/testing/web/dto/question/CreateQuestionRequest.java
package org.santayn.testing.web.dto.question;

public record CreateQuestionRequest(Integer topicId, String text, String correctAnswer) {}
