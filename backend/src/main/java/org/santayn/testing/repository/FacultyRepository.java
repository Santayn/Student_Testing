package org.santayn.testing.repository;

import jakarta.persistence.LockModeType;
import org.santayn.testing.models.faculty.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Integer> {

    Optional<Faculty> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select faculty from Faculty faculty where faculty.id = :id")
    Optional<Faculty> findByIdForUpdate(@Param("id") Integer id);

    boolean existsByCode(String code);

    @Query("""
            select faculty
            from Faculty faculty
            where exists (
                select 1
                from FacultySubject facultySubject
                where facultySubject.facultyId = faculty.id
                  and facultySubject.subjectId = :subjectId
            )
            """)
    List<Faculty> findBySubjectId(@Param("subjectId") Integer subjectId);
}
