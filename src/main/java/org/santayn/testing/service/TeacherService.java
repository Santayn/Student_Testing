package org.santayn.testing.service;
import jakarta.transaction.Transactional;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.teacher.Teacher_Group;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.GroupRepository;
import org.santayn.testing.repository.TeacherRepository;
import org.santayn.testing.repository.Teacher_GroupRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class TeacherService {
    private final TeacherRepository teacherRepository;
    private final GroupRepository groupRepository;
    private final Teacher_GroupRepository teacherGroupRepository;
    public TeacherService(TeacherRepository teacherRepository,GroupRepository groupRepository,Teacher_GroupRepository teacherGroupRepository) {
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.teacherGroupRepository = teacherGroupRepository;
    }
    public List<Teacher> findAll() {
        return teacherRepository.findAllTeachers();
    }
    public Optional<Teacher> findById(Integer id) {
        return teacherRepository.findTeacherById(id);
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
        // 2. Получаем список group
        List<Group> groups = groupRepository.findGroupByTeacherId(teacherId);
        if (groups.size() != groupIds.size()) {
            throw new RuntimeException("Не все groups найдены");
        }
        for (Group group : groups) {
            // Находим конкретную связь "Teacher_Group"
            Teacher_Group existingLink = teacherGroupRepository.findByTeacherIdAndGroupId(teacherId, group.getId())
                    .orElseThrow(() -> new RuntimeException("Связь между группой и студентом не найдена"));
            // Удаляем эту связь
            teacherGroupRepository.delete(existingLink);
        }
        // 4. Опционально: можно вернуть обновлённую группу (если нужно)
        return teacher;
    }

    @Transactional
    public void deleteTeacherAndRelatedData(Integer teacherId, Integer userId) {

        // 2. Удаляем связь студента с группами
        teacherGroupRepository.deleteByTeacherId(teacherId);

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
}