package dev.jcasaslopez.user.dto;

import java.time.LocalDate;
import java.util.Set;

import dev.jcasaslopez.user.enums.AccountStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

public class UserDto {
	
	private int idUser;
	
	@NotBlank(message = "Username field is required")
	private String username;
	
	@NotBlank(message = "Password field is required")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.,:;#_\\-])[A-Za-z\\d@$!%*?&.,:;#_\\-]{8,}$",
		     message = "Password must have at least 8 characters, including one upper case letter"
		     		+ ", one lower case letter, a number and a symbol"
		    )
	private String password;
	
	@NotBlank(message = "Full name field is required")
	private String fullName;
	
	@NotBlank(message = "Email field is required")
	@Email
	private String email;
	
	@NotNull(message = "Date of birth field is required")
	@Past
	private LocalDate dateOfBirth;
	
	@NotNull(message = "Roles field is required")
	private Set<RoleDto> roles;
	
	@NotNull(message = "Account status field is required")
	private AccountStatus accountStatus;
	
	public UserDto(String username, String password, String fullName, String email, LocalDate dateOfBirth, Set<RoleDto> roles,
			AccountStatus accountStatus) {
		this.username = username;
		this.password = password;
		this.fullName = fullName;
		this.email = email;
		this.dateOfBirth = dateOfBirth;
		this.roles = roles;
		this.accountStatus = accountStatus;
	}

	public UserDto() {
		super();
	}

	public int getIdUser() {
		return idUser;
	}

	public void setIdUser(int idUser) {
		this.idUser = idUser;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Set<RoleDto> getRoles() {
		return roles;
	}

	public void setRoles(Set<RoleDto> roles) {
		this.roles = roles;
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

}
