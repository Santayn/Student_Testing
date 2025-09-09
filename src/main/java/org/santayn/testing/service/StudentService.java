package org.santayn.testing.service;

import lombok.extern.slf4j.Slf4j;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.Group_StudentRepository;
import org.santayn.testing.repository.StudentRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final Group_StudentRepository groupStudentRepository;

    public StudentService(StudentRepository studentRepository,
                          Group_StudentRepository groupStudentRepository) {
        this.studentRepository = studentRepository;
        this.groupStudentRepository = groupStudentRepository;
    }

    public List<Student> findAll() {
        return studentRepository.findAllStudents();
    }

    public Optional<Student> findById(Integer id) {
        return studentRepository.findStudentById(id);
    }

    /**
     * Ищет студента по логину пользователя.
     * Если пользователь не найден — кидает UsernameNotFoundException.
     */
    public Student findByLogin(String login) {
        return studentRepository.findByUserLogin(login)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Студент с логином '" + login + "' не найден")
                );
    }

    // Получение списка студентов по ID группы
    public List<Student> getStudentsByGroupID(Integer groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("Group ID cannot be null");
        }
        log.info("Fetching students for group ID: {}", groupId);
        List<Student> students = studentRepository.findStudentByGroupId(groupId);
        log.info("Students found: {}", students.size());
        return students;
    }

    // Получение конкретного студента по ID группы и ID студента
    public Student getSpecificStudentByGroupIdAndStudentId(Integer groupId, Integer studentId) {
        if (groupId == null || studentId == null) {
            throw new IllegalArgumentException("Group ID and Student ID cannot be null");
        }
        // Текущая реализация через фильтр — ок, но лучше сделать точечный метод репозитория
        return studentRepository.findStudentByGroupId(groupId).stream()
                .filter(student -> student.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Студент с ID " + studentId + " не найден в группе с ID " + groupId));
    }

    // Получение свободных студентов (не входящих в указанную группу)
    public List<Student> findFreeStudents() {
        List<Student> students = studentRepository.findStudentsNotInAnyGroup();
        log.info("Free Students found: {}", students.size());
        return students;
    }

    public void deleteStudent(Integer id) {
        studentRepository.deleteById(id);
    }

    public void save(Student student) {
        studentRepository.save(student);
    }

    public Optional<Student> findByUserId(Integer userId) {
        return studentRepository.findStudentByUserId(userId);
    }

    @Transactional
    public void deleteStudentAndRelatedData(Integer studentId, Integer userId) {
        // 1) Удаляем связи студента с группами
        groupStudentRepository.deleteByStudentId(studentId);
        // 2) Удаляем самого студента по его ID (а не по userId)
        studentRepository.deleteById(studentId);
        // При необходимости — удаляйте и User в соответствующем UserRepository
        log.info("Deleted student {} and related group links (userId provided: {})", studentId, userId);
    }

    @Transactional
    public void createStudentForUser(User user) {
        Student student = new Student();
        student.setUser(user);
        studentRepository.save(student);
    }
    public List<Student> getStudentsByGroupId(Integer groupId) {
        return studentRepository.findByGroup_Id(groupId);
    }
    // === УДОБНЫЕ "обязательные" геттеры — ДОБАВИТЬ В КЛАСС ===

    /** Вернёт студента по id или бросит UsernameNotFoundException. */
    public Student getById(Integer id) {
        return studentRepository.findStudentById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Студент не найден: id=" + id));
    }

    /** Вернёт студента по userId или бросит UsernameNotFoundException. */
    public Student getByUserIdOrThrow(Integer userId) {
        return studentRepository.findStudentByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("Студент не найден по userId=" + userId));
    }

    /** Удобный метод: студент по группе и id, сразу с исключением если нет. */
    public Student getByGroupIdAndStudentIdOrThrow(Integer groupId, Integer studentId) {
        if (groupId == null || studentId == null) {
            throw new IllegalArgumentException("Group ID and Student ID cannot be null");
        }
        return studentRepository.findStudentByGroupId(groupId).stream()
                .filter(st -> st.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Студент с ID " + studentId + " не найден в группе " + groupId));
    }
    // === УДОБНЫЕ "обязательные" геттеры — ДОБАВИТЬ В КЛАСС ===




}
