package org.santayn.testing.service;

import org.santayn.testing.models.teacher.Teacher;
import org.santayn.testing.repository.LectureRepository;
import org.santayn.testing.repository.SubjectRepository;
import org.santayn.testing.repository.TeacherRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class UserSearch {
    private final TeacherRepository teacherRepository;

    public UserSearch(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;

    }

    public Teacher getCurrentTeacher() {
        // Получаем объект Authentication из SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Проверяем, что пользователь аутентифицирован
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User is not authenticated");
        }

        // Получаем имя пользователя (логин)
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();

        // Ищем учителя по логину в базе данных
        return teacherRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Teacher not found for user: " + username));
    }
}
