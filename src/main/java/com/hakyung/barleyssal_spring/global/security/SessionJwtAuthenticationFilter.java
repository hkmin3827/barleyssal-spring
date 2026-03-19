package com.hakyung.barleyssal_spring.global.security;

import com.hakyung.barleyssal_spring.domain.user.User;
import com.hakyung.barleyssal_spring.domain.user.UserRepository;
import com.hakyung.barleyssal_spring.global.exception.UserNotFoundException;
import com.hakyung.barleyssal_spring.global.jwt.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SessionJwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return uri.equals("/api/v1/auth/login")
                || uri.equals("/api/v1/auth/signup")
                || uri.equals("/api/v1/auth/csrf")
                || uri.startsWith("/api/v1/stats")
                || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs")
                || uri.endsWith(".html")
                || uri.equals("/favicon.ico")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);

        if (session != null && session.getAttribute("ACCESS_TOKEN") != null) {
            String accessToken = (String) session.getAttribute("ACCESS_TOKEN");

            try {
                Long userId = jwtProvider.getId(accessToken);

                User user = userRepository.findById(userId)
                        .orElseThrow(UserNotFoundException::new);

                if (!user.isActive()) {
                    throw new DisabledException("비활성화된 계정입니다.");
                }

                if (user.getDeletedAt() != null) {
                    throw new AccountExpiredException("탈퇴한 계정입니다.");
                }

                UsernamePasswordAuthenticationToken authentication = createAuthentication(user, req);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (ExpiredJwtException e) {
                sendErrorResponse(res, "TOKEN_EXPIRED", "만료된 토큰입니다.");
                return;
            } catch (UsernameNotFoundException | DisabledException | JwtException e) {
                sendErrorResponse(res, "UNAUTHORIZED", e.getMessage());
                return;
            } catch (Exception e) {
                res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private void sendErrorResponse(HttpServletResponse res, String code, String message) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.getWriter().write(String.format("{\"code\": \"%s\", \"message\": \"%s\"}", code, message));
    }

    private UsernamePasswordAuthenticationToken createAuthentication(
            User user,
            HttpServletRequest request
    ) {
        CustomUserDetails userDetails =
                new CustomUserDetails(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        return authentication;
    }
}
