package org.santayn.testing.service.ai;

import jakarta.annotation.PreDestroy;
import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.repository.AiJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Исполнитель очереди AiJobs. Задачи берутся из таблицы (переживают перезапуск), каждую
 * забирает ровно один исполнитель — даже если серверов несколько.
 */
@Component
public class AiJobWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(AiJobWorker.class);

    private final AiProperties properties;
    private final AiJobRepository jobRepository;
    private final AiJobService jobService;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, AiJobHandler> handlers;
    private final ExecutorService executor;
    private final AtomicInteger active = new AtomicInteger();

    public AiJobWorker(AiProperties properties,
                       AiJobRepository jobRepository,
                       AiJobService jobService,
                       TransactionTemplate transactionTemplate,
                       List<AiJobHandler> handlers) {
        this.properties = properties;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
        this.transactionTemplate = transactionTemplate;
        this.handlers = handlers.stream().collect(Collectors.toMap(AiJobHandler::type, Function.identity()));
        int threads = Math.max(1, properties.getWorker().getThreads());
        AtomicInteger counter = new AtomicInteger();
        this.executor = Executors.newFixedThreadPool(threads, runnable -> {
            Thread thread = new Thread(runnable, "ai-job-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void requeueInterruptedJobs() {
        Integer requeued = transactionTemplate.execute(status -> jobRepository.requeueRunning(Instant.now()));
        if (requeued != null && requeued > 0) {
            LOGGER.info("Requeued {} interrupted AI jobs.", requeued);
        }
    }

    @Scheduled(fixedDelayString = "${app.ai.worker.poll-interval-ms:2000}", initialDelay = 5000)
    public void poll() {
        if (!properties.getWorker().isEnabled()) {
            return;
        }
        int capacity = Math.max(1, properties.getWorker().getThreads()) - active.get();
        if (capacity <= 0) {
            return;
        }
        for (Long id : jobRepository.findReadyIds(Instant.now(), PageRequest.of(0, capacity))) {
            if (claim(id)) {
                active.incrementAndGet();
                executor.submit(() -> {
                    try {
                        run(id);
                    } finally {
                        active.decrementAndGet();
                    }
                });
            }
        }
    }

    /** Выполняет все готовые задачи в текущем потоке (для тестов). */
    public int runReadyJobsNow() {
        int processed = 0;
        List<Long> ids;
        while (!(ids = jobRepository.findReadyIds(Instant.now(), PageRequest.of(0, 20))).isEmpty()) {
            boolean claimedAny = false;
            for (Long id : ids) {
                if (claim(id)) {
                    claimedAny = true;
                    run(id);
                    processed++;
                }
            }
            if (!claimedAny) {
                break;
            }
        }
        return processed;
    }

    private boolean claim(Long id) {
        Integer updated = transactionTemplate.execute(status -> jobRepository.claim(id, Instant.now()));
        return updated != null && updated == 1;
    }

    void run(Long id) {
        AiJob job = jobRepository.findById(id).orElse(null);
        if (job == null) {
            return;
        }
        AiJobHandler handler = handlers.get(job.getType());
        if (handler == null) {
            jobService.markFailed(id, "Неизвестный тип задачи: " + job.getType(), null, 0);
            return;
        }
        Context context = new Context(id);
        try {
            Object result = handler.handle(job, context);
            jobService.markDone(id, result, context.drain());
        } catch (Exception error) {
            String message = userMessage(error);
            if (!(error instanceof AiUnavailableException) && !(error instanceof IllegalArgumentException)) {
                LOGGER.warn("AI job {} ({}) failed", id, job.getType(), error);
            }
            // Ошибка во входных данных не исправится повтором.
            int maxAttempts = error instanceof IllegalArgumentException ? 0 : handler.maxAttempts();
            boolean requeued = jobService.markFailed(id, message, context.drain(), maxAttempts);
            if (!requeued) {
                try {
                    handler.onFinalFailure(job, message);
                } catch (Exception callbackError) {
                    LOGGER.warn("AI job {} failure callback failed", id, callbackError);
                }
            }
        }
    }

    static String userMessage(Exception error) {
        if (error instanceof AiUnavailableException || error instanceof IllegalArgumentException) {
            return error.getMessage();
        }
        return "Не удалось выполнить задачу ИИ. Попробуйте ещё раз.";
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private final class Context implements AiJobContext {
        private final Long jobId;
        private long inputTokens;
        private long outputTokens;
        private int ocrPages;
        private int images;

        private Context(Long jobId) {
            this.jobId = jobId;
        }

        @Override
        public void progress(String text) {
            jobService.saveProgress(jobId, text, drain());
        }

        @Override
        public synchronized void addUsage(AiChatClient.AiChatResult result) {
            if (result != null) {
                inputTokens += result.inputTokens();
                outputTokens += result.outputTokens();
            }
        }

        @Override
        public synchronized void addOcrPages(int pages) {
            ocrPages += pages;
        }

        @Override
        public synchronized void addGeneratedImages(int count) {
            images += count;
        }

        synchronized AiJobService.Usage drain() {
            AiJobService.Usage usage = new AiJobService.Usage(inputTokens, outputTokens, ocrPages, images);
            inputTokens = 0;
            outputTokens = 0;
            ocrPages = 0;
            images = 0;
            return usage;
        }
    }
}
