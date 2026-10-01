package org.santayn.testing.service;

import org.junit.jupiter.api.Test;
import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.lecture.LectureTestLink;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.LectureTestLinkRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LectureTestLinkServiceBatchIntegrationTests {

    @Autowired private LectureTestLinkService lectureTestLinkService;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private LectureTestLinkRepository lectureTestLinkRepository;
    @Autowired private TestRepository testRepository;

    @Test
    void loadsLinkedTestsForManyLecturesWithOneBatchContract() {
        Lecture firstLecture = createLecture("Batch lecture one");
        Lecture secondLecture = createLecture("Batch lecture two");
        org.santayn.testing.models.test.Test firstTest = createTest("Batch test one");
        org.santayn.testing.models.test.Test secondTest = createTest("Batch test two");

        createLink(firstLecture.getId(), firstTest.getId());
        createLink(firstLecture.getId(), secondTest.getId());
        createLink(secondLecture.getId(), secondTest.getId());

        var result = lectureTestLinkService.findTestsByLectureIds(
                List.of(firstLecture.getId(), secondLecture.getId())
        );

        assertThat(result).containsKeys(firstLecture.getId(), secondLecture.getId());
        assertThat(result.get(firstLecture.getId()))
                .extracting(org.santayn.testing.models.test.Test::getId)
                .containsExactly(firstTest.getId(), secondTest.getId());
        assertThat(result.get(secondLecture.getId()))
                .extracting(org.santayn.testing.models.test.Test::getId)
                .containsExactly(secondTest.getId());
    }

    private Lecture createLecture(String title) {
        Lecture lecture = new Lecture();
        lecture.setOrdinal(1);
        lecture.setTitle(title + " " + System.nanoTime());
        lecture.setContentFolderKey("batch-" + System.nanoTime());
        lecture.setPublicVisible(true);
        return lectureRepository.saveAndFlush(lecture);
    }

    private org.santayn.testing.models.test.Test createTest(String title) {
        org.santayn.testing.models.test.Test test = new org.santayn.testing.models.test.Test();
        test.setTitle(title + " " + System.nanoTime());
        test.setAttemptsAllowed(1);
        test.setQuestionCount(1);
        return testRepository.saveAndFlush(test);
    }

    private void createLink(Integer lectureId, Integer testId) {
        LectureTestLink link = new LectureTestLink();
        link.setCourseLectureId(lectureId);
        link.setTestId(testId);
        lectureTestLinkRepository.saveAndFlush(link);
    }
}
