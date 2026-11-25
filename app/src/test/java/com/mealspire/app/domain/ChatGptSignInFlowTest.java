package com.mealspire.app.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChatGptSignInFlowTest {

    private final ChatGptSignInFlow flow = new ChatGptSignInFlow();

    @Test
    public void idleUntilStarted() {
        assertFalse(flow.isRunning());
    }

    @Test
    public void lateSuccessIsShownAndEndsTheWait() {
        int attempt = flow.start();
        assertTrue(flow.isRunning());

        assertEquals(ChatGptSignInFlow.Outcome.SHOW_SUCCESS, flow.succeeded(attempt));
        assertFalse(flow.isRunning());
    }

    @Test
    public void realFailureEndsTheWaitAndIsShown() {
        int attempt = flow.start();

        assertEquals(ChatGptSignInFlow.Outcome.SHOW_FAILURE, flow.failed(attempt));
        assertFalse("a failure lets the user start again", flow.isRunning());
    }

    @Test
    public void supersededAttemptFailingStaysSilentAndKeepsNewOneRunning() {
        int first = flow.start();
        int second = flow.start();

        assertEquals(ChatGptSignInFlow.Outcome.IGNORE, flow.failed(first));
        assertTrue(flow.isRunning());
        assertEquals(ChatGptSignInFlow.Outcome.SHOW_SUCCESS, flow.succeeded(second));
    }

    @Test
    public void supersededSuccessKeepsTheNewAttemptRunning() {
        int first = flow.start();
        int second = flow.start();

        assertEquals(ChatGptSignInFlow.Outcome.IGNORE, flow.succeeded(first));
        assertTrue(flow.isCurrent(second));
        assertEquals(ChatGptSignInFlow.Outcome.SHOW_SUCCESS, flow.succeeded(second));
    }

    @Test
    public void cancelledAttemptCannotFinish() {
        int attempt = flow.start();
        flow.cancel();

        assertEquals(ChatGptSignInFlow.Outcome.IGNORE, flow.succeeded(attempt));
        assertEquals(ChatGptSignInFlow.Outcome.IGNORE, flow.failed(attempt));
        assertFalse(flow.isRunning());
    }

    @Test
    public void onlyTheNewestRunningAttemptIsCurrent() {
        int first = flow.start();
        assertTrue(flow.isCurrent(first));
        int second = flow.start();
        assertFalse(flow.isCurrent(first));
        assertTrue(flow.isCurrent(second));
        flow.failed(second);
        assertFalse(flow.isCurrent(second));
    }

    @Test
    public void failureAfterASucceededAttemptIsIgnored() {
        int attempt = flow.start();
        flow.succeeded(attempt);

        assertEquals(ChatGptSignInFlow.Outcome.IGNORE, flow.failed(attempt));
    }
}
