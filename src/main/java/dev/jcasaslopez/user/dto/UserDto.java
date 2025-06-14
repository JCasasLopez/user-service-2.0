package dev.jcasaslopez.user.dto;

import java.time.LocalDate;
import java.util.Set;

import dev.jcasaslopez.user.enums.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(
	    name = "UserDto",
	    description = "DTO used to persist a user in the database"
	)
public class UserDto {
	
	@Schema(hidden = true)
	private int idUser;
	
	@Schema(description = "Username between 6 and 20 characters", example = "john_doe")
	@NotBlank(message = "Username field is required")
	@Size(min=6, max=20)
	private String username;
	
	@Schema(description = "Secure password (min 8 chars, upper/lowercase, number and symbol)", 
	        example = "Secure@123")
	@NotBlank(message = "Password field is required")
	@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.,:;#_\\-])[A-Za-z\\d@$!%*?&.,:;#_\\-]{8,}$",
		     message = "Password must have at least 8 characters, including one upper case letter"
		     		+ ", one lower case letter, a number and a symbol"
		    )
	private String password;
	
	@Schema(description = "Full name of the user", example = "John Doe")
	@NotBlank(message = "Full name field is required")
	@Size(max=30)
	private String fullName;
	
	@Schema(description = "User email address", example = "john@example.com")
	@NotBlank(message = "Email field is required")
	@Email
	private String email;
	
	@Schema(description = "Date of birth in ISO format (yyyy-MM-dd)", example = "1990-05-20")
	@NotNull(message = "Date of birth field is required")
	@Past
	private LocalDate dateOfBirth;
	
	@Schema(hidden = true)
	private Set<RoleDto> roles;
	
	@Schema(hidden = true)
	private AccountStatus accountStatus;
	
	// UserDto -> User. Sirve para crear un nuevo User. No tiene idUser puesto que no se ha creado aún.
	//
	// UserDto -> User. To create a new User. No idUser since it has been created yet.
	public UserDto(String username, String password, String fullName, String email, LocalDate dateOfBirth) {
		this.username = username;
		this.password = password;
		this.fullName = fullName;
		this.email = email;
		this.dateOfBirth = dateOfBirth;
	}
	
	// User -> UserDto. Sirve para enviar información sobre User al front-end.
	//
	// User -> UserDto. To send User info to the front-end.
	public UserDto(int idUser, String username, String fullName, String email, 
			LocalDate dateOfBirth, Set<RoleDto> roles, AccountStatus accountStatus) {
		this.idUser = idUser;
		this.username = username;
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
