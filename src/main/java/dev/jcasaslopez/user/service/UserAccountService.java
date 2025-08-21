package dev.jcasaslopez.user.service;

import org.springframework.security.core.userdetails.UserDetails;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;

public interface UserAccountService {

	User findUser(String username);
	User findUserByEmail(String email);
	boolean userExists(String username);
	void createUser(UserDetails user);
	void deleteUser(String username);
	void upgradeUser(String email);
	void updateAccountStatus(User user, AccountStatus accountStatus);

}