package dev.jcasaslopez.user.entity;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import dev.jcasaslopez.user.enums.RoleName;
import dev.jcasaslopez.user.repository.RoleRepository;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UniquenessNameRoleTest {
	
	@Autowired private RoleRepository roleRepository;
	
	@Test
	@DisplayName("Role entity throws exception when 2 roles have the same name")
	void roleEntity_WhenNameUnicityViolated_ShouldThrowException() {
	    Role role2 = new Role(RoleName.ROLE_USER); 

	    assertThrows(DataIntegrityViolationException.class, () -> {
	        roleRepository.save(role2);
	    });
	}
}
