
package org.santayn.testing.service;

import org.santayn.testing.models.lecture.Lecture;
import org.santayn.testing.models.question.Question;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.TestRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class LectureService {

    private final LectureRepository lectureRepository;

    public LectureService(LectureRepository lectureRepository) {
        this.lectureRepository = lectureRepository;
    }

    public List<Lecture> getLectureByID(Integer subjectId) {
        if (subjectId == null) {
            throw new IllegalArgumentException("Test ID cannot be null");
        }
        return lectureRepository.findLectureBySubjectId(subjectId);
    }


    public Lecture getSpecificLectureBySubjectIdAndLectureId(Integer subjectId, Integer lectureId) {

        List<Lecture> lectures = lectureRepository.findLectureBySubjectId(subjectId);

        Optional<Lecture> specificLecture = lectures.stream()
                .filter(lecture -> lecture.getId().equals(lectureId))
                .findFirst();

        return specificLecture.orElseThrow(() -> new RuntimeException(
                "Question with ID " + lectureId + " not found in Test with ID " + subjectId));
    }
}