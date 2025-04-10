package dev.jcasaslopez.user.config;

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
	    filter.setFilterProcessesUrl("/user/login");

	    return filter;
	}


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, 
    		CustomUsernamePasswordAuthenticationFilter loginFilter) throws Exception {
       
        http
        	.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sessMang -> sessMang.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(authenticationFilter, CustomUsernamePasswordAuthenticationFilter.class)
            .addFilterAt(loginFilter, CustomUsernamePasswordAuthenticationFilter.class) 
            // Deshabilito LogoutFilter poque voy a usar una implementación personalizada.
            //
            // We disable LogoutFilter because we are using custom implementation.
            .logout(logout -> logout.disable()) 
            .authorizeHttpRequests(authorize -> authorize
            							.requestMatchers(
            									Constants.INITIATE_REGISTRATION_PATH,
            									Constants.REGISTRATION_PATH,
            									Constants.FORGOT_PASSWORD_PATH,
            									Constants.RESET_PASSWORD_PATH,
            									Constants.REFRESH_TOKEN_PATH
            													).permitAll() 
            							.requestMatchers(
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
