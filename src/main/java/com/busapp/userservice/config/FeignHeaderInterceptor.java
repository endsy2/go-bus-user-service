package com.busapp.userservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign interceptor that forwards gateway headers (X-User-Id, X-User-Email, etc.)
 * to other microservices when making Feign client calls.
 * 
 * This ensures that user context is preserved across service-to-service calls.
 */
@Component
public class FeignHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            // Forward all gateway headers to the target service
            String userId = request.getHeader("X-User-Id");
            String userEmail = request.getHeader("X-User-Email");
            String userName = request.getHeader("X-User-Name");
            String userRoles = request.getHeader("X-User-Roles");
            String userPermissions = request.getHeader("X-User-Permissions");
            
            if (userId != null) {
                template.header("X-User-Id", userId);
            }
            if (userEmail != null) {
                template.header("X-User-Email", userEmail);
            }
            if (userName != null) {
                template.header("X-User-Name", userName);
            }
            if (userRoles != null) {
                template.header("X-User-Roles", userRoles);
            }
            if (userPermissions != null) {
                template.header("X-User-Permissions", userPermissions);
            }
        }
    }
}
