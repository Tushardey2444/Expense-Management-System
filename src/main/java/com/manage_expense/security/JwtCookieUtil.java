package com.manage_expense.security;


import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;


public class JwtCookieUtil {
    public static String extractToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
/*
request.getCookies() returns an array like:
[
  Cookie("ACCESS_TOKEN", "abc123"),
  Cookie("REFRESH_TOKEN", "xyz789")
]
 */