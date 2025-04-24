package dev.jcasaslopez.user.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import dev.jcasaslopez.user.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name="roles")
@Entity
public class Role {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private int idRole;
	
	@Column(unique=true)
	@Enumerated(EnumType.STRING)
	private RoleName roleName;
	
	@ManyToMany(mappedBy="roles")
	private Set<User> users = new HashSet<>();

	public Role(RoleName roleName) {
		this.roleName = roleName;
	}

	public Role() {
		super();
	}

	public int getIdRole() {
		return idRole;
	}

	public void setIdRole(int idRole) {
		this.idRole = idRole;
	}

	public RoleName getRoleName() {
		return roleName;
	}

	public void setRoleName(RoleName roleName) {
		this.roleName = roleName;
	}

	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}
	
	@Override
	public boolean equals(Object o) {
	    if (this == o) return true;
	    if (!(o instanceof Role)) return false;
	    Role role = (Role) o;
	    return roleName == role.roleName; 
	}

	@Override
	public int hashCode() {
	    return Objects.hash(roleName);
	}
}