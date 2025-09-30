package dev.jcasaslopez.user.testhelper;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.enums.AccountStatus;
import dev.jcasaslopez.user.enums.RoleName;

public class UserTestBuilder {
	
	private String username;
	private String password;
	private String fullName;
	private String email;
	private LocalDate dateOfBirth;
	private Set<RoleName> roleNames;
	private AccountStatus accountStatus;
	
	public UserTestBuilder(String username, String password) {
		this.username = username;
		this.password = password;
		this.fullName = "test-user";
		this.email = fullName + "@test.com";
		this.dateOfBirth = LocalDate.of(1979, 12, 27);
		this.roleNames = buildRolesSet(RoleName.ROLE_USER);
		this.accountStatus = AccountStatus.ACTIVE;
	}
	
	private Set<RoleName> buildRolesSet(RoleName roleName) {
		Set<RoleName> roles = new HashSet<>();
		roles.add(roleName);
		return roles;
	}
	
	public UserTestBuilder withFullName(String fullName) {
        this.fullName = fullName;
        return this;
    }
	
	public UserTestBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
	
	public UserTestBuilder withDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }
	
	public UserTestBuilder withRole(RoleName roleName) {
        this.roleNames.add(roleName);
        return this;
    }
	
	public UserTestBuilder withAccountStatus(AccountStatus status) {
        this.accountStatus = status;
        return this;
    }
	
	public User build() {
		User user = new User(username, password, fullName, email, dateOfBirth);
		
		// Setting the roles will be done in the last step in TestHelper: createUser() or createAndPersistUser().
		user.setAccountStatus(accountStatus);
		return user;
	}

	public Set<RoleName> getRoleNames() {
		return roleNames;
	}

}
