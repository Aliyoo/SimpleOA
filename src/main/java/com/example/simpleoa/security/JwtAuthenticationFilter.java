package com.example.simpleoa.security;

import com.example.simpleoa.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Slf4j()
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserService userService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 记录当前请求信息，方便调试
            String url = request.getRequestURI();
            String method = request.getMethod();
            log.info("Processing request: {} {}", method, url);
            
            String jwt = getJwtFromRequest(request);
            
            if (jwt != null) {
                if (jwtTokenProvider.validateToken(jwt)) {
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);
                    log.info("Token valid for user: {}", username);
                    
                    UserDetails userDetails = userService.loadUserByUsername(username);
                    
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    log.info("Authenticated user: {}, authorities: {}", userDetails.getUsername(), 
                                userDetails.getAuthorities());

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("Invalid JWT token in request to {} {}", method, url);
                }
            } else {
                log.info("No JWT token found in request to {} {}", method, url);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context for request: {} {}", 
                        request.getMethod(), request.getRequestURI(), ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        // 首先尝试从 Cookie 中获取 JWT
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    log.debug("JWT token extracted from cookie: {}", token);
                    return token;
                }
            }
        }
        // 如果 Cookie 中没有找到，尝试从 Authorization 头获取（作为备用）
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            String token = bearerToken.substring(7);
            log.debug("JWT token extracted from Authorization header: {}", token);
            return token;
        }
        return null;
    }
}
