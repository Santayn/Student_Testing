package org.santayn.testing.сonfig;

import lombok.RequiredArgsConstructor;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.RoleRepository;
import org.santayn.testing.repository.StudentRepository;
import org.santayn.testing.repository.TeacherRepository;
import org.santayn.testing.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Загружаем тестовые данные...");

        createRoleIfNotExists("USER");
        createRoleIfNotExists("STUDENT");
        createRoleIfNotExists("TEACHER");
        createRoleIfNotExists("ADMIN");

        Role studentRole = roleRepository.findByName("STUDENT").orElseThrow();
        Role teacherRole = roleRepository.findByName("TEACHER").orElseThrow();
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        // 1. Создаём студента (User)
        if (userRepository.findByLogin("student").isEmpty()) {
            User user = new User();
            user.setLogin("student");
            user.setPassword(passwordEncoder.encode("student"));
            user.setFirstName("Student");
            user.setLastName("Last");
            user.setPhoneNumber("98348938");
            user.setRole(studentRole);
            userRepository.save(user); // после save() появится ID
        }
        User studentUser = userRepository.findByLogin("student").orElseThrow();
        if (studentUser.getStudent() == null) {
            Student student = new Student();
            student.setUser(studentUser); // связь
            studentRepository.save(student);

            studentUser.setStudent(student); // двусторонняя связь
            userRepository.save(studentUser);
        }

        // 2. Создаём преподавателя
        if (userRepository.findByLogin("teacher").isEmpty()) {
            User user = new User();
            user.setLogin("teacher");
            user.setPassword(passwordEncoder.encode("teacher"));
            user.setFirstName("Teacher");
            user.setLastName("Teacherov");
            user.setPhoneNumber("98348938");
            user.setRole(teacherRole);
            userRepository.save(user);
        }
        User teacherUser = userRepository.findByLogin("teacher").orElseThrow();
        if (teacherUser.getTeacher() == null) {
            Teacher teacher = new Teacher();
            teacher.setUser(teacherUser); // связь
            teacherRepository.save(teacher);
            teacherUser.setTeacher(teacher); // двусторонняя связь
            userRepository.save(teacherUser);
        }

        // 3. Создаём admin
        if (userRepository.findByLogin("admin").isEmpty()) {
            User user = new User();
            user.setLogin("admin");
            user.setPassword(passwordEncoder.encode("admin"));
            user.setFirstName("admin");
            user.setLastName("admin");
            user.setPhoneNumber("98348938");
            user.setRole(adminRole);
            userRepository.save(user);
        }

        // 4. Привязываем Teacher к созданному User
        User adminUser = userRepository.findByLogin("admin").orElseThrow();
        if (adminUser.getTeacher() == null) {
            Teacher teacher = new Teacher();
            teacher.setUser(adminUser); // связь
            teacherRepository.save(teacher);
            adminUser.setTeacher(teacher); // двусторонняя связь
            userRepository.save(adminUser);
        }
    }

    private void createRoleIfNotExists(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role();
            role.setName(roleName);
            roleRepository.save(role);
        }
    }
}