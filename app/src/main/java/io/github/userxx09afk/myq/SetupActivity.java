package io.github.userxx09afk.myq;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Receives the Home Assistant settings from adb, e.g.
 *
 *   adb shell am start -n io.github.userxx09afk.myq/.SetupActivity \
 *       --es url http://192.168.1.50:8123 --es token TOKEN --es entity cover.garage_door
 *
 * All three values are required together, so another app can never point the
 * watch at a different server while keeping the existing token.
 */
public class SetupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String url = trim(intent.getStringExtra("url"));
        String token = trim(intent.getStringExtra("token"));
        String entity = trim(intent.getStringExtra("entity"));

        if (url == null || token == null || entity == null || !entity.startsWith("cover.")) {
            Toast.makeText(this, R.string.setup_invalid, Toast.LENGTH_LONG).show();
        } else {
            while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            Config.save(this, url, token, entity);
            Toast.makeText(this, R.string.setup_saved, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private static String trim(String s) {
        if (s == null) return null;
        s = s.trim();
        return s.isEmpty() ? null : s;
    }
}
