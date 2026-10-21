package org.santayn.testing.models.ai;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Фоновая задача ИИ: проверка текстового ответа, распознавание скана лекции, план и сборка
 * презентации, генерация картинки. Синхронные вызовы (подсказка критериев) тоже записываются
 * сюда уже завершёнными — так весь расход токенов виден в одной таблице.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`AiJobs`")
public class AiJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    @Column(name = "`Type`", nullable = false, length = 40)
    private String type;

    @Column(name = "`Status`", nullable = false)
    private int status = AiJobStatus.QUEUED;

    /** Кто запустил задачу (преподаватель). Для проверки ответов — null: её ставит система. */
    @Column(name = "`OwnerPersonId`")
    private Integer ownerPersonId;

    /** Объект задачи: id ответа, лекции и т. п. — зависит от типа. */
    @Column(name = "`TargetId`")
    private Long targetId;

    @Column(name = "`InputJson`", columnDefinition = "text")
    private String inputJson;

    @Column(name = "`ResultJson`", columnDefinition = "text")
    private String resultJson;

    @Column(name = "`ProgressText`", length = 300)
    private String progressText;

    @Column(name = "`Error`", length = 2000)
    private String error;

    @Column(name = "`Attempts`", nullable = false)
    private int attempts;

    @Column(name = "`InputTokens`", nullable = false)
    private long inputTokens;

    @Column(name = "`OutputTokens`", nullable = false)
    private long outputTokens;

    @Column(name = "`OcrPages`", nullable = false)
    private int ocrPages;

    @Column(name = "`ImagesGenerated`", nullable = false)
    private int imagesGenerated;

    @Column(name = "`CreatedAtUtc`", nullable = false)
    private Instant createdAtUtc = Instant.now();

    /** Раньше этого момента задачу не берут — так откладываются повторы после ошибки. */
    @Column(name = "`AvailableAtUtc`", nullable = false)
    private Instant availableAtUtc = Instant.now();

    @Column(name = "`StartedAtUtc`")
    private Instant startedAtUtc;

    @Column(name = "`FinishedAtUtc`")
    private Instant finishedAtUtc;
}
