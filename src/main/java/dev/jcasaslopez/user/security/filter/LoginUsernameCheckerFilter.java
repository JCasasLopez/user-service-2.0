package dev.jcasaslopez.user.security.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import dev.jcasaslopez.user.enums.LoginFailureReason;
import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.service.LoginAttemptService;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.utilities.Constants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filtro previo al proceso de autenticación que verifica si el nombre de usuario enviado en una 
// solicitud de login existe en la base de datos.
// Su objetivo es evitar que intentos de autenticación con usernames inexistentes lancen una excepción
// dentro de CustomUsernamePasswordAuthenticationFilter, lo que rompería el flujo de autenticación 
// normal y desviaría la gestión del error al AuthenticationEntryPoint. Este comportamiento, aunque 
// forma parte del diseño de Spring Security, impide registrar correctamente el intento de login con 
// la causa real del fallo.
// Al validar la existencia del usuario antes del filtro de autenticación, este filtro permite 
// devolver una respuesta coherente al cliente y persistir adecuadamente el motivo real del fallo 
// (usuario no encontrado).
// Este filtro se ejecuta únicamente sobre la ruta de login y se sitúa antes de 
// CustomUsernamePasswordAuthenticationFilter.
//
// Pre-authentication filter that checks whether the username provided in a login request exists 
// in the database.
// Its purpose is to prevent authentication attempts with non-existent usernames from throwing an 
// exception inside CustomUsernamePasswordAuthenticationFilter, which would break the normal 
// authentication flow and delegate error handling to the AuthenticationEntryPoint. While this behavior 
// is part of Spring Security design, it prevents the application
// from properly recording the login attempt with the actual cause of the failure.
// By validating the user's existence before the authentication filter, this filter allows the 
// application to return a coherent response to the client and persist the real reason for the failure 
// (user not found).
// This filter is executed exclusively on the login route and runs before CustomUsernamePasswordAuthenticationFilter.
@Component
public class LoginUsernameCheckerFilter extends OncePerRequestFilter {
	
    private static final Logger logger = LoggerFactory.getLogger(LoginUsernameCheckerFilter.class);
	
	private final UserAccountService userAccountService;
    private final StandardResponseHandler standardResponseHandler;
    private final LoginAttemptService loginAttemptService;

	public LoginUsernameCheckerFilter(UserAccountService userAccountService,
			StandardResponseHandler standardResponseHandler, LoginAttemptService loginAttemptService) {
		this.userAccountService = userAccountService;
		this.standardResponseHandler = standardResponseHandler;
		this.loginAttemptService = loginAttemptService;
	}

	@Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
		
		logger.info("Entering LoginUsernameCheckerFilter...");
		String requestPath = request.getServletPath();
        String requestMethod = request.getMethod();

        if (Constants.LOGIN_PATH.equals(requestPath) && "POST".equalsIgnoreCase(requestMethod)) {
            String username = request.getParameter("username");
            logger.debug("Intercepted login attempt");
            
            if (username == null || username.trim().isEmpty()) {
            	loginAttemptService.recordAttempt(false, request.getRemoteAddr(),
                        LoginFailureReason.MISSING_FIELD, null);
                logger.warn("Login attempt with missing username");
                standardResponseHandler.handleResponse(response, 400, "Username required", null);
                return;
            }

            try {
                userAccountService.findUser(username);
                logger.debug("Username '{}' found, proceeding with authentication", username);
                
            } catch (UsernameNotFoundException ex) {
                loginAttemptService.recordAttempt(false, request.getRemoteAddr(),
                        LoginFailureReason.USER_NOT_FOUND, null);
                logger.info("Failed login attempt - User not found: '{}'", username);
                standardResponseHandler.handleResponse(response, 400, "Wrong username", null);
                return;
            }
        }
        
        logger.trace("Continuing filter chain for: {} {}", requestMethod, requestPath);
        filterChain.doFilter(request, response);
    }
}