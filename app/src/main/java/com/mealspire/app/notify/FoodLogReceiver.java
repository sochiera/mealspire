package com.mealspire.app.notify;

import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.mealspire.app.storage.MealspireDataReset;
import com.mealspire.app.storage.SharedPreferencesFoodLogStore;

/**
 * Evening "Co dzisiaj jedliście?": {@link #ACTION_ASK} comes from the daily alarm
 * and posts the question; {@link #ACTION_REPLY} carries the text typed straight
 * into the notification and appends it to the food log. Offline and cheap.
 */
public final class FoodLogReceiver extends BroadcastReceiver {

    static final String EXTRA_DATA_GENERATION = "data_generation";
    public static final String ACTION_ASK = "com.mealspire.app.action.FOOD_LOG_ASK";
    public static final String ACTION_REPLY = "com.mealspire.app.action.FOOD_LOG_REPLY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MealspireDataReset.isPending(context)) return;
        String action = intent == null ? null : intent.getAction();
        if (ACTION_ASK.equals(action)) {
            FoodLogNotifications.showQuestion(context);
        } else if (ACTION_REPLY.equals(action)) {
            if (intent.getLongExtra(EXTRA_DATA_GENERATION, 0)
                    != MealspireDataReset.generation(context)) return;
            Bundle results = RemoteInput.getResultsFromIntent(intent);
            CharSequence text = results == null
                    ? null : results.getCharSequence(FoodLogNotifications.KEY_REPLY_TEXT);
            if (text == null || text.toString().trim().isEmpty()) {
                FoodLogNotifications.showQuestion(context); // nothing typed — ask again
                return;
            }
            SharedPreferencesFoodLogStore store = new SharedPreferencesFoodLogStore(context);
            store.save(store.load().append(text.toString(), System.currentTimeMillis()));
            FoodLogNotifications.showSaved(context);
        }
    }
}
