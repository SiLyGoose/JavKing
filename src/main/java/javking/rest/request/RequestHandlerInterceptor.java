package javking.rest.request;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.Callable;

public class RequestHandlerInterceptor {
    private final FormBody.Builder requestBody;
    private final OkHttpClient client;

    private RequestContext requestContext;
    private Response response;

    public RequestHandlerInterceptor(HttpServletResponse response) {
        requestBody = new FormBody.Builder();
        requestContext = new RequestContext(response);
        client = new OkHttpClient();
    }

    public HttpServletResponse getHttpServletResponse() {
        return requestContext.getResponse();
    }

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public RequestContext newRequestContext() {
        requestContext = new RequestContext(getHttpServletResponse());
        return requestContext;
    }

    public Response getResponse() {
        return response;
    }

    public RequestHandlerInterceptor executeRequest(@Nullable String redirectUrl) throws Exception {
        return executeRequest(redirectUrl, null);
    }

    public RequestHandlerInterceptor executeRequest(@Nullable String redirectUrl, @Nullable Callable task) throws Exception {
        Request request = requestContext.build();

        response = client.newCall(request).execute();

        if (redirectUrl != null) {
            getHttpServletResponse().sendRedirect(redirectUrl);
        }

        if (task != null) {
            task.call();
        }

        return this;
    }
}
