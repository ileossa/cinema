package com.cinema.cinema.infrastructure.security;

import com.cinema.cinema.domain.model.Role;
import com.cinema.cinema.domain.model.User;
import com.cinema.cinema.domain.port.out.UserRepositoryOut;
import com.cinema.cinema.domain.service.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepositoryOut userRepositoryOut;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepositoryOut userRepositoryOut) {
        this.jwtTokenProvider = jwtTokenProvider != null ? jwtTokenProvider : new JwtTokenProvider();
        this.userRepositoryOut = userRepositoryOut;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (jwtTokenProvider.validateToken(token)) {
                Long userId = jwtTokenProvider.getUserIdFromToken(token);
                String email = jwtTokenProvider.getEmailFromToken(token);
                Set<String> tokenRoles = jwtTokenProvider.getRolesFromToken(token);

                if (userId != null && email != null) {
                    if (userRepositoryOut != null) {
                        // Double verification: verify user existence and roles against database
                        Optional<User> userOpt = userRepositoryOut.findById(userId);
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            if (user.email().equalsIgnoreCase(email)) {
                                Set<SimpleGrantedAuthority> authorities = new HashSet<>();
                                if (user.roles() != null) {
                                    for (Role role : user.roles()) {
                                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name().toUpperCase()));
                                        authorities.add(new SimpleGrantedAuthority(role.name().toUpperCase()));
                                    }
                                }
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(user, null, authorities);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                            }
                        }
                    } else {
                        // Fallback when repository is not present in context (e.g. unit/slice test)
                        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
                        if (tokenRoles != null) {
                            for (String r : tokenRoles) {
                                authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()));
                                authorities.add(new SimpleGrantedAuthority(r.toUpperCase()));
                            }
                        }
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(email, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
