package com.mealspire.app.storage;

import android.content.Context;
import android.content.SharedPreferences;

import com.mealspire.app.domain.ChatGptSession;
import com.mealspire.app.domain.ChatGptSessionStore;

import java.util.UUID;

/**
 * {@link ChatGptSessionStore} in app-private {@link SharedPreferences}. The
 * OS sandbox keeps the user's tokens from other apps, and the backup rules
 * (res/xml) exclude this file, so the tokens never leave the device.
 */
public final class SharedPreferencesChatGptSessionStore implements ChatGptSessionStore {

    static final String PREFS_NAME = "mealspire_chatgpt";
    private static final String KEY_SESSION = "session";
    private static final String KEY_HOST_ID = "host_id";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesChatGptSessionStore(Context context) {
        this.sharedPreferences = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public ChatGptSession load() {
        return ChatGptSession.fromJson(sharedPreferences.getString(KEY_SESSION, ""));
    }

    @Override
    public void save(ChatGptSession session) {
        // commit(): a rotated refresh token must not be lost if the process dies.
        sharedPreferences.edit().putString(KEY_SESSION, session.toJson()).commit();
    }

    @Override
    public void clear() {
        sharedPreferences.edit().remove(KEY_SESSION).commit();
    }

    @Override
    public synchronized String hostId() {
        String id = sharedPreferences.getString(KEY_HOST_ID, "");
        if (id.isEmpty()) {
            id = "urn:uuid:" + UUID.randomUUID();
            sharedPreferences.edit().putString(KEY_HOST_ID, id).commit();
        }
        return id;
    }
}
