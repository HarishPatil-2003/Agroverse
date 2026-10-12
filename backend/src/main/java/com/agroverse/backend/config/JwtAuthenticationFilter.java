package com.agroverse.backend.config;

import com.agroverse.backend.model.User;
import com.agroverse.backend.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserService userService) {
        this.tokenProvider = tokenProvider;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromToken(jwt);
                String role = tokenProvider.getRoleFromToken(jwt);

                if (userId == null) {
                    // Supabase UUID token: resolve or lazily provision persistent farmer profile
                    String sub = tokenProvider.getSubjectFromToken(jwt);
                    String email = tokenProvider.getEmailFromToken(jwt);
                    Map<String, Object> metadata = tokenProvider.getUserMetadata(jwt);

                    if (userService != null && sub != null) {
                        try {
                            User user = userService.resolveOrCreateSupabaseUser(sub, email, metadata);
                            if (user != null) {
                                userId = user.getId();
                                if (user.getRole() != null) {
                                    role = user.getRole();
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Could not resolve/provision farmer profile from Supabase token", e);
                        }
                    }
                }

                if (userId != null) {
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority(role != null ? role : "ROLE_FARMER");
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, Collections.singletonList(authority));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
