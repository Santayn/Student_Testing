package org.santayn.testing.repository;

import org.santayn.testing.models.faculty.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, Integer> {

   // Получение всех факультетов
   @Query("SELECT f FROM Faculty f")
   List<Faculty> findAllFacultys();

   // Очистка faculty_id в таблице app_group
   @Modifying
   @Transactional
   @Query(value = "UPDATE app_group SET faculty_id = NULL WHERE faculty_id = :facultyId", nativeQuery = true)
   void clearFacultyIdFromAppGroup(@Param("facultyId") Integer facultyId);

   @Modifying
   @Transactional
   @Query(value = "DELETE FROM subject_faculty WHERE faculty_id = :facultyId", nativeQuery = true)
   void deleteSubjectFacultyByFacultyId(@Param("facultyId") Integer facultyId);

   @Modifying
   @Transactional
   @Query(value = "DELETE FROM faculty WHERE id = :facultyId", nativeQuery = true)
   void deleteFacultyById(@Param("facultyId") Integer facultyId);
}