package com.mealspire.app.domain;

import java.io.IOException;
import java.util.Map;

/**
 * The few HTTP shapes the ChatGPT sign-in and inference need. Kept as an
 * interface so the OAuth and Responses logic is unit-testable without network.
 */
public interface HttpTransport {

    /** POSTs {@code form} as application/x-www-form-urlencoded. */
    Response postForm(String url, Map<String, String> form) throws IOException;

    /** GETs {@code url}, with a Bearer token when {@code bearerToken} is non-null. */
    Response get(String url, String bearerToken) throws IOException;

    /** POSTs a JSON body with a Bearer token and returns the whole response body. */
    Response postJson(String url, String bearerToken, String json) throws IOException;

    /** Status code plus body (error bodies included). */
    final class Response {
        public final int status;
        public final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body == null ? "" : body;
        }

        public boolean isSuccess() {
            return status >= 200 && status < 300;
        }
    }
}
