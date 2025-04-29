package dev.jcasaslopez.user.utilities;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dev.jcasaslopez.user.entity.User;
import dev.jcasaslopez.user.repository.UserRepository;

@Component
public class TestHelper {
	
	@Autowired private UserRepository userRepository;	
	
	public User createUser() {
		// El usuario solo tiene el rol "USER", que se asigna automáticamente.
		//
		// User only has the role "USER", which is assigned automatically.
		User plainUser = new User ("Yorch123", "Jorge22!", "Jorge García", "jorgecasas22@hotmail.com", 
				LocalDate.of(1978, 11, 26));
		userRepository.save(plainUser);
		return plainUser;
	}
}