package dev.jcasaslopez.user.service;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import dev.jcasaslopez.user.dto.UserDto;
import dev.jcasaslopez.user.enums.AccountStatus;
import jakarta.servlet.http.HttpServletRequest;

public interface AccountOrchestrationService {

	void initiateRegistration(UserDto user) throws JsonProcessingException;
	void userRegistration(HttpServletRequest request) throws JsonMappingException, JsonProcessingException;
	void deleteAccount();
	void forgotPassword(String email);
	void resetPassword(String newPassword, HttpServletRequest request);
	void changePassword(String newPassword, String oldPassword);
	void upgradeUser(String email);
	void updateAccountStatus(String email, AccountStatus newAccountStatus);
	void sendNotification(Map<String, String> messageAsMap);
	List<String> refreshToken(String username);
	
}