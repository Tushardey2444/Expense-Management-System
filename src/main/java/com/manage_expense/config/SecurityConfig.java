package com.manage_expense.config;

import com.manage_expense.redis.GlobalRateLimitFilter;
import com.manage_expense.redis.UserRateLimitFilter;
import com.manage_expense.security.JwtAuthenticationEntryPoint;
import com.manage_expense.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;


@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private GlobalRateLimitFilter globalRateLimitFilter;

    @Autowired
    private UserRateLimitFilter userRateLimitFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) {
        httpSecurity.csrf(AbstractHttpConfigurer::disable);
        httpSecurity.cors(AbstractHttpConfigurer::disable);

        httpSecurity.cors(httpSecurityCorsConfigurer ->
                httpSecurityCorsConfigurer.configurationSource(request -> {
                    CorsConfiguration corsConfiguration = new CorsConfiguration();
                    // for development, we can allow specific origins like localhost:3000, but for production, we should restrict it to our frontend domain
                    corsConfiguration.setAllowedOrigins(List.of("http://localhost:3000"));
                    // for production, we can use setAllowedOriginPatterns to allow subdomains or specific patterns of origins
                    // corsConfiguration.setAllowedOriginPatterns(List.of("http://localhost:3000"));
                    corsConfiguration.setAllowedMethods(List.of("GET","POST","PUT","DELETE"));
                    // Allow cookies and credentials to be sent in CORS requests
                    corsConfiguration.setAllowCredentials(true);
                    corsConfiguration.setAllowedHeaders(List.of("*"));
                    corsConfiguration.setMaxAge(3600L);
                    return corsConfiguration;
                })
        );

        httpSecurity.authorizeHttpRequests(auth -> auth
                  .requestMatchers("/api/auth/**").permitAll()
                  .requestMatchers("/api/user-profile/**").hasAnyRole(AppConstants.ROLE_ADMIN,AppConstants.ROLE_USER)
                  .requestMatchers("/api/budget/**").hasAnyRole(AppConstants.ROLE_ADMIN,AppConstants.ROLE_USER)
                  .requestMatchers("/api/item/**").hasAnyRole(AppConstants.ROLE_ADMIN, AppConstants.ROLE_USER)
                  .requestMatchers("/test/api/sms/**").hasRole(AppConstants.ROLE_ADMIN)
                  .requestMatchers("/test/api/media/**").hasRole(AppConstants.ROLE_ADMIN)
                  .anyRequest().authenticated()
        );
        httpSecurity.exceptionHandling(
                exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint)
        );
        httpSecurity.sessionManagement(
                session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        );
        httpSecurity.addFilterBefore(globalRateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterAfter(userRateLimitFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration builder){
        return builder.getAuthenticationManager();
    }

    // Another way to configure CORS globally using a CorsConfigurationSource bean
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//
//        CorsConfiguration configuration = new CorsConfiguration();
//        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
//        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
//        configuration.setAllowedHeaders(List.of("*"));
//        configuration.setAllowCredentials(true);
//
//        UrlBasedCorsConfigurationSource source =
//                new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//
//        return source;
//    }
}
