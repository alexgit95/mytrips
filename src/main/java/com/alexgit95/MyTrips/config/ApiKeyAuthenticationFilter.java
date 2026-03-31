package com.alexgit95.MyTrips.config;

import com.alexgit95.MyTrips.service.ApiAccessKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_EXPORT_PATH = "/api/admin/export";
    private static final String API_KEY_QUERY_PARAM = "apiKey";

    private final ApiAccessKeyService apiAccessKeyService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !("GET".equalsIgnoreCase(request.getMethod()) && API_EXPORT_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getParameter(API_KEY_QUERY_PARAM);
        if (!apiAccessKeyService.authenticate(apiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API key invalide ou expiree");
            return;
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                "api-export",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_API_EXPORT"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
