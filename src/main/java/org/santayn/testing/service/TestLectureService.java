package org.santayn.testing.service;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.test.Test_Lecture;
import org.santayn.testing.repository.Test_LectureRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TestLectureService {

    private final Test_LectureRepository testLectureRepository;

    public void save(Test_Lecture testLecture) {
        testLectureRepository.save(testLecture);
    }
}