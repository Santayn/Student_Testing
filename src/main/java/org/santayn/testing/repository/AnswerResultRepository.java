package org.santayn.testing.repository;

import org.santayn.testing.models.answer.AnswerResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnswerResultRepository extends JpaRepository<AnswerResult, Long> {


    List<AnswerResult> findByTestId(Integer testId);
    List<AnswerResult> findByStudentId(Integer studentId);


    @Query("""
        SELECT ar 
        FROM AnswerResult ar 
        WHERE ar.studentId IN (
            SELECT s.id 
            FROM Student s 
            JOIN s.groupStudents gs 
            JOIN gs.group g 
            WHERE g.id = :groupId
        )
        """)
    List<AnswerResult> findByGroupId(@Param("groupId") Integer groupId);

    @Query("""
        SELECT ar 
        FROM AnswerResult ar 
        WHERE ar.studentId = :studentId 
          AND ar.studentId IN (
            SELECT s.id 
            FROM Student s 
            JOIN s.groupStudents gs 
            JOIN gs.group g 
            WHERE g.id = :groupId
          )
        """)
    List<AnswerResult> findByGroupIdAndStudentId(
            @Param("groupId") Integer groupId,
            @Param("studentId") Integer studentId);

    @Query("""
        SELECT ar 
        FROM AnswerResult ar 
        WHERE ar.testId = :testId 
          AND ar.studentId = :studentId 
          AND ar.studentId IN (
            SELECT s.id 
            FROM Student s 
            JOIN s.groupStudents gs 
            JOIN gs.group g 
            WHERE g.id = :groupId
          )
        """)
    List<AnswerResult> findByGroupStudentAndTest(
            @Param("groupId") Integer groupId,
            @Param("studentId") Integer studentId,
            @Param("testId") Integer testId);
}