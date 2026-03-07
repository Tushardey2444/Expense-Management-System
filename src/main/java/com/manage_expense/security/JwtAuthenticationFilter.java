package com.manage_expense.security;

import com.manage_expense.config.AppConstants;
import com.manage_expense.entities.User;
import com.manage_expense.entities.UserSession;
import com.manage_expense.repository.UserSessionRepository;
import com.manage_expense.services.services_impl.CustomUserDetailService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private JwtHelper jwtHelper;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

//        String reqHeader = request.getHeader("Authorization");
//        if (reqHeader != null && reqHeader.startsWith("Bearer ")) {
//
//        }
//        token = reqHeader.substring(7);

        String path = request.getRequestURI();

        if (AppConstants.PUBLIC_URLS.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = null;
        String token = JwtCookieUtil.extractToken(request, AppConstants.ACCESS_TOKEN);

        if (token != null) {
            try {
                email = jwtHelper.getEmailFromToken(token);
            } catch (ExpiredJwtException e) {
                log.warn("JWT expired: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            } catch (JwtException | IllegalArgumentException e) {
                // JwtException covers MalformedJwtException and others
                log.warn("Invalid JWT: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }


        if (email != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            try {
                UserDetails userDetails =
                        customUserDetailService.loadUserByUsername(email);

                User user = (User) userDetails;

                // Enforce account status
                if (!userDetails.isEnabled()) {
                    log.warn("Inactive/deleted user tried access: {}", email);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                // Token version validation
                Integer tokenVersionFromJwt =
                        jwtHelper.getTokenVersionFromToken(token);

                Integer tokenVersionFromDb =
                        user.getTokenVersion();

                if (!Objects.equals(tokenVersionFromJwt, tokenVersionFromDb)) {
                    log.warn("Token version mismatch for user {}", email);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                String sessionId = jwtHelper.getSessionFromToken(token);

                UserSession session = sessionRepository.findById(sessionId)
                        .orElseThrow(() -> new RuntimeException("Session not found"));

                // Logout single device
                if (session.isRevoked()) {
                    throw new RuntimeException("Session revoked, please login again !!");
                }

                request.setAttribute("sessionId", sessionId);

                // Set authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (UsernameNotFoundException e) {
                log.warn("User not found: {}", email);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
