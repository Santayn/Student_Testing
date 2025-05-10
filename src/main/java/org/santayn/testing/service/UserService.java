package org.santayn.testing.service;

import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Метод для получения пользователя по имени
    public User getUserByUsername(String username) {
        return userRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + username));
    }
}