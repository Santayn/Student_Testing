package org.santayn.testing.models.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Выбранный администратором движок функции ИИ. Нет записи — действует значение из настроек (app.ai.engines.*). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`AiEngineSettings`")
public class AiEngineSetting {

    @Id
    @Column(name = "`Feature`", length = 40)
    private String feature;

    @Column(name = "`Engine`", nullable = false, length = 20)
    private String engine;

    @Column(name = "`UpdatedAtUtc`", nullable = false)
    private Instant updatedAtUtc = Instant.now();

    @Column(name = "`UpdatedByPersonId`")
    private Integer updatedByPersonId;
}
