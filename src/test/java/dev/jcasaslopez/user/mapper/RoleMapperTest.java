package dev.jcasaslopez.user.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import dev.jcasaslopez.user.dto.RoleDto;
import dev.jcasaslopez.user.entity.Role;
import dev.jcasaslopez.user.enums.RoleName;

// Instead of loading the full context with @SpringBootTest, we initialize only  
// RoleMapper and the minimal required configuration.
@ExtendWith(SpringExtension.class)
@Import(RoleMapper.class)
public class RoleMapperTest {

	@Autowired private RoleMapper roleMapper;
	
	@Test
	@DisplayName("roleToRoleDtoMapper() should map Role to RoleDto correctly")
	void roleToRoleDtoMapper_ShouldMapCorrectly() {
	    // Arrange
	    Role role = new Role(RoleName.ROLE_USER);

	    // Act
	    RoleDto roleDto = roleMapper.roleToRoleDtoMapper(role);

	    // Assert
	    assertEquals(role.getRoleName(), roleDto.getRoleName(), "Role names should match");
	}
	
	@Test
	@DisplayName("roleDtoToRoleMapper() should map RoleDto to Role correctly")
	void roleDtoToRoleMapper_ShouldMapCorrectly() {
	    // Arrange
	    RoleDto roleDto = new RoleDto(RoleName.ROLE_ADMIN);

	    // Act
	    Role role = roleMapper.roleDtoToRoleMapper(roleDto);

	    // Assert
	    assertEquals(roleDto.getRoleName(), role.getRoleName(), "Role names should match");
	}
}