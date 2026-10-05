package io.github.userxx09afk.myq;

import android.content.Context;
import android.content.SharedPreferences;

/** Home Assistant connection settings, stored privately on the watch. */
final class Config {

    private static final String PREFS = "config";
    private static final String KEY_URL = "url";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_ENTITY = "entity";

    private Config() {}

    /** Returns a client, or null if setup has not been run yet. */
    static HomeAssistant load(Context context) {
        SharedPreferences prefs = prefs(context);
        String url = prefs.getString(KEY_URL, null);
        String token = prefs.getString(KEY_TOKEN, null);
        String entity = prefs.getString(KEY_ENTITY, null);
        if (url == null || token == null || entity == null) return null;
        return new HomeAssistant(url, token, entity);
    }

    static void save(Context context, String url, String token, String entity) {
        prefs(context).edit()
                .putString(KEY_URL, url)
                .putString(KEY_TOKEN, token)
                .putString(KEY_ENTITY, entity)
                .apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
