package com.alexgit95.MyTrips.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre qui ajoute "Alt-Svc: clear" à chaque réponse.
 * Cela force les navigateurs qui avaient mis en cache une annonce QUIC/HTTP3
 * (via un ancien header Alt-Svc de Cloudflare) à abandonner toute tentative QUIC.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AltSvcClearFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        response.setHeader("Alt-Svc", "clear");
        filterChain.doFilter(request, response);
    }
}
