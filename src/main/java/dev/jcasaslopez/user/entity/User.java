package dev.jcasaslopez.user.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dev.jcasaslopez.user.enums.AccountStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name="users")
public class User {
	
		@Id
		@GeneratedValue(strategy=GenerationType.IDENTITY)
		private int idUser;
		
		@Column(unique=true)
		private String username;
		
		private String password;
		private String fullName;
		
		@Column(unique=true)
		private String email;
		private LocalDate dateOfBirth;
	
		@ManyToMany(fetch = FetchType.EAGER) 
	    @JoinTable(name="user_roles",
	        joinColumns=@JoinColumn(name="user_id", referencedColumnName="idUser"),
	        inverseJoinColumns=@JoinColumn(name="role_id", referencedColumnName="idRole"))
		private Set<Role> roles = new HashSet<>();
		
		@OneToMany(mappedBy="user")
		private List<LoginAttempt> loginAttempts = new ArrayList<>();
		
		@Enumerated(EnumType.STRING)
		private AccountStatus accountStatus;

		public User(String username, String password, String fullName, String email, LocalDate dateOfBirth) {
			this.username = username;
			this.password = password;
			this.fullName = fullName;
			this.email = email;
			this.dateOfBirth = dateOfBirth;
		}

		public User() {
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

		public Set<Role> getRoles() {
			return roles;
		}

		public void setRoles(Set<Role> roles) {
			this.roles = roles;
		}

		public List<LoginAttempt> getLoginAttempts() {
			return loginAttempts;
		}

		public void setLoginAttempts(List<LoginAttempt> loginAttempts) {
			this.loginAttempts = loginAttempts;
		}

		public AccountStatus getAccountStatus() {
			return accountStatus;
		}

		public void setAccountStatus(AccountStatus accountStatus) {
			this.accountStatus = accountStatus;
		}
		
		@Override
		public boolean equals(Object o) {
		    if (this == o)
		    	return true;
		    if (!(o instanceof User)) 
		    	return false; 
		    User other = (User) o;
		    return Objects.equals(username, other.username);
		}

		@Override
		public int hashCode() {
		    return Objects.hash(username);
		}

}