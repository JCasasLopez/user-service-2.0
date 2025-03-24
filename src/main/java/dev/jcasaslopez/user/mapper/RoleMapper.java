package dev.jcasaslopez.user.mapper;

import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.dto.RoleDto;
import dev.jcasaslopez.user.entity.Role;

@Component
public class RoleMapper {
	
	public RoleDto roleToRoleDtoMapper(Role role) {
		return new RoleDto(role.getRoleName());
	}
	
	public Role roleDtoToRoleMapper(RoleDto role) {
		return new Role(role.getRoleName());
	}

}
