package dev.jcasaslopez.user.service;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;

public interface UserAccountService {

	User findUser(String username);
	User findUserByEmail(String email);
	void upgradeUser(String email);
	void updateAccountStatus(User user, AccountStatus accountStatus);

}