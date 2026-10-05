package io.github.userxx09afk.myq;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Home Assistant REST client for a single cover entity.
 * https://developers.home-assistant.io/docs/api/rest/
 */
final class HomeAssistant {

    private static final int TIMEOUT_MS = 8000;

    private final String baseUrl;
    private final String token;
    private final String entityId;

    HomeAssistant(String baseUrl, String token, String entityId) {
        this.baseUrl = baseUrl;
        this.token = token;
        this.entityId = entityId;
    }

    /** Returns the cover state: open, closed, opening, closing, unavailable or unknown. */
    String getState() throws IOException {
        String body = request("GET", "/api/states/" + entityId, null);
        try {
            return new JSONObject(body).getString("state");
        } catch (JSONException e) {
            throw new IOException("Unexpected reply from Home Assistant");
        }
    }

    /** Calls cover.open_cover, cover.close_cover or cover.stop_cover. */
    void callCover(String service) throws IOException {
        String payload;
        try {
            payload = new JSONObject().put("entity_id", entityId).toString();
        } catch (JSONException e) {
            throw new IOException(e);
        }
        request("POST", "/api/services/cover/" + service, payload);
    }

    private String request(String method, String path, String payload) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + path).openConnection();
        try {
            conn.setRequestMethod(method);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");

            if (payload != null) {
                conn.setDoOutput(true);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(payload.getBytes(StandardCharsets.UTF_8));
                }
            }

            int code = conn.getResponseCode();
            if (code == 401) throw new IOException("Token rejected");
            if (code == 404) throw new IOException("Entity not found");
            if (code >= 400) throw new IOException("HTTP " + code);

            try (InputStream in = conn.getInputStream()) {
                return readAll(in);
            }
        } finally {
            conn.disconnect();
        }
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toString(StandardCharsets.UTF_8.name());
    }
}
