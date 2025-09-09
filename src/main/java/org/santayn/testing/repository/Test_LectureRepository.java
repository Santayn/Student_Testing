package org.santayn.testing.repository;

import org.santayn.testing.models.test.Test;
import org.santayn.testing.models.test.Test_Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface Test_LectureRepository extends JpaRepository<Test_Lecture, Integer> {
    @Query("select tl.test from Test_Lecture tl where tl.lecture.id = :lectureId")
    List<Test> findTestsByLectureId(@Param("lectureId") Integer lectureId);
}