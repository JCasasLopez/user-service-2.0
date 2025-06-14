package dev.jcasaslopez.user.config;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import dev.jcasaslopez.user.repository.UserRepository;
import dev.jcasaslopez.user.security.filter.AuthenticationFilter;
import dev.jcasaslopez.user.security.filter.CustomUsernamePasswordAuthenticationFilter;
import dev.jcasaslopez.user.service.UserAccountService;
import dev.jcasaslopez.user.utilities.Constants;

@Configuration
@EnableWebSecurity(debug=true)
@EnableMethodSecurity
public class SecurityConfig {
	
	private UserDetailsService userDetailsService;
	private PasswordEncoder passwordEncoder;
	private AuthenticationFilter authenticationFilter;
	private AuthenticationEntryPoint authenticationEntryPoint;
	private AccessDeniedHandler accessDeniedHandler;
	
	public SecurityConfig(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder,
			AuthenticationFilter authenticationFilter, AuthenticationEntryPoint authenticationEntryPoint,
			AccessDeniedHandler accessDeniedHandler) {
		this.userDetailsService = userDetailsService;
		this.passwordEncoder = passwordEncoder;
		this.authenticationFilter = authenticationFilter;
		this.authenticationEntryPoint = authenticationEntryPoint;
		this.accessDeniedHandler = accessDeniedHandler;
	}

	@Bean
    DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, 
    																	PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
     
        // Prevents UsernameNotFoundException from being wrapped as InternalAuthenticationServiceException,
        // so it can be handled directly in the AuthenticationFailureHandler and the 
        // actual failure reason (user not found) can be logged.
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }
	
	@Bean
	AuthenticationManager authenticationManager() {
	    return new ProviderManager(daoAuthenticationProvider(userDetailsService, passwordEncoder));
	}
	
	@Bean
	CustomUsernamePasswordAuthenticationFilter customUsernamePasswordAuthenticationFilter(
	        StringRedisTemplate redisTemplate,
	        UserAccountService userAccountService,
	        UserRepository userRepository,
	        ApplicationEventPublisher eventPublisher,
	        AuthenticationManager authenticationManager,
	        AuthenticationSuccessHandler authenticationSuccessHandler,
	        AuthenticationFailureHandler authenticationFailureHandler) {

	    CustomUsernamePasswordAuthenticationFilter filter = new CustomUsernamePasswordAuthenticationFilter(
	        redisTemplate, userAccountService, userRepository, eventPublisher
	    );

	    filter.setAuthenticationManager(authenticationManager);
	    filter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
	    filter.setAuthenticationFailureHandler(authenticationFailureHandler);
	    filter.setFilterProcessesUrl(Constants.LOGIN_PATH);

	    return filter;
	}
	
	@Bean 
	CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration config = new CorsConfiguration();
	    config.setAllowedOrigins(List.of("http://localhost:4200")); 
	    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); 
	    config.setAllowedHeaders(List.of("*")); 
	    config.setAllowCredentials(true); 

	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", config); 
	    return source;
	}

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, 
    		CustomUsernamePasswordAuthenticationFilter loginFilter) throws Exception {
       
        http
        	.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sessMang -> sessMang.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // We disable LogoutFilter because we are using custom implementation.
            .logout(logout -> logout.disable()) 
            .authorizeHttpRequests(authorize -> authorize
				            			.requestMatchers(
				            		        "/swagger-ui.html",
				            		        "/swagger-ui/index.html",
				            		        "/swagger-ui/**",
				            		        "/v3/api-docs/**"
				            		    ).permitAll()
				            			
            							.requestMatchers(
            									Constants.INITIATE_REGISTRATION_PATH,
            									Constants.REGISTRATION_PATH,
            									Constants.FORGOT_PASSWORD_PATH,
            									Constants.RESET_PASSWORD_PATH,
            									Constants.LOGOUT_PATH,
            									Constants.LOGIN_PATH
            													).permitAll() 
            							
            							.requestMatchers(
            									Constants.REFRESH_TOKEN_PATH,
            									"/deleteAccount",
            									"/upgradeUser",
            									"/changePassword",
            									"/updateAccountStatus",
            									"/sendNotification"
            													).authenticated()
            							.anyRequest().authenticated()
            );
            return http.build();
    }
}