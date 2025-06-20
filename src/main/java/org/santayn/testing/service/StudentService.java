package org.santayn.testing.service;
import jakarta.transaction.Transactional;
import org.santayn.testing.models.group.Group_Student;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.Group_StudentRepository;
import org.santayn.testing.repository.StudentRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class StudentService {
    private final StudentRepository studentRepository;
    private final Group_StudentRepository groupStudentRepository;
    public StudentService(StudentRepository studentRepository, Group_StudentRepository groupStudentRepository) {
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
        System.out.println("Fetching students for group ID: " + groupId); // Логирование
        List<Student> students = studentRepository.findStudentByGroupId(groupId);
        System.out.println("Students found: " + students.size()); // Проверка количества найденных студентов
        return students;
    }
    // Получение конкретного студента по ID группы и ID студента
    public Student getSpecificStudentByGroupIdAndStudentId(Integer groupId, Integer studentId) {
        if (groupId == null || studentId == null) {
            throw new IllegalArgumentException("Group ID and Student ID cannot be null");
        }

        return studentRepository.findStudentByGroupId(groupId).stream()
                .filter(student -> student.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Студент с ID " + studentId + " не найден в группе с ID " + groupId));
    }
    // Получение свободных студентов (не входящих в указанную группу)
    public List<Student> findFreeStudents() {
        List<Student> student = studentRepository.findStudentsNotInAnyGroup();
        System.out.println("Free Students found: " + student.size());
        return student;
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

        // 2. Удаляем связь студента с группами
        groupStudentRepository.deleteByStudentId(studentId);

        // 3. Удаляем самого студента
        studentRepository.deleteByUser_Id(userId); // или по studentId, если он есть
    }
    @Transactional
    public void createStudentForUser(User user) {
        Student student = new Student();
        student.setUser(user);
        studentRepository.save(student);
    }
}