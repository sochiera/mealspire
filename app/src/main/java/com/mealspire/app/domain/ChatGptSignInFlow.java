package com.mealspire.app.domain;

/**
 * State of the in-app "Sign in with ChatGPT" wait. The browser may come back
 * late (the app was in the background, the token endpoint was slow), the
 * user may give up and start again, and the earlier attempt may still finish.
 * This decides what the screen shows so it always matches the stored session:
 * a success ends the wait and is shown even from a superseded attempt (the
 * session is real), a failure ends the wait and offers a retry only when it
 * belongs to the attempt the user is still waiting for.
 */
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

    /** Attempt {@code attempt} saved a session. */
    public synchronized Outcome succeeded(int attempt) {
        running = false;
        return Outcome.SHOW_SUCCESS;
    }

    /** Attempt {@code attempt} ended without a session (error, timeout, cancelled). */
    public synchronized Outcome failed(int attempt) {
        if (attempt != latest || !running) {
            return Outcome.IGNORE;
        }
        running = false;
        return Outcome.SHOW_FAILURE;
    }
}
