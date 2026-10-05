package io.github.userxx09afk.myq;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One button: shows "Open" when the door is closed and "Close" when it is open.
 * The first tap arms the button and a second tap within a few seconds sends the
 * command, so a bumped wrist can't move the door. While the door is moving the
 * button becomes "Stop".
 */
public class MainActivity extends Activity {

    private static final long POLL_IDLE_MS = 10_000;
    private static final long POLL_MOVING_MS = 2_000;
    private static final long CONFIRM_WINDOW_MS = 3_000;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private HomeAssistant ha;
    private TextView status;
    private Button button;

    private String state;          // last state reported by Home Assistant
    private boolean armed;         // waiting for the confirming second tap
    private boolean busy;          // a request is in flight
    private boolean resumed;

    private final Runnable poll = this::refresh;
    private final Runnable disarm = () -> {
        armed = false;
        render();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.status);
        button = findViewById(R.id.button);
        button.setOnClickListener(v -> onTap());
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        ha = Config.load(this);   // re-read in case setup just ran
        if (ha == null) {
            status.setText(R.string.not_set_up);
            button.setVisibility(Button.GONE);
            return;
        }
        button.setVisibility(Button.VISIBLE);
        refresh();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        armed = false;
        main.removeCallbacks(poll);
        main.removeCallbacks(disarm);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }

    private void onTap() {
        if (busy) return;

        if (isMoving()) {
            send("stop_cover");
            return;
        }
        if (!"open".equals(state) && !"closed".equals(state)) {
            refresh();   // the button reads "Retry"
            return;
        }
        if (!armed) {
            armed = true;
            render();
            main.postDelayed(disarm, CONFIRM_WINDOW_MS);
            return;
        }

        main.removeCallbacks(disarm);
        armed = false;
        send("closed".equals(state) ? "open_cover" : "close_cover");
    }

    private void send(String service) {
        busy = true;
        render();
        io.execute(() -> {
            try {
                ha.callCover(service);
                main.post(() -> {
                    // Show motion immediately; the next poll reports the real state.
                    if ("open_cover".equals(service)) state = "opening";
                    else if ("close_cover".equals(service)) state = "closing";
                    busy = false;
                    render();
                    schedulePoll(1_000);
                });
            } catch (IOException e) {
                main.post(() -> fail(e));
            }
        });
    }

    private void refresh() {
        if (ha == null || busy) return;
        main.removeCallbacks(poll);
        busy = true;
        render();
        io.execute(() -> {
            try {
                String s = ha.getState();
                main.post(() -> {
                    state = s;
                    busy = false;
                    render();
                    schedulePoll(isMoving() ? POLL_MOVING_MS : POLL_IDLE_MS);
                });
            } catch (IOException e) {
                main.post(() -> fail(e));
            }
        });
    }

    private void fail(IOException e) {
        busy = false;
        state = null;
        status.setText(getString(R.string.error, e.getMessage() != null ? e.getMessage() : "No connection"));
        button.setText(R.string.retry);
        button.setEnabled(true);
        tint(R.color.neutral);
    }

    private void schedulePoll(long delayMs) {
        main.removeCallbacks(poll);
        if (resumed) main.postDelayed(poll, delayMs);
    }

    private boolean isMoving() {
        return "opening".equals(state) || "closing".equals(state);
    }

    private void render() {
        button.setEnabled(!busy);
        if (busy && state == null) {
            status.setText(R.string.connecting);
            button.setText("…");
            tint(R.color.neutral);
            return;
        }

        String s = state == null ? "" : state;
        switch (s) {
            case "closed":
                status.setText(R.string.state_closed);
                button.setText(armed ? R.string.confirm : R.string.action_open);
                tint(armed ? R.color.confirm : R.color.open);
                break;
            case "open":
                status.setText(R.string.state_open);
                button.setText(armed ? R.string.confirm : R.string.action_close);
                tint(armed ? R.color.confirm : R.color.close);
                break;
            case "opening":
            case "closing":
                status.setText("opening".equals(s) ? R.string.state_opening : R.string.state_closing);
                button.setText(R.string.action_stop);
                tint(R.color.confirm);
                break;
            default:
                status.setText(getString(R.string.state_other, s));
                button.setText(R.string.retry);
                tint(R.color.neutral);
        }
    }

    private void tint(int colorRes) {
        button.setBackgroundTintList(ColorStateList.valueOf(getColor(colorRes)));
    }
}
