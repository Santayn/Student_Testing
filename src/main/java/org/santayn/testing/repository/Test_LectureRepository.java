package org.santayn.testing.repository;

import org.santayn.testing.models.test.Test_Lecture;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Test_LectureRepository extends JpaRepository<Test_Lecture, Integer> {
}