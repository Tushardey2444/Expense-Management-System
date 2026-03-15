package com.manage_expense.config;

import com.manage_expense.entities.Role;
import com.manage_expense.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RoleSeeder implements ApplicationRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Role role1 = roleRepository.findByRoleName("ROLE_"+ AppConstants.ROLE_ADMIN).orElse(
                Role.builder()
                        .roleName("ROLE_"+AppConstants.ROLE_ADMIN)
                        .build()
        );

        Role role2 = roleRepository.findByRoleName("ROLE_"+ AppConstants.ROLE_USER).orElse(
                Role.builder()
                        .roleName("ROLE_"+AppConstants.ROLE_USER)
                        .build()
        );

        roleRepository.saveAll(List.of(role1,role2));
    }
}
