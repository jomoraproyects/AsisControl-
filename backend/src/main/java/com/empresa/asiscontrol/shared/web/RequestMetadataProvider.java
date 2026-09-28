package com.empresa.asiscontrol.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestMetadataProvider {

    public RequestMetadata from(HttpServletRequest request) {
        Object attribute = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        String correlationId = attribute == null ? "unknown" : attribute.toString();
        return new RequestMetadata(
                truncate(request.getRemoteAddr(), 45),
                truncate(request.getHeader("User-Agent"), 500),
                truncate(correlationId, 64));
    }

    private String truncate(String value, int maximum) {
        if (value == null || value.length() <= maximum) {
            return value;
        }
        return value.substring(0, maximum);
    }
}

