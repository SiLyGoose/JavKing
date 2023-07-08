//package javking.rest.configuration;
//
//import org.jetbrains.annotations.NotNull;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//
//public class CorsInterceptor implements HandlerInterceptor {
//    @Override
//    public void afterCompletion(@NotNull HttpServletRequest request, HttpServletResponse response, @NotNull Object handler, Exception ex) throws Exception {
//        response.setHeader("Vary", "Origin");
//    }
//}
