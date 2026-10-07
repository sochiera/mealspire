package com.mealspire.app.domain;

import java.io.IOException;

/**
 * {@link LlmClient} that runs on the signed-in user's ChatGPT plan via the
 * Responses API. Retries once with a refreshed token on HTTP 401.
 */
public final class ChatGptLlmClient implements LlmClient {

    private final ChatGptAccount account;
    private final HttpTransport transport;

    public ChatGptLlmClient(ChatGptAccount account, HttpTransport transport) {
        this.account = account;
        this.transport = transport;
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) throws IOException {
        ChatGptSession session = account.activeSession(false);
        HttpTransport.Response response = send(session, systemPrompt, userPrompt);
        if (response.status == 401) {
            session = account.activeSession(true);
            response = send(session, systemPrompt, userPrompt);
        }
        if (!response.isSuccess()) {
            throw ResponsesApi.parseErrorBody(response.status, response.body);
        }
        return ResponsesApi.parseStream(response.body);
    }

    private HttpTransport.Response send(ChatGptSession session, String systemPrompt,
                                        String userPrompt) throws IOException {
        return transport.postJson(ResponsesApi.RESPONSES_URL, session.accessToken,
                ResponsesApi.buildRequest(session.model, systemPrompt, userPrompt));
    }
}
