package org.santayn.testing.service;

import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final Group_StudentRepository groupStudentRepository;

    public GroupService(GroupRepository groupRepository,
                        StudentRepository studentRepository,
                        Group_StudentRepository groupStudentRepository) {
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
        this.groupStudentRepository = groupStudentRepository;
    }

    public List<Group> getAllGroup() {
        return groupRepository.findAllGroups();
    }

    public Group getSpecificGroupByGroupName(String name) {
        List<Group> groups = groupRepository.findGroupByName(name);
        return groups.stream()
                .filter(group -> group.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Группа с именем " + name + " не найдена."));
    }

    public void save(Group group) {
        groupRepository.save(group);
    }

    public Group getGroupById(Integer groupId) {
        return groupRepository.findGroupById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));
    }

    public List<Group> findFreeGroups() {
        List<Group> groups = groupRepository.findGroupsNotInAnyTeachers();
        System.out.println("Free Groups found: " + groups.size());
        return groups;
    }

    public List<Group> getGroupsByTecherID(Integer teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId не может быть null");
        }
        System.out.println("Fetching Groups for teacher ID: " + teacherId);
        List<Group> groups = groupRepository.findGroupByTeacherId(teacherId);
        System.out.println("Groups found: " + groups.size());
        return groups;
    }

    @Transactional
    public Group addStudentsToGroup(Integer groupId, List<Integer> studentIds) {
        Group group = getGroupById(groupId);
        List<Student> students = studentRepository.findAllById(studentIds);

        if (students.size() != studentIds.size()) {
            throw new RuntimeException("Не все студенты найдены");
        }

        for (Student student : students) {
            // 1. Устанавливаем группу у студента
            student.setGroup(group);
            studentRepository.save(student); // Сохраняем изменения

            // 2. Создаём запись в group_student
            Group_Student groupStudent = new Group_Student();
            groupStudent.setGroup(group);
            groupStudent.setStudent(student);
            groupStudentRepository.save(groupStudent);
        }

        return group;
    }

    @Transactional
    public Group deleteStudentsFromGroup(Integer groupId, List<Integer> studentIds) {
        Group group = getGroupById(groupId);
        List<Student> students = studentRepository.findAllById(studentIds);

        if (students.size() != studentIds.size()) {
            throw new RuntimeException("Не все студенты найдены");
        }

        for (Student student : students) {
            Optional<Group_Student> existingLinkOpt = groupStudentRepository.findByGroupIdAndStudentId(groupId, student.getId());

            existingLinkOpt.ifPresent(groupStudentRepository::delete);

            // Очищаем ссылку на группу у студента
            if (student.getGroup() != null && student.getGroup().getId().equals(groupId)) {
                student.setGroup(null);
                studentRepository.save(student);
            }
        }

        return group;
    }
}