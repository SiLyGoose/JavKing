package javking.rest.request;

import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletResponse;
import java.net.URL;

public class RequestContext extends Request.Builder {
    private final FormBody.Builder requestBody;
    private final HttpServletResponse response;

    public RequestContext(HttpServletResponse response) {
        requestBody = new FormBody.Builder();
        this.response = response;
    }

    public RequestContext setUrl(String url) {
        return (RequestContext) super.url(url);
    }

    public RequestContext setUrl(URL url) {
        return (RequestContext) super.url(url);
    }

    public RequestContext setUrl(HttpUrl url) {
        return (RequestContext) super.url(url);
    }

    public RequestContext setFormBody(FormBody formBody) {
        return (RequestContext) super.post(formBody);
    }

    @NotNull
    public RequestContext addHeader(@NotNull String name, @NotNull String value) {
        return (RequestContext) super.header(name, value);
    }

    public RequestContext setHeaders(Headers headers) {
        return (RequestContext) super.headers(headers);
    }

    public RequestContext concatRequestBody(String name, String value) {
        requestBody.add(name, value);
        return this;
    }

    public Request.Builder post() {
        return super.post(requestBody.build());
    }

    public Request.Builder put() {
        return super.put(requestBody.build());
    }

    public Request.Builder addRequestBody(RequestBody requestBody) {
        return super.post(requestBody);
    }

    public HttpServletResponse getResponse() {
        return response;
    }
}
