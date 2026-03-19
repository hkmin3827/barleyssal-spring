package com.hakyung.barleyssal_spring.global.ratelimit;

import com.hakyung.barleyssal_spring.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    private static final int DEFAULT_MAX_REQUESTS = 5;
    private static final int DEFAULT_WINDOW_SECONDS = 1;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return true;
        }

        Long userId = userDetails.getId();
        int maxRequests = DEFAULT_MAX_REQUESTS;
        int windowSeconds = DEFAULT_WINDOW_SECONDS;
        String limitScope = "global";

        if (handler instanceof HandlerMethod handlerMethod) {
            RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (rateLimit != null) {
                maxRequests = rateLimit.maxRequests();
                windowSeconds = rateLimit.windowSeconds();
                limitScope = request.getRequestURI();
            }
        }

        if (!rateLimitService.isAllowed(userId, limitScope, maxRequests, windowSeconds)) {
            log.warn("[RateLimit] 유저 ID: {}, Scope: {} - 요청 횟수 초과", userId, limitScope);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            try (PrintWriter writer = response.getWriter()) {
                // 프론트엔드 파싱 오류를 방지하기 위해 유효한 JSON 포맷으로 수정
                writer.write("{\"error\": \"TOO_MANY_REQUESTS\", \"message\": \"요청이 너무 많습니다. 잠시 후 다시 시도해주세요.\"}");
            }
            return false;
        }

        return true;
    }
}