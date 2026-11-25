package com.mealspire.app.domain;

/** Tracks which sign-in attempt may still finish or report an error. */
public final class ChatGptSignInFlow {

    public enum Outcome { SHOW_SUCCESS, SHOW_FAILURE, IGNORE }

    private int latest;
    private boolean running;

    /** Starts a new attempt, superseding any earlier one; returns its id. */
    public synchronized int start() {
        latest++;
        running = true;
        return latest;
    }

    /** Whether the user is waiting for a sign-in to come back. */
    public synchronized boolean isRunning() {
        return running;
    }

    /** Whether {@code attempt} is the newest one and the user still waits for it. */
    public synchronized boolean isCurrent(int attempt) {
        return running && attempt == latest;
    }

    public synchronized void cancel() {
        running = false;
    }

    /** Attempt {@code attempt} saved a session. */
    public synchronized Outcome succeeded(int attempt) {
        if (!isCurrent(attempt)) {
            return Outcome.IGNORE;
        }
        running = false;
        return Outcome.SHOW_SUCCESS;
    }

    /** Attempt {@code attempt} ended without a session (error, timeout, cancelled). */
    public synchronized Outcome failed(int attempt) {
        if (!isCurrent(attempt)) {
            return Outcome.IGNORE;
        }
        running = false;
        return Outcome.SHOW_FAILURE;
    }
}
