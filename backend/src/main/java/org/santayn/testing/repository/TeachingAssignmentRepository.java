package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.teacher.TeachingAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeachingAssignmentRepository extends JpaRepository<TeachingAssignment, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select assignment from TeachingAssignment assignment where assignment.id = :id")
    Optional<TeachingAssignment> findByIdForUpdate(@Param("id") Integer id);

    List<TeachingAssignment> findByGroupId(Integer groupId);

    boolean existsByGroupId(Integer groupId);

    List<TeachingAssignment> findByGroupIdIn(Collection<Integer> groupIds);

    List<TeachingAssignment> findBySubjectMembershipId(Integer subjectMembershipId);

    List<TeachingAssignment> findBySubjectMembershipIdIn(Collection<Integer> subjectMembershipIds);

    @Query("""
            select assignment
            from TeachingAssignment assignment
            where assignment.subjectMembershipId in :subjectMembershipIds
              and (:studyCourse is null or assignment.studyCourse = :studyCourse)
              and assignment.semester = :semester
              and assignment.academicYear = :academicYear
            order by assignment.subjectMembershipId, assignment.groupId, assignment.id
            """)
    List<TeachingAssignment> findTeacherWorkload(@Param("subjectMembershipIds") Collection<Integer> subjectMembershipIds,
                                                  @Param("studyCourse") Integer studyCourse,
                                                  @Param("semester") int semester,
                                                  @Param("academicYear") int academicYear);

    @Query("""
            select count(assignment) > 0
            from TeachingAssignment assignment
            where assignment.subjectMembership.subjectId = :subjectId
              and assignment.groupId = :groupId
              and assignment.studyCourse = :studyCourse
              and assignment.academicYear = :academicYear
              and assignment.semester = :semester
            """)
    boolean existsBySubjectIdAndGroupIdAndStudyCourseAndAcademicYearAndSemester(@Param("subjectId") Integer subjectId,
                                                                                 @Param("groupId") Integer groupId,
                                                                                 @Param("studyCourse") Integer studyCourse,
                                                                                 @Param("academicYear") int academicYear,
                                                                                 @Param("semester") int semester);

    @Query("""
            select count(assignment) > 0
            from TeachingAssignment assignment
            where assignment.subjectMembership.subjectId = :subjectId
              and assignment.groupId = :groupId
              and assignment.studyCourse = :studyCourse
              and assignment.academicYear = :academicYear
              and assignment.semester = :semester
              and assignment.id <> :assignmentId
            """)
    boolean existsBySubjectIdAndGroupIdAndStudyCourseAndAcademicYearAndSemesterAndIdNot(@Param("subjectId") Integer subjectId,
                                                                                         @Param("groupId") Integer groupId,
                                                                                         @Param("studyCourse") Integer studyCourse,
                                                                                         @Param("academicYear") int academicYear,
                                                                                         @Param("semester") int semester,
                                                                                         @Param("assignmentId") Integer assignmentId);

    @Query("""
            select assignment
            from TeachingAssignment assignment
            where (:groupId is null or assignment.groupId = :groupId)
              and (:subjectMembershipId is null or assignment.subjectMembershipId = :subjectMembershipId)
              and (:courseVersionId is null or assignment.courseVersionId = :courseVersionId)
              and (:facultyId is null or assignment.group.facultyId = :facultyId)
              and (:loadTypeId is null or assignment.loadTypeId = :loadTypeId)
              and (:studyCourse is null or assignment.studyCourse = :studyCourse)
              and (:semester is null or assignment.semester = :semester)
              and (:academicYear is null or assignment.academicYear = :academicYear)
              and (:status is null or assignment.status = :status)
            """)
    List<TeachingAssignment> findByFilters(@Param("groupId") Integer groupId,
                                           @Param("subjectMembershipId") Integer subjectMembershipId,
                                           @Param("courseVersionId") Integer courseVersionId,
                                           @Param("facultyId") Integer facultyId,
                                           @Param("loadTypeId") Integer loadTypeId,
                                           @Param("studyCourse") Integer studyCourse,
                                           @Param("semester") Integer semester,
                                           @Param("academicYear") Integer academicYear,
                                           @Param("status") Integer status);

    @Query("""
            select count(assignment) > 0
            from TeachingAssignment assignment
            where assignment.group.facultyId = :facultyId
              and assignment.subjectMembership.subjectId = :subjectId
            """)
    boolean existsByFacultyIdAndSubjectId(@Param("facultyId") Integer facultyId,
                                          @Param("subjectId") Integer subjectId);
}
