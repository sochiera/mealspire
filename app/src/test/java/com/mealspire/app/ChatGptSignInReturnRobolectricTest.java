package com.mealspire.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.widget.Button;

import androidx.test.core.app.ApplicationProvider;

import com.mealspire.app.domain.ChatGptOAuth;
import com.mealspire.app.domain.HttpTransport;
import com.mealspire.app.domain.ResponsesApi;
import com.mealspire.app.domain.SignInTestTokens;
import com.mealspire.app.storage.SharedPreferencesAppSettings;
import com.mealspire.app.storage.SharedPreferencesChatGptSessionStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowAlertDialog;
import org.robolectric.shadows.ShadowDialog;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Coming back from the browser sign-in: a late but valid redirect (app frozen
 * in the background, slow token endpoint, browser already showing its own
 * timeout) still signs the user in without an error; a real failure ends the
 * wait and offers a retry. Real loopback socket, OpenAI endpoints mocked.
 */
@RunWith(RobolectricTestRunner.class)
public class ChatGptSignInReturnRobolectricTest {

    private static final String CLIENT_ID = "oaiapp_new";
    private static final String MODELS = "{\"models\":["
            + "{\"slug\":\"gpt-6-luna\",\"visibility\":\"list\"},"
            + "{\"slug\":\"gpt-6.1-sol\",\"visibility\":\"list\"}]}";

    private final FakeOpenAi openAi = new FakeOpenAi();
    private MainActivity activity;

