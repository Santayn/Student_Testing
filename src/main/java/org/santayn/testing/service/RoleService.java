package org.santayn.testing.service;

import org.santayn.testing.models.role.Role;
import org.santayn.testing.repository.RoleRepository;
import org.springframework.stereotype.Service;
@Service
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role getRoleById(Integer id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Роль не найдена"));
    }
}