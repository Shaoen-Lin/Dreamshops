package com.Shawn.dream_shops.security.jwt;


import com.Shawn.dream_shops.security.service.ShopUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.Shawn.dream_shops.security.user.ShopUserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private ShopUserDetailsService userDetailsService;

    @Autowired
    private JwtAuthEntryPoint authEntryPoint;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);
            if (StringUtils.hasText(jwt))
            {
                Claims claims = jwtUtils.parseClaims(jwt);
                if (claims.getExpiration() == null || !StringUtils.hasText(claims.getSubject())
                        || !(claims.get("id") instanceof Number id)) {
                    throw new JwtException("The token does not identify an account");
                }
                ShopUserDetails userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
                if (!userDetails.getId().equals(id.longValue())) {
                    throw new JwtException("The token does not belong to the current account");
                }
                Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException | UsernameNotFoundException e) {
            SecurityContextHolder.clearContext();
            authEntryPoint.commence(request, response, null);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request)
    {
        String headerAuth = request.getHeader("Authorization");
        // 從 HTTP 請求中讀取 Authorization 標頭(header)。
        if(StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")){
            return headerAuth.substring(7);
        }
        return null;
    }
}
