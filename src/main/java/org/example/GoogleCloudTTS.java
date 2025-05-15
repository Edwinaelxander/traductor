package main.java.org.example;

import com.google.cloud.texttospeech.v1.*;
import com.google.protobuf.ByteString;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class GoogleCloudTTS {

    public static InputStream synthesize(String text) throws Exception {
        try (TextToSpeechClient client = TextToSpeechClient.create()) {
            SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();

            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                    .setLanguageCode("es-ES")
                    .setSsmlGender(SsmlVoiceGender.FEMALE)
                    .build();

            AudioConfig audioConfig = AudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.MP3)  // mp3 para ser compatible con ElevenLabs y player
                    .build();

            SynthesizeSpeechResponse response = client.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContents = response.getAudioContent();

            // Convertir a InputStream para reproducir directamente
            return new ByteArrayInputStream(audioContents.toByteArray());
        }
    }
}
