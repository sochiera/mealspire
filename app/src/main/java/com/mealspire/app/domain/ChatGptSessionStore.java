package com.mealspire.app.domain;

/**
 * Persists the signed-in ChatGPT session in app-private storage, plus the
 * per-install agent host id that Sign in with ChatGPT binds the client to.
 */
public interface ChatGptSessionStore {

    /** The saved session, or null when signed out. */
    ChatGptSession load();

    void save(ChatGptSession session);

    /** Signs out: forgets the session (the host id stays). */
    void clear();

    /** The user's model pick; {@link GptModel#DEFAULT} until changed. Survives sign-out. */
    GptModel modelChoice();

    void saveModelChoice(GptModel model);

    /** Stable per-install {@code ext_agent_host_id}, created on first use. */
    String hostId();
}
