package com.alexgit95.MyTrips.config;

import com.alexgit95.MyTrips.service.LoginAttemptService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        boolean lockedByThreshold = loginAttemptService.recordFailure(username);
        boolean lockedBySecurity = exception instanceof LockedException;

        String target = lockedByThreshold || lockedBySecurity ? "/login?locked" : "/login?error";
        response.sendRedirect(request.getContextPath() + target);
    }
}