package com.hakyung.barleyssal_spring.global.jwt;

import hakyung.barleyssal_spring.domain.user.constant.Role;
import hakyung.barleyssal_spring.global.security.CustomUserDetails;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SessionJwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtProvider jwtProvider;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();

        return uri.equals("/api/auth/login")
                || uri.equals("/api/auth/signup")
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
        if(session != null && session.getAttribute("ACCESS_TOKEN") != null){
            String accessToken = (String) session.getAttribute("ACCESS_TOKEN");

            try{
                Long userId = jwtProvider.getId(accessToken);
                Role role = jwtProvider.getRole(accessToken);

                UsernamePasswordAuthenticationToken authentication =
                        createAuthentication(userId, role, req);

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (ExpiredJwtException e) {
                sendUnauthorizedResponse(res);
                return;

            } catch (JwtException | IllegalArgumentException e) {
                sendUnauthorizedResponse(res);
                return;
            }
        }
        chain.doFilter(req, res);
    }

    private void sendUnauthorizedResponse(HttpServletResponse res) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);

        res.getWriter().write("""
            {
              "code": "UNAUTHORIZED",
              "message": "유효하지 않거나 만료된 토큰입니다."
            }
            """);
    }

    private UsernamePasswordAuthenticationToken createAuthentication(
            Long userId,
            Role role,
            HttpServletRequest request
    ) {
        CustomUserDetails userDetails =
                new CustomUserDetails(userId, role);

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
