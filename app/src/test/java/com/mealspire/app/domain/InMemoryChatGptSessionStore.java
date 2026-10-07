package com.mealspire.app.domain;

/** Test double for {@link ChatGptSessionStore}. */
final class InMemoryChatGptSessionStore implements ChatGptSessionStore {

    ChatGptSession session;
    int clears;

    @Override
    public ChatGptSession load() {
        return session;
    }

    @Override
    public void save(ChatGptSession session) {
        this.session = session;
    }

    @Override
    public void clear() {
        session = null;
        clears++;
    }

    GptModel modelChoice = GptModel.DEFAULT;

    @Override
    public GptModel modelChoice() {
        return modelChoice;
    }

    @Override
    public void saveModelChoice(GptModel model) {
        modelChoice = model;
    }

    @Override
    public String hostId() {
        return "urn:uuid:00000000-0000-4000-8000-000000000001";
    }
}
