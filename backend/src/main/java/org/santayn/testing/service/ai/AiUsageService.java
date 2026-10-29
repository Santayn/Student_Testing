package org.santayn.testing.service.ai;

import org.santayn.testing.repository.AiJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Расход ИИ за текущий месяц и месячный лимит токенов. */
@Service
public class AiUsageService {

    private final AiProperties properties;
    private final AiJobRepository jobRepository;

    public AiUsageService(AiProperties properties, AiJobRepository jobRepository) {
        this.properties = properties;
        this.jobRepository = jobRepository;
    }

    @Transactional(readOnly = true)
    public Usage currentMonth() {
        Instant since = startOfMonth();
        return new Usage(
                jobRepository.sumTokensSince(since),
                properties.getMonthlyTokenLimit(),
                jobRepository.sumOcrPagesSince(since),
                jobRepository.sumImagesSince(since)
        );
    }

    /** Бросает исключение, если месячный лимит токенов уже исчерпан. */
    @Transactional(readOnly = true)
    public void requireTokenBudget() {
        long limit = properties.getMonthlyTokenLimit();
        if (limit <= 0) {
            return;
        }
        long used = jobRepository.sumTokensSince(startOfMonth());
        if (used >= limit) {
            throw new AiUnavailableException(
                    "Исчерпан месячный лимит ИИ (" + limit + " токенов). Его задаёт администратор: APP_AI_MONTHLY_TOKEN_LIMIT."
            );
        }
    }

    static Instant startOfMonth() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public record Usage(long tokens, long tokenLimit, long ocrPages, long imagesGenerated) {
    }
}