    @Before
    public void setUp() {
        new SharedPreferencesAppSettings(ApplicationProvider.getApplicationContext())
                .markOnboardingDone();
        MainActivity.backendTransportOverride = openAi;
        MainActivity.readyPoolExecutorOverride = command -> { };
        activity = Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    @After
    public void tearDown() {
        MainActivity.backendTransportOverride = null;
        MainActivity.readyPoolExecutorOverride = null;
    }

    @Test
    public void lateValidRedirectSignsInWithoutFalseError() throws Exception {
        openAi.tokenDelayMs = 1500; // slow token endpoint
        Uri authorize = acceptSignInOffer();
        assertEquals("the wait is visible", MainActivity.SIGN_IN_CHIP_LABEL, chipText());

        Thread.sleep(800); // the user is still in the browser; nothing comes back yet
        idle();
        assertFalse(failureShown());

        // The browser gave up waiting (the app was frozen) and closed the connection,
        // but the redirect itself reached the loopback socket.
        openAi.nonce = authorize.getQueryParameter("nonce");
        redirectAndHangUp(authorize, "code=abc&state=" + authorize.getQueryParameter("state")
                + "&client_id=" + CLIENT_ID);

        waitUntil(() -> chipText().startsWith("AI"));
        assertTrue(signedIn());
        assertFalse("no error over a working account", failureShown());
    }

    @Test
    public void realFailureEndsTheWaitAndOffersRetry() throws Exception {
        Uri authorize = acceptSignInOffer();

        redirectAndHangUp(authorize, "error=access_denied&state="
                + authorize.getQueryParameter("state"));

        waitUntil(this::failureShown);
        assertFalse(signedIn());
        assertEquals("the wait ended", "Tryb offline", chipText());

        AlertDialog failure = ShadowAlertDialog.getLatestAlertDialog();
        Button retry = failure.getButton(AlertDialog.BUTTON_POSITIVE);
        assertEquals(MainActivity.SIGN_IN_RETRY_LABEL, retry.getText().toString());
        retry.performClick();
        Uri second = awaitBrowser();
        assertNotEquals(authorize.getQueryParameter("state"), second.getQueryParameter("state"));
        assertEquals(MainActivity.SIGN_IN_CHIP_LABEL, chipText());
    }

    @Test
    public void tokenExchangeFailureEndsTheWaitAndOffersRetry() throws Exception {
        openAi.tokenStatus = 500;
        Uri authorize = acceptSignInOffer();

        redirectAndHangUp(authorize, "code=abc&state=" + authorize.getQueryParameter("state")
                + "&client_id=" + CLIENT_ID);

        waitUntil(this::failureShown);
        assertFalse(signedIn());
        assertEquals("Tryb offline", chipText());
    }

    @Test
    public void startingOverWhileWaitingSilencesTheAbandonedAttempt() throws Exception {
        Uri first = acceptSignInOffer();
        ShadowDialog.reset();

        activity.findViewById(R.id.ai_chip).performClick();
        AlertDialog pending = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(MainActivity.SIGN_IN_PENDING_TITLE,
                Shadows.shadowOf(pending).getTitle().toString());
        pending.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Uri second = awaitBrowser();

        // The abandoned attempt's listener is gone and its end is not reported as an error.
        try {
            redirectAndHangUp(first, "code=old&state=" + first.getQueryParameter("state"));
        } catch (ConnectException expected) {
            // closed listener
        }
        Thread.sleep(300);
        idle();
        assertFalse(failureShown());
        assertEquals(MainActivity.SIGN_IN_CHIP_LABEL, chipText());

        openAi.nonce = second.getQueryParameter("nonce");
        redirectAndHangUp(second, "code=abc&state=" + second.getQueryParameter("state")
                + "&client_id=" + CLIENT_ID);
        waitUntil(() -> chipText().startsWith("AI"));
        assertFalse(failureShown());
        assertEquals("one token exchange: the new attempt only", 1, openAi.tokenCalls.get());
    }

    // ----- helpers ---------------------------------------------------------

    private Uri acceptSignInOffer() throws Exception {
        AlertDialog offer = ShadowAlertDialog.getLatestAlertDialog();
        assertEquals(MainActivity.SIGN_IN_DIALOG_TITLE, Shadows.shadowOf(offer).getTitle().toString());
        offer.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        return awaitBrowser();
    }

    private Uri awaitBrowser() throws Exception {
        Intent[] started = new Intent[1];
        waitUntil(() -> {
            Intent next = Shadows.shadowOf(activity).getNextStartedActivity();
            if (next != null && Intent.ACTION_VIEW.equals(next.getAction())) {
                started[0] = next;
            }
            return started[0] != null;
        });
        Uri uri = started[0].getData();
        assertNotNull(uri);
        assertTrue(uri.toString().startsWith(ChatGptOAuth.AUTHORIZE_URL));
        return uri;
    }

    /** What a browser does on the redirect, except it stops waiting for the page. */
    private static void redirectAndHangUp(Uri authorize, String query) throws IOException {
        Uri redirect = Uri.parse(authorize.getQueryParameter("redirect_uri"));
        try (Socket browser = new Socket(redirect.getHost(), redirect.getPort())) {
            OutputStream out = browser.getOutputStream();
            out.write(("GET " + redirect.getPath() + "?" + query + " HTTP/1.1\r\n"
                    + "Host: 127.0.0.1\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }
    }

    private interface Condition {
        boolean holds() throws Exception;
    }

    private static void waitUntil(Condition condition) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            idle();
            if (condition.holds()) {
                return;
            }
            Thread.sleep(20);
        }
        fail("condition not reached in time");
    }

    private static void idle() {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private String chipText() {
        return activity.<Button>findViewById(R.id.ai_chip).getText().toString();
    }

    private static boolean signedIn() {
        return new SharedPreferencesChatGptSessionStore(
                ApplicationProvider.getApplicationContext()).load() != null;
    }

    private boolean failureShown() {
        for (Dialog dialog : ShadowDialog.getShownDialogs()) {
            if (dialog instanceof AlertDialog && dialog.isShowing()) {
                CharSequence title = Shadows.shadowOf((AlertDialog) dialog).getTitle();
                if (title != null && MainActivity.SIGN_IN_FAILED_TITLE.contentEquals(title)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** auth.openai.com + api.openai.com stand-in; anything else (backend) is unavailable. */
    private static final class FakeOpenAi implements HttpTransport {
        volatile long tokenDelayMs;
        volatile int tokenStatus = 200;
        volatile String nonce;
        final AtomicInteger tokenCalls = new AtomicInteger();

        @Override
        public Response postForm(String url, Map<String, String> form) throws IOException {
            if (!ChatGptOAuth.TOKEN_URL.equals(url)) {
                return new Response(200, "{}"); // revoke
            }
            tokenCalls.incrementAndGet();
            try {
                Thread.sleep(tokenDelayMs);
                if (tokenStatus != 200) {
                    return new Response(tokenStatus, "{\"error\":\"server_error\"}");
                }
                String idToken = SignInTestTokens.idToken(CLIENT_ID, nonce,
                        System.currentTimeMillis() / 1000 + 600);
                return new Response(200, "{\"access_token\":\"at\",\"refresh_token\":\"rt\","
                        + "\"id_token\":\"" + idToken + "\",\"token_type\":\"Bearer\","
                        + "\"expires_in\":3600,\"scope\":\"openid email offline_access "
                        + ChatGptOAuth.PLAN_USAGE_SCOPE + "\"}");
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        @Override
        public Response get(String url, String bearerToken) throws IOException {
            try {
                if (ChatGptOAuth.JWKS_URL.equals(url)) {
                    return new Response(200, SignInTestTokens.jwks());
                }
            } catch (Exception e) {
                throw new IOException(e);
            }
            if (ResponsesApi.MODELS_URL.equals(url)) {
                return new Response(200, MODELS);
            }
            return new Response(503, "");
        }

        @Override
        public Response postJson(String url, String bearerToken, String json) {
            return new Response(503, "");
        }
    }
}
