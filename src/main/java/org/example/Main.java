package main.java.org.example;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import com.microsoft.cognitiveservices.speech.translation.*;
import javazoom.jl.player.Player;

import java.io.InputStream;
import java.util.concurrent.Executors;

public class Main {
    public static volatile boolean micTtsPlaying = false;
    // Ajusta estos nombres según tu lista de mixers:
    private static final String MIC_MIXER   = "Micrófono (2- USB PnP Audio Device)";
    private static final String SYS_MIXER   = "CABLE Output (VB-Audio Virtual Cable)";
    private static final String AZURE_KEY = "DCJm7CU4OJTsH77IrfCtuCYRjs2NjvRtT0GJE1Ogr7ea8lgtNa7NJQQJ99BDACYeBjFXJ3w3AAAYACOGWQnN";
    private static final String AZURE_REGION = "eastus";
    public static void main(String[] args) throws Exception {
        // Hilo que traduce tu voz (ES→EN)
        startThread("MicThread", MIC_MIXER, "es-ES", "en");
        // Hilo que traduce sistema (EN→ES)
        startThread("SysThread", SYS_MIXER, "en-US", "es");

        // Mantén viva la app
        Thread.currentThread().join();
    }

    private static void startThread(String name, String mixerName, String fromLang, String toLang) {
        Executors.newSingleThreadExecutor(r -> new Thread(r, name))
                .submit(() -> {
                    try {
                        AudioCapturer capturer = new AudioCapturer(mixerName);

                        SpeechTranslationConfig config =
                                SpeechTranslationConfig.fromSubscription(AZURE_KEY, AZURE_REGION);
                        config.setSpeechRecognitionLanguage(fromLang);
                        config.addTargetLanguage(toLang);
                        config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "200");
                        // bajamos timeout a 300 ms para que detecte pausas cortas

                        PushAudioInputStream push = AudioInputStream.createPushStream(
                                AudioStreamFormat.getWaveFormatPCM(16000L, (short) 16, (short) 1));
                        AudioConfig audioInput = AudioConfig.fromStreamInput(push);

                        try (TranslationRecognizer recognizer = new TranslationRecognizer(config, audioInput)) {
                            // 4) Listener para traducción final
                            recognizer.recognized.addEventListener((s, e) -> {
                                if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {
                                    String full = e.getResult().getTranslations().get(toLang);
                                    System.out.printf("\n[%s][✓] %s→%s: %s%n", name, fromLang, toLang, full);

                                    // 5) TTS en hilo separado
                                    Executors.newSingleThreadExecutor().submit(() -> {
                                        try (InputStream mp3 = ElevenLabsTTS.synthesize(full)) {
                                            if (name.equals("MicThread")) {
                                                Main.micTtsPlaying = true;
                                                TtsPlayer.playOnDevice(mp3, "VB-Audio Virtual Cable");
                                                Main.micTtsPlaying = false;
                                            } else {
                                                TtsPlayer.playOnDevice(mp3, "USB PnP Audio Device");
                                            }

                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    });
                                }
                            });

                            // arranca el streaming
                            recognizer.startContinuousRecognitionAsync().get();

                            // loop que alimenta los chunks
                            while (true) {
                                byte[] chunk = capturer.nextChunk();
                                if (name.equals("SysThread") && Main.micTtsPlaying) {
                                    continue;
                                }
                                push.write(chunk);
                            }
                        }
                    } catch (Exception ex) {
                        System.err.printf("[%s] ERROR: %s%n", name, ex.getMessage());
                    }
                });
    }

}