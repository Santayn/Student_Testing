package org.santayn.testing.models.user;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.santayn.testing.models.student.Student;
import org.santayn.testing.models.role.Role;
import org.santayn.testing.models.teacher.Teacher;

@Data
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    String password;
    String login;

    @Setter
    @ManyToOne // Связь "многие к одному" (много пользователей могут иметь одну роль)
    @JoinColumn(name = "role_id") // Создаст внешний ключ ID_Role в таблице users
    Role role;

    String firstName;
    String lastName;
    String phoneNumber;

    // Связь OneToOne с Student
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Student student;
    // Связь OneToOne с Student
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    Teacher teacher;

    public void setUsername(String teacher) {

    }
}