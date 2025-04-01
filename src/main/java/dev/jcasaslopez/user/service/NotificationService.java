package dev.jcasaslopez.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import dev.jcasaslopez.user.event.ChangePasswordEvent;
import dev.jcasaslopez.user.event.CreateAccountEvent;
import dev.jcasaslopez.user.event.ForgotPasswordEvent;
import dev.jcasaslopez.user.event.ResetPasswordEvent;
import dev.jcasaslopez.user.event.UpdateAccountStatusEvent;
import dev.jcasaslopez.user.event.VerifyEmailEvent;

public interface NotificationService {

	void handleVerifyEmail(VerifyEmailEvent event) throws JsonProcessingException;
	void handleCreateAccount(CreateAccountEvent event);
	void handleForgotPassword(ForgotPasswordEvent event) throws JsonProcessingException;
	void handleResetPassword(ResetPasswordEvent event);
	void handleChangePassword(ChangePasswordEvent event);
	void handleUpdateAccountStatus(UpdateAccountStatusEvent event);

}