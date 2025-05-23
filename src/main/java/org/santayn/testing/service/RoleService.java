package org.santayn.testing.service;

import org.santayn.testing.models.role.Role;
import org.santayn.testing.repository.RoleRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }
    // Получить роль по ID
    public Role getRoleById(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Роль не найдена"));
    }

    public List<Role> findAll() {
        return roleRepository.findAll();
    }
}