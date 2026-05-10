package org.santayn.testing.repository;

import org.santayn.testing.models.lecture.LectureMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LectureMaterialRepository extends JpaRepository<LectureMaterial, Integer> {

    List<LectureMaterial> findByCourseLectureIdOrderByIdAsc(Integer courseLectureId);

    Optional<LectureMaterial> findByIdAndCourseLectureId(Integer id, Integer courseLectureId);
}
