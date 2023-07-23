package com.esmile.security.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @Description TODO
 * @Date 2023/7/18
 * @Created by nisran
 */
public class AuthLoggingFilter implements Filter {

    private final Logger LOG = Logger.getLogger(this.getClass().getName());
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            LOG.info("logged successful, username: " + authentication.getName() + ", authorities: " + authentication.getAuthorities());
        }
        chain.doFilter(request, response);
    }
}
