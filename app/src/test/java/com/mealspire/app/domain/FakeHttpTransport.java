package com.mealspire.app.domain;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/** Scripted {@link HttpTransport}: answers queued responses and records calls. */
final class FakeHttpTransport implements HttpTransport {

    static final class Call {
        final String method;
        final String url;
        final String bearer;
        final Map<String, String> form;
        final String json;

        Call(String method, String url, String bearer, Map<String, String> form, String json) {
            this.method = method;
            this.url = url;
            this.bearer = bearer;
            this.form = form;
            this.json = json;
        }
    }

    final List<Call> calls = new ArrayList<>();
    private final Deque<Response> responses = new ArrayDeque<>();

    FakeHttpTransport respond(int status, String body) {
        responses.add(new Response(status, body));
        return this;
    }

    private Response next() throws IOException {
        if (responses.isEmpty()) {
            throw new IOException("no scripted response");
        }
        return responses.poll();
    }

    @Override
    public Response postForm(String url, Map<String, String> form) throws IOException {
        calls.add(new Call("POST", url, null, form, null));
        return next();
    }

    @Override
    public Response get(String url, String bearerToken) throws IOException {
        calls.add(new Call("GET", url, bearerToken, null, null));
        return next();
    }

    @Override
    public Response postJson(String url, String bearerToken, String json) throws IOException {
        calls.add(new Call("POST", url, bearerToken, null, json));
        return next();
    }
}
