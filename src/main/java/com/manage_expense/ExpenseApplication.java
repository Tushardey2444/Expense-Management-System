package com.manage_expense;

import com.manage_expense.config.AppConstants;
import com.manage_expense.entities.Role;
import com.manage_expense.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@EnableScheduling
public class ExpenseApplication implements CommandLineRunner {

	@Autowired
	private RoleRepository roleRepository;

	public static void main(String[] args) {
		SpringApplication.run(ExpenseApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
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
