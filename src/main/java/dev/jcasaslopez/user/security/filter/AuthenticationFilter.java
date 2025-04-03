package dev.jcasaslopez.user.security.filter;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.jcasaslopez.user.handler.StandardResponseHandler;
import dev.jcasaslopez.user.service.TokenService;
import dev.jcasaslopez.user.token.TokenValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthenticationFilter extends OncePerRequestFilter {
	
	private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
	
	private TokenService tokenService;
    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;
    private StandardResponseHandler standardResponseHandler;
    private TokenValidator tokenValidator;
	
	public AuthenticationFilter(TokenService tokenService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper,
			StandardResponseHandler standardResponseHandler, TokenValidator tokenValidator) {
		this.tokenService = tokenService;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.standardResponseHandler = standardResponseHandler;
		this.tokenValidator = tokenValidator;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");
		String urlRequested = request.getRequestURL().toString();
		
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);

			// Tanto en el caso de la verificación del email como del reseteo de contraseña, la lógica es idéntica.
			// 
			// The business logic for both the email verification and password reset cases is the same.
			if (urlRequested.endsWith("userRegistration") || urlRequested.endsWith("resetPassword")) {
				try {
					if (tokenValidator.isTokenFullyValid(token).isPresent()) {
						logger.info("Token valid for user: {}", tokenService.parseClaims(token).getSubject());
						request.setAttribute("verificationToken", token); 
						filterChain.doFilter(request, response);
						return;
					} else {
						logger.warn("Token verification failed: invalid token or username mismatch.");
					}
				} catch (Exception ex) {
					logger.error("Error verifying token: {}", ex.getMessage());
				}
				standardResponseHandler.handleResponse(response, 401, "Invalid or expired token", null);
				return;
				
			} else {
				// Para el resto de endpoints protegidos (distintos de verificación de email o reseteo de contraseña), 
				// se espera un token válido que será procesado según la lógica de autenticación estándar.
				//
				// For all other protected endpoints (other than email verification or password reset), 
				// a valid token is expected and will be handled by the standard authentication logic.
				
				// *********************************************************************************
				// ***************** Meter aquí el contador de intentos fallidos *******************+
				// Ya que ni para el log out ni para las verificaciones de email o reset password
				// influyen los intentos fallidos.
				
				// HABRÁ QUE CONSULTAR PRIMERO CON REDIS CUÁNTOS FALLOS HAY ACUMULADOS, SI ES QUE HAY ALGUNO.
				// SI SE SOBREPASA EL MÁXIMO, QUE ESTÁ EN APPLICATION.PROPERTIES, SE BLOQUE LA CUENTA.
				
			}
		}
		// No se proporciona el encabezado "Bearer", por lo que esta petición no requiere autenticación 
		// bajo este filtro y se permite continuar normalmente.
		//
		// No "Bearer" header is provided, so this request is considered public by this filter 
		// and is allowed to proceed.
		filterChain.doFilter(request, response);
	}
		
}
