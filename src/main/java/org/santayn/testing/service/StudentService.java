package org.santayn.testing.service;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.repository.StudentRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
@Service
public class StudentService {
    private final StudentRepository studentRepository;
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }
    public List<Student> findAll() {
        return studentRepository.findAllStudents();
    }
    public Optional<Student> findById(Integer id) {
        return studentRepository.findStudentById(id);
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
}