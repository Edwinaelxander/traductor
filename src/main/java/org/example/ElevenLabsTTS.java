package main.java.org.example;

import java.io.InputStream;
import javazoom.jl.player.Player;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ElevenLabsTTS {
    private static final String API_URL = "https://api.elevenlabs.io/v1/text-to-speech/5Q0t7uMcjvnagumLfvZi";
    private static final String API_KEY = "sk_f8ff40c633b1fbafd2db8638e909fe4a4a340a9e424957d6";
    private static final OkHttpClient client = new OkHttpClient();

    public ElevenLabsTTS() {
    }

    public static InputStream synthesize(String text) throws Exception {
        MediaType jsonMediaType = MediaType.get("application/json; charset=utf-8");
        String payload = "{\"text\":\"" + text.replace("\"", "\\\"") + "\",\"voice_settings\":{}}";
        Request request = (new Request.Builder()).url("https://api.elevenlabs.io/v1/text-to-speech/5Q0t7uMcjvnagumLfvZi").addHeader("xi-api-key", "sk_f8ff40c633b1fbafd2db8638e909fe4a4a340a9e424957d6").post(RequestBody.create(jsonMediaType, payload)).build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException("Error ElevenLabs TTS: " + response.code() + " " + response.message());
        } else {
            return response.body().byteStream();
        }
    }

}
