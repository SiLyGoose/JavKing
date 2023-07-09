package javking.rest.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

public class CorsInterceptor implements HandlerInterceptor {
    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, HttpServletResponse response, @NotNull Object handler, @Nullable Exception ex) throws Exception {
        response.setHeader("Vary", "Origin");
    }
}
