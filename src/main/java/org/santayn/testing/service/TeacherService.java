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

    public TeacherService(SubjectRepository subjectRepository,
                          Teacher_SubjectRepository teacherSubjectRepository,
                          TeacherRepository teacherRepository,
                          GroupRepository groupRepository,
                          Teacher_GroupRepository teacherGroupRepository) {
        this.teacherRepository = teacherRepository;
        this.groupRepository = groupRepository;
        this.teacherGroupRepository = teacherGroupRepository;
        this.subjectRepository = subjectRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
    }

    public List<Teacher> getAllTeacher() {
        return teacherRepository.findAllTeachers();
    }

    /** ГРУППЫ ПРЕПОДАВАТЕЛЯ (по ID) */
    public List<Group> getGroupsByTeacherID(Integer teacherId) {
        if (teacherId == null) throw new IllegalArgumentException("teacherId не может быть null");
        System.out.println("Fetching Groups for teacher ID: " + teacherId);
        List<Group> groups = groupRepository.findGroupByTeacherId(teacherId);
        System.out.println("Groups found: " + groups.size());
        return groups;
    }

    /** ГРУППЫ ПРЕПОДАВАТЕЛЯ (по сущности) */
    public List<Group> getGroupsByTeacher(Teacher teacher) {
        return getGroupsByTeacherID(teacher.getId());
    }

    /** Свободные преподаватели (пример твоего кода) */
    public List<Teacher> findFreeTeacherss() {
        List<Teacher> teacher = teacherRepository.findTeachersNotInAnyGroup();
        System.out.println("Free teachers found: " + teacher.size());
        return teacher;
    }

    public Teacher getTeacherById(Integer teacherId) {
        return teacherRepository.findTeacherById(teacherId)
                .orElseThrow(() -> new RuntimeException("Преподаватель не найден"));
    }

    /** ПРИКРЕПИТЬ ГРУППЫ К ПРЕПОДАВАТЕЛЮ (без дублей) */
    @Transactional
    public Teacher addGroupsToTeacher(Integer teacherId, List<Integer> groupIds) {
        Teacher teacher = getTeacherById(teacherId);

        List<Group> groups = groupRepository.findAllById(groupIds);
        if (groups.size() != groupIds.size()) throw new RuntimeException("Не все группы найдены");

        for (Group group : groups) {
            // избегаем дублей
            boolean exists = teacherGroupRepository
                    .findByTeacherIdAndGroupId(teacherId, group.getId())
                    .isPresent();
            if (!exists) {
                Teacher_Group link = new Teacher_Group();
                link.setTeacher(teacher);
                link.setGroup(group);
                teacherGroupRepository.save(link);
            }
        }
        return teacher;
    }

    /** ОТКРЕПИТЬ ГРУППЫ ОТ ПРЕПОДАВАТЕЛЯ (bulk) */
    @Transactional
    public Teacher deleteGroupsFromTeacher(Integer teacherId, List<Integer> groupIds) {
        Teacher teacher = getTeacherById(teacherId);
        if (groupIds == null || groupIds.isEmpty()) return teacher;

        teacherGroupRepository.deleteByTeacherIdAndGroupIdIn(teacherId, groupIds);
        return teacher;
    }

    /** ПРЕДМЕТЫ — оставил твою логику, слегка подчистил тексты ошибок */
    @Transactional
    public Teacher addSubjectsToTeacher(Integer teacherId, List<Integer> subjectIds) {
        Teacher teacher = getTeacherById(teacherId);
        List<Subject> subjects = subjectRepository.findAllById(subjectIds);
        if (subjects.size() != subjectIds.size()) throw new RuntimeException("Не все предметы найдены");
        for (Subject subject : subjects) {
            Teacher_Subject link = new Teacher_Subject();
            link.setTeacher(teacher);
            link.setSubject(subject);
            teacherSubjectRepository.save(link);
        }
        return teacher;
    }

    @Transactional
    public Teacher deleteSubjectsFromTeacher(Integer teacherId, List<Integer> subjectIds) {
        Teacher teacher = getTeacherById(teacherId);
        for (Integer subjectId : subjectIds) {
            Teacher_Subject link = teacherSubjectRepository
                    .findByTeacherIdAndSubjectId(teacherId, subjectId)
                    .orElseThrow(() -> new RuntimeException("Связь преподавателя с предметом не найдена"));
            teacherSubjectRepository.delete(link);
        }
        return teacher;
    }

    /** Удаление препода и связей */
    @Transactional
    public void deleteTeacherAndRelatedData(Integer teacherId, Integer userId) {
        teacherGroupRepository.deleteByTeacherId(teacherId);
        teacherSubjectRepository.deleteByTeacherId(teacherId);
        teacherRepository.deleteByUser_Id(userId);
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
