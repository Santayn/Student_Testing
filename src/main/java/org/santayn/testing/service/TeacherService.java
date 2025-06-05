package org.santayn.testing.service;
import jakarta.transaction.Transactional;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.subject.Subject;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.teacher.Teacher_Subject;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final Teacher_GroupRepository teacherGroupRepository;
    private final SubjectRepository subjectRepository;
    private final Teacher_SubjectRepository teacherSubjectRepository;
    public TeacherService(SubjectRepository subjectRepository,Teacher_SubjectRepository teacherSubjectRepository,TeacherRepository teacherRepository, GroupRepository groupRepository, Teacher_GroupRepository teacherGroupRepository) {
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.teacherGroupRepository = teacherGroupRepository;
        this.subjectRepository = subjectRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
    }



    public List<Teacher> getAllTeacher() {
        return teacherRepository.findAllTeachers();
    }
    // Получение списка студентов по ID группы
    public List<Teacher> getGroupsByTeacherID(Integer teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }
        System.out.println("Fetching groups for teachers ID: " + teacherId); // Логирование
        List<Teacher> teachers = teacherRepository.findGroupByTeacherId(teacherId);
        System.out.println("teachers found: " + teachers.size()); // Проверка количества найденных студентов
        return teachers;
    }

    // Получение свободных teacher (не входящих в указанную группу)
    public List<Teacher> findFreeTeacherss() {
        List<Teacher> teacher = teacherRepository.findTeachersNotInAnyGroup();
        System.out.println("Free teachers found: " + teacher.size());
        return teacher;
    }
    public Teacher getTeacherById(Integer teacherId) {
        return teacherRepository.findTeacherById(teacherId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
    }
    public Teacher addGroupsToTeacher(Integer teacherId, List<Integer> groupIds) {
        // 1. Получаем группу
        Teacher teacher = getTeacherById(teacherId);
        // 2. Получаем список Group
        List<Group> groups = groupRepository.findAllById(groupIds);
        if (groups.size() != groupIds.size()) {
            throw new RuntimeException("Не все group найдены");
        }
        // 3. Создаём связи между group и teacher
        for (Group group : groups) {
            Teacher_Group groupTeacher = new Teacher_Group();
            groupTeacher.setTeacher(teacher);
            groupTeacher.setGroup(group);

            // ❗ Сохраняем связь НАПРЯМУЮ в БД
            teacherGroupRepository.save(groupTeacher);
        }
        // 4. Опционально: можно вернуть обновлённую teacher (если нужно)
        return teacher;
    }
    public Teacher deleteGroupsFromTeacher(Integer teacherId, List<Integer> groupIds) {
        // 1. Получаем teacher
        Teacher teacher = getTeacherById(teacherId);

        for (Integer groupId : groupIds) {
            // Находим конкретную связь "Teacher_Subject"
            Teacher_Group existingLink = teacherGroupRepository.findByTeacherIdAndGroupId(teacherId, groupId)
                    .orElseThrow(() -> new RuntimeException("Связь между предметом и преподавателем не найдена"));

            // Удаляем эту связь
            teacherGroupRepository.delete(existingLink);

        }
        // 4. Опционально: можно вернуть обновлённую группу (если нужно)
        return teacher;
    }



    public Teacher addSubjectsToTeacher(Integer teacherId, List<Integer> subjectIds) {

        Teacher teacher = getTeacherById(teacherId);
        // 2. Получаем список Group
        List<Subject> subjects = subjectRepository.findAllById(subjectIds);
        if (subjects.size() != subjectIds.size()) {
            throw new RuntimeException("Не все subject найдены");
        }
        // 3. Создаём связи между group и teacher
        for (Subject subject : subjects) {
            Teacher_Subject subjectTeacher = new Teacher_Subject();
            subjectTeacher.setTeacher(teacher);
            subjectTeacher.setSubject(subject);

            // ❗ Сохраняем связь НАПРЯМУЮ в БД
            teacherSubjectRepository.save(subjectTeacher);
        }
        // 4. Опционально: можно вернуть обновлённую teacher (если нужно)
        return teacher;
    }
    public Teacher deleteSubjectsFromTeacher(Integer teacherId, List<Integer> subjectIds) {
        // 1. Получаем преподавателя
        Teacher teacher = getTeacherById(teacherId);

        // 2. Для каждого subjectId находим связь "Teacher_Subject" и удаляем её
        for (Integer subjectId : subjectIds) {
            // Находим конкретную связь "Teacher_Subject"
            Teacher_Subject existingLink = teacherSubjectRepository.findByTeacherIdAndSubjectId(teacherId, subjectId)
                    .orElseThrow(() -> new RuntimeException("Связь между предметом и преподавателем не найдена"));

            // Удаляем эту связь
            teacherSubjectRepository.delete(existingLink);
        }

        // 3. Возвращаем обновлённого преподавателя
        return teacher;
    }






    @Transactional
    public void deleteTeacherAndRelatedData(Integer teacherId, Integer userId) {

        // 2. Удаляем связь студента с группами
        teacherGroupRepository.deleteByTeacherId(teacherId);

        teacherSubjectRepository.deleteByTeacherId(teacherId);

        // 3. Удаляем самого студента
        teacherRepository.deleteByUser_Id(userId); // или по studentId, если он есть


    }
    public Optional<Teacher> findByUserId(Integer userId) {
        return teacherRepository.findByUserId(userId);
    }
    @Transactional
    public void createTeacherForUser(User user) {
        Teacher teacher = new Teacher();
        teacher.setUser(user);
        teacherRepository.save(teacher);
    }
    public List<Teacher_Subject> getSubjectsByTeacherId(Integer teacherId) {
        return teacherSubjectRepository.findByTeacherId(teacherId);
    }
}