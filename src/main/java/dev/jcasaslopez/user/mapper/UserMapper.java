package dev.jcasaslopez.user.mapper;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.entity.User;

@Component
public class UserMapper {
	
	@Autowired
	private RoleMapper roleMapper;

	public UserDto userToUserDtoMapper(User user) {
		return new UserDto(user.getIdUser(),
				user.getUsername(),
				user.getPassword(),
				user.getFullName(),
				user.getEmail(),
				user.getDateOfBirth(),
				user.getRoles().stream()
						.map(rol -> roleMapper.roleToRoleDtoMapper(rol))
						.collect(Collectors.toSet()),
				user.getAccountStatus()
				);
	}
	
	public User userDtoToUserMapper(UserDto user) {
		return new User(user.getUsername(),
				user.getPassword(),
				user.getFullName(),
				user.getEmail(),
				user.getDateOfBirth(),
				user.getRoles().stream()
						.map(rol -> roleMapper.roleDtoToRoleMapper(rol))
						.collect(Collectors.toSet()),
				user.getAccountStatus()
				);
	}
}
