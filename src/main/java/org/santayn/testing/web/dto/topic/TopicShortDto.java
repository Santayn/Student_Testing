// src/main/java/org/santayn/testing/web/dto/topic/TopicShortDto.java
package org.santayn.testing.web.dto.topic;

import org.santayn.testing.models.topic.Topic;

public record TopicShortDto(Integer id, String name) {
    public static TopicShortDto from(Topic t) {
        return new TopicShortDto(t.getId(), t.getName());
    }
}
