package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.repository.TestAssignmentRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class TestServiceAtomicCreateIntegrationTests {

    @Autowired private TestService testService;
    @Autowired private TestRepository testRepository;
    @Autowired private TestAssignmentRepository testAssignmentRepository;

    @Test
    void createsTestAndAssignmentsAsOneCommand() {
        String title = "Atomic create success " + System.nanoTime();
        Instant now = Instant.now();

        TestService.CreatedTestWithAssignments created = testService.createWithAssignments(
                title,
                "Created atomically",
                null,
                2,
                1,
                List.of(),
                null,
                List.of(new TestService.AssignmentInput(
                        1,
                        null,
                        null,
                        null,
                        now.minusSeconds(60),
                        now.plusSeconds(3600),
                        1
                ))
        );

        assertThat(created.test().getId()).isNotNull();
        assertThat(created.assignments()).hasSize(1);
        assertThat(created.assignments().get(0).getTestId()).isEqualTo(created.test().getId());
        assertThat(testRepository.findById(created.test().getId())).isPresent();
        assertThat(testAssignmentRepository.findByTestId(created.test().getId())).hasSize(1);

        testService.delete(created.test().getId());
    }

    @Test
    void invalidAssignmentRollsBackTestAndEarlierAssignments() {
        String title = "Atomic create rollback " + System.nanoTime();
        Instant now = Instant.now();
        long testsBefore = testRepository.count();
        long assignmentsBefore = testAssignmentRepository.count();

        assertThatThrownBy(() -> testService.createWithAssignments(
                title,
                "Must roll back",
                null,
                1,
                1,
                List.of(),
                null,
                List.of(
                        new TestService.AssignmentInput(
                                1,
                                null,
                                null,
                                null,
                                now.minusSeconds(60),
                                now.plusSeconds(3600),
                                1
                        ),
                        new TestService.AssignmentInput(
                                2,
                                Integer.MAX_VALUE,
                                null,
                                null,
                                now.minusSeconds(60),
                                now.plusSeconds(3600),
                                1
                        )
                )
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Course version not found");

        assertThat(testRepository.count()).isEqualTo(testsBefore);
        assertThat(testAssignmentRepository.count()).isEqualTo(assignmentsBefore);
        assertThat(testRepository.findAll())
                .noneMatch(test -> title.equals(test.getTitle()));
    }
}
