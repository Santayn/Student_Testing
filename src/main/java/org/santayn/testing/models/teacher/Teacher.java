    package org.santayn.testing.models.teacher;

    import jakarta.persistence.*;
    import lombok.Data;
    import org.hibernate.annotations.OnDelete;
    import org.hibernate.annotations.OnDeleteAction;
    import org.santayn.testing.models.faculty.Faculty;
    import org.santayn.testing.models.faculty.Faculty_Teacher;
    import org.santayn.testing.models.group.Group;
    import org.santayn.testing.models.user.User;
    import org.springframework.boot.autoconfigure.web.WebProperties;

    import java.util.List;

    @Table
    @Entity
    @Data
    public class Teacher {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        Integer id;
        @OneToOne
        @OnDelete(action = OnDeleteAction.CASCADE)
        @JoinColumn(name = "user_id", referencedColumnName = "id")
        private User user;

        @OneToOne
        @JoinColumn(name = "id_faculty", referencedColumnName = "id")
        private Faculty faculty;
        Integer group_id;
        // Связь OneToMany с Teacher_Group
        @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Teacher_Group> teacherGroups;

        // Связь OneToMany с Faculty_Teacher
        @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Faculty_Teacher> facultyTeachers;

        // Связь OneToMany с Teacher_Subject
        @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<Teacher_Subject> teacherSubjects;

    }
