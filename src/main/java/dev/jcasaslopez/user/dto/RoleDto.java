package dev.jcasaslopez.user.dto;

import dev.jcasaslopez.user.enums.RoleName;

public class RoleDto {
	
	private RoleName roleName;

	public RoleDto(RoleName roleName) {
		this.roleName = roleName;
	}

	public RoleDto() {
	}

	public RoleName getRoleName() {
		return roleName;
	}

	public void setRoleName(RoleName roleName) {
		this.roleName = roleName;
	}
	
}
