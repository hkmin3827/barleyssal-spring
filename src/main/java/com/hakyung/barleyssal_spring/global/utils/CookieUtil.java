package com.hakyung.barleyssal_spring.global.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public class CookieUtil {

    private CookieUtil() {
    }

    public static void clearAuthCookies(HttpServletResponse res) {
        ResponseCookie sessionCookie = ResponseCookie.from("SESSION", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(true)
                .build();

        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", "")
                .path("/")
                .maxAge(0)
                .httpOnly(false)
                .secure(true)
                .build();

        res.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());
        res.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
    }
}