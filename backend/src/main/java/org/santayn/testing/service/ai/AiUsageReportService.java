package org.santayn.testing.service.ai;

import org.santayn.testing.models.ai.AiJob;
import org.santayn.testing.models.ai.AiJobStatus;
import org.santayn.testing.models.ai.AiJobType;
import org.santayn.testing.models.group.GroupMembership;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.question.QuestionResponse;
import org.santayn.testing.models.test.TestAttempt;
import org.santayn.testing.repository.AiJobRepository;
import org.santayn.testing.repository.GroupMembershipRepository;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.PersonRepository;
import org.santayn.testing.repository.QuestionResponseRepository;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestAttemptRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Отчёт о расходе ИИ для администраторов: всего, по типам задач, по преподавателям и по студенческим
 * группам. Проверка ответа относится к автору теста и к группам студента, остальные задачи — к тому,
 * кто их запустил.
 */
@Service
public class AiUsageReportService {

    private static final int GROUP_ROLE_STUDENT = 1;
    private static final Map<String, String> TYPE_TITLES = Map.of(
            AiJobType.TEXT_GRADING, "Проверка ответов",
            AiJobType.CRITERIA_SUGGESTION, "Подсказка критериев",
            AiJobType.LECTURE_SCAN, "Распознавание сканов",
            AiJobType.PRESENTATION_OUTLINE, "План презентации",
            AiJobType.PRESENTATION_BUILD, "Сборка презентации",
            AiJobType.IMAGE_GENERATION, "Генерация картинок"
    );

    private final AiProperties properties;
    private final AiJobRepository jobRepository;
    private final QuestionResponseRepository responseRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final TestRepository testRepository;
    private final PersonRepository personRepository;
    private final GroupMembershipRepository groupMembershipRepository;
    private final GroupRepository groupRepository;

    public AiUsageReportService(AiProperties properties,
                                AiJobRepository jobRepository,
                                QuestionResponseRepository responseRepository,
                                TestAttemptRepository attemptRepository,
                                TestAssignmentRepository assignmentRepository,
                                TestRepository testRepository,
                                PersonRepository personRepository,
                                GroupMembershipRepository groupMembershipRepository,
                                GroupRepository groupRepository) {
        this.properties = properties;
        this.jobRepository = jobRepository;
        this.responseRepository = responseRepository;
        this.attemptRepository = attemptRepository;
        this.assignmentRepository = assignmentRepository;
        this.testRepository = testRepository;
        this.personRepository = personRepository;
        this.groupMembershipRepository = groupMembershipRepository;
        this.groupRepository = groupRepository;
    }

    /** @param from первый день периода, @param to последний день (включительно); по умолчанию — текущий месяц */
    @Transactional(readOnly = true)
    public Report report(LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        LocalDate end = to != null ? to : LocalDate.now(ZoneOffset.UTC);
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Конец периода раньше начала.");
        }
        Instant since = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant until = end.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<AiJob> jobs = jobRepository.findByCreatedAtUtcGreaterThanEqualAndCreatedAtUtcLessThan(since, until);

        Map<String, Row> byType = new LinkedHashMap<>();
        Map<Integer, Row> byUser = new HashMap<>();
        Map<Integer, Row> byGroup = new HashMap<>();
        Row total = new Row();
        Attribution attribution = new Attribution();

        for (AiJob job : jobs) {
            total.add(job);
            byType.computeIfAbsent(job.getType(), key -> new Row()).add(job);
            if (AiJobType.TEXT_GRADING.equals(job.getType())) {
                GradingOwner owner = attribution.grading(job.getTargetId());
                byUser.computeIfAbsent(owner.teacherPersonId(), key -> new Row()).add(job);
                for (Integer groupId : owner.groupIds()) {
                    byGroup.computeIfAbsent(groupId, key -> new Row()).add(job);
                }
            } else {
                byUser.computeIfAbsent(job.getOwnerPersonId(), key -> new Row()).add(job);
            }
        }

