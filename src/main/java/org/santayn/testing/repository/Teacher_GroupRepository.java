package org.santayn.testing.repository;

import java.util.List;
import java.util.Optional;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface Teacher_GroupRepository extends JpaRepository<Teacher_Group, Long> {

    // поиск конкретной связи
    Optional<Teacher_Group> findByTeacherIdAndGroupId(Integer teacherId, Integer groupId);

    // есть ли связь (для защиты от дублей)
    default boolean existsByTeacherIdAndGroupId(Integer teacherId, Integer groupId) {
        return findByTeacherIdAndGroupId(teacherId, groupId).isPresent();
    }

    // удалить все связи учителя
    @Modifying
    @Query(value = "DELETE FROM teacher_group WHERE teacher_id = :teacherId", nativeQuery = true)
    void deleteByTeacherId(@Param("teacherId") Integer teacherId);

    // получить связи по учителю
    List<Teacher_Group> findByTeacherId(Integer teacherId);

    // группы по логину учителя (оставил твой JPQL)
    @Query("""
        select tg.group
        from Teacher_Group tg
        join tg.teacher t
        join t.user u
        where u.login = :login
    """)
    List<Group> findGroupsByTeacherLogin(@Param("login") String login);

    // bulk-удаление связей учителя по списку групп
    @Modifying
    @Query("DELETE FROM Teacher_Group tg WHERE tg.teacher.id = :teacherId AND tg.group.id IN :groupIds")
    void deleteByTeacherIdAndGroupIdIn(@Param("teacherId") Integer teacherId,
                                       @Param("groupIds") List<Integer> groupIds);
}
