package dev.jcasaslopez.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

public interface RegistrationService {

	void createAccount(String token) throws JsonMappingException, JsonProcessingException;

}