        List<TypeUsage> types = byType.entrySet().stream()
                .map(entry -> new TypeUsage(entry.getKey(), TYPE_TITLES.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue().usage()))
                .sorted(Comparator.comparingLong((TypeUsage usage) -> usage.usage().tokens()).reversed())
                .toList();
        List<UserUsage> users = byUser.entrySet().stream()
                .map(entry -> new UserUsage(entry.getKey(), attribution.personName(entry.getKey()), entry.getValue().usage()))
                .sorted(Comparator.comparingLong((UserUsage usage) -> usage.usage().tokens()).reversed())
                .toList();
        List<GroupUsage> groups = byGroup.entrySet().stream()
                .map(entry -> new GroupUsage(entry.getKey(), attribution.groupName(entry.getKey()), entry.getValue().usage()))
                .sorted(Comparator.comparingLong((GroupUsage usage) -> usage.usage().tokens()).reversed())
                .toList();

        Instant monthStart = AiUsageService.startOfMonth();
        return new Report(
                start,
                end,
                properties.normalizedProvider(),
                total.usage(),
                properties.getMonthlyTokenLimit(),
                jobRepository.sumTokensSince(monthStart),
                types,
                users,
                groups
        );
    }

    /** Кэш поиска автора теста и групп студента по ответу. */
    private final class Attribution {
        private final Map<Integer, String> people = new HashMap<>();
        private final Map<Integer, String> groups = new HashMap<>();
        private final Map<Integer, GradingOwner> byAttempt = new HashMap<>();

        GradingOwner grading(Long responseId) {
            Optional<QuestionResponse> response = responseId == null ? Optional.empty() : responseRepository.findById(responseId);
            if (response.isEmpty()) {
                return new GradingOwner(null, List.of());
            }
            return byAttempt.computeIfAbsent(response.get().getTestAttemptId(), this::ownerOfAttempt);
        }

        private GradingOwner ownerOfAttempt(Integer attemptId) {
            TestAttempt attempt = attemptRepository.findById(attemptId).orElse(null);
            if (attempt == null) {
                return new GradingOwner(null, List.of());
            }
            Integer teacher = assignmentRepository.findById(attempt.getTestAssignmentId())
                    .flatMap(assignment -> testRepository.findById(assignment.getTestId()))
                    .map(test -> test.getAuthorPersonId())
                    .orElse(null);
            List<Integer> groupIds = groupMembershipRepository.findByPersonIdAndRemovedAtUtcIsNull(attempt.getPersonId())
                    .stream()
                    .filter(membership -> membership.getRole() == GROUP_ROLE_STUDENT)
                    .map(GroupMembership::getGroupId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            return new GradingOwner(teacher, groupIds);
        }

        String personName(Integer personId) {
            if (personId == null) {
                return "Без автора (тесты администратора)";
            }
            return people.computeIfAbsent(personId, id -> personRepository.findById(id)
                    .map(Attribution::fullName)
                    .filter(name -> !name.isBlank())
                    .orElse("Пользователь удалён"));
        }

        String groupName(Integer groupId) {
            return groups.computeIfAbsent(groupId, id -> groupRepository.findById(id)
                    .map(group -> group.getName())
                    .orElse("Группа удалена"));
        }

        private static String fullName(Person person) {
            return Stream.of(person.getLastName(), person.getFirstName())
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining(" "));
        }
    }

    private record GradingOwner(Integer teacherPersonId, List<Integer> groupIds) {
    }

    private static final class Row {
        private long jobs;
        private long failed;
        private long inputTokens;
        private long outputTokens;
        private long ocrPages;
        private long images;

        void add(AiJob job) {
            jobs++;
            if (job.getStatus() == AiJobStatus.FAILED) {
                failed++;
            }
            inputTokens += job.getInputTokens();
            outputTokens += job.getOutputTokens();
            ocrPages += job.getOcrPages();
            images += job.getImagesGenerated();
        }

        Usage usage() {
            return new Usage(jobs, failed, inputTokens, outputTokens, inputTokens + outputTokens, ocrPages, images);
        }
    }

    public record Usage(long jobs, long failed, long inputTokens, long outputTokens, long tokens, long ocrPages, long images) {
    }

    public record TypeUsage(String type, String title, Usage usage) {
    }

    public record UserUsage(Integer personId, String name, Usage usage) {
    }

    public record GroupUsage(Integer groupId, String name, Usage usage) {
    }

    public record Report(LocalDate from,
                         LocalDate to,
                         String provider,
                         Usage total,
                         long monthlyTokenLimit,
                         long monthTokens,
                         List<TypeUsage> byType,
                         List<UserUsage> byUser,
                         List<GroupUsage> byGroup) {
    }

}
