package org.santayn.testing.service;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.repository.*;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final Group_StudentRepository groupStudentRepository;
    public GroupService(GroupRepository groupRepository, StudentRepository studentRepository,Group_StudentRepository groupStudentRepository) {
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
        this.groupStudentRepository = groupStudentRepository;
    }
    public List<Group> getAllGroup() {
        return groupRepository.findAllGroups();
    }
    public Group getSpecificGroupByGroupName(String name) {
        List<Group> groups = groupRepository.findGroupByName(name);
        if (groups.isEmpty()) {
            throw new RuntimeException("Группа с именем " + name + " не найдена.");
        }
        Optional<Group> specificGroup = groups.stream()
                .filter(group -> group.getName().equals(name))
                .findFirst();
        return specificGroup.orElseThrow(() -> new RuntimeException(
                "Группа с именем " + name + " не найдена."));
    }
    public void save(Group group) {
        groupRepository.save(group);
    }
    public Group getGroupById(Integer groupId) {
        return groupRepository.findGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
    }
    // Получение свободных group
    public List<Group> findFreeGroups() {
        List<Group> group = groupRepository.findGroupsNotInAnyTeachers();
        System.out.println("Free Students found: " + group.size());
        return group;
    }
    // Получение списка студентов по ID группы
    public List<Group> getGroupsByTecherID(Integer teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId cannot be null");
        }
        System.out.println("Fetching Group for group ID: " + teacherId); // Логирование
        List<Group> groups = groupRepository.findGroupByTeacherId(teacherId);
        System.out.println("Group found: " + groups.size()); // Проверка количества найденных студентов
        return groups;
    }
    public Group addStudentsToGroup(Integer groupId, List<Integer> studentIds) {
        // 1. Получаем группу
        Group group = getGroupById(groupId);
        // 2. Получаем список студентов
        List<Student> students = studentRepository.findAllById(studentIds);
        if (students.size() != studentIds.size()) {
            throw new RuntimeException("Не все студенты найдены");
        }
        // 3. Создаём связи между студентами и группой
        for (Student student : students) {
            Group_Student studentGroup = new Group_Student();
            studentGroup.setGroup(group);
            studentGroup.setStudent(student);

            // ❗ Сохраняем связь НАПРЯМУЮ в БД
            groupStudentRepository.save(studentGroup);
        }
        // 4. Опционально: можно вернуть обновлённую группу (если нужно)
        return group;
    }
    public Group deleteStudentsFromGroup(Integer groupId, List<Integer> studentIds) {
        // 1. Получаем группу
        Group group = getGroupById(groupId);
        // 2. Получаем список студентов
        List<Student> students = studentRepository.findStudentByGroupId(groupId);
        if (students.size() != studentIds.size()) {
            throw new RuntimeException("Не все студенты найдены");
        }
        for (Student student : students) {
            // Находим конкретную связь "группа - студент"
            Group_Student existingLink = groupStudentRepository.findByGroupIdAndStudentId(groupId, student.getId())
                    .orElseThrow(() -> new RuntimeException("Связь между группой и студентом не найдена"));
            // Удаляем эту связь
            groupStudentRepository.delete(existingLink);
        }
        // 4. Опционально: можно вернуть обновлённую группу (если нужно)
        return group;
    }
}