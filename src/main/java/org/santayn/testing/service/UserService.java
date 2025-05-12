package org.santayn.testing.service;

import org.apache.el.stream.Optional;
import org.santayn.testing.models.group.Group;
import org.santayn.testing.models.user.User;
import org.santayn.testing.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<User> findUsers(Integer roleID) {
        List<User> users = userRepository.findUserByRoleId(roleID);
        System.out.println("Free users found: " + users.size());
        return users;
    }
    public User getUserById(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }

    public void save(User user) {
        userRepository.save(user);
    }

}