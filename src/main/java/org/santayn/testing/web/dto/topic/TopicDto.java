package org.santayn.testing.web.dto.topic;

import org.santayn.testing.models.topic.Topic;

public record TopicDto(Integer id, String name, Integer subjectId) {
    public static TopicDto from(Topic t) {
        return new TopicDto(t.getId(), t.getName(),
                t.getSubject() != null ? t.getSubject().getId() : null);
    }
}
