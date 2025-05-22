package main.java.org.example;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import com.microsoft.cognitiveservices.speech.translation.*;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    public static BlockingQueue<AudioJob> micQueue = new LinkedBlockingQueue<>();
    public static BlockingQueue<AudioJob> sysQueue = new LinkedBlockingQueue<>();
    public static volatile boolean micTtsPlaying = false;
    private static final String MIC_MIXER = "Micrófono (2- USB PnP Audio Device)";
    private static final String SYS_MIXER = "CABLE Output (VB-Audio Virtual Cable)";
    private static final String AZURE_KEY = "DCJm7CU4OJTsH77IrfCtuCYRjs2NjvRtT0GJE1Ogr7ea8lgtNa7NJQQJ99BDACYeBjFXJ3w3AAAYACOGWQnN";
    private static final String AZURE_REGION = "eastus";

    public static void main(String[] args) throws Exception {
        // Hilo que reproduce en orden el audio del micrófono (VB-Cable)
        new Thread(() -> {
            while (true) {
                try {
                    AudioJob job = micQueue.take();
                    TtsPlayer.playOnDevice(job.audioStream, job.deviceName);
                    synchronized (Main.class) {
                        micTtsPlaying = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "MicPlaybackThread").start();

        // Hilo que reproduce en orden el audio del sistema (audífonos)
        new Thread(() -> {
            while (true) {
                try {
                    AudioJob job = sysQueue.take();
                    TtsPlayer.playOnDevice(job.audioStream, job.deviceName);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "SysPlaybackThread").start();

        // Iniciar hilos de captura y traducción
        startMicThread();
        startSysThread();

        Thread.currentThread().join();
    }

    private static void startMicThread() {
        Executors.newSingleThreadExecutor(r -> new Thread(r, "MicThread")).submit(() -> {
            try {
                AudioCapturer capturer = new AudioCapturer(MIC_MIXER);

                SpeechTranslationConfig config = SpeechTranslationConfig.fromSubscription(AZURE_KEY, AZURE_REGION);
                config.setSpeechRecognitionLanguage("es-ES");  // SIEMPRE ESPAÑOL ENTRANTE
                config.addTargetLanguage("en");               // SALIDA EN INGLÉS
                config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "200");

                PushAudioInputStream push = AudioInputStream.createPushStream(
                        AudioStreamFormat.getWaveFormatPCM(16000L, (short) 16, (short) 1));
                AudioConfig audioInput = AudioConfig.fromStreamInput(push);

                try (TranslationRecognizer recognizer = new TranslationRecognizer(config, audioInput)) {
                    recognizer.recognized.addEventListener((s, e) -> {
                        if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {
                            String translated = e.getResult().getTranslations().get("en");
                            System.out.printf("\n[MicThread][✓] es→en: %s%n", translated);

                                try {
                                    InputStream audioStream = ElevenLabsTTS.synthesize(translated);
                                    synchronized (Main.class) {
                                        micTtsPlaying = true;
                                    }
                                    TtsPlayer.playOnDevice(audioStream, "VB-Audio Virtual Cable");
                                    synchronized (Main.class) {
                                        micTtsPlaying = false;
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                        }
                    });

                    recognizer.startContinuousRecognitionAsync().get();

                    while (true) {
                        byte[] chunk = capturer.nextChunk();
                        push.write(chunk);
                    }
                }
            } catch (Exception ex) {
                System.err.printf("[MicThread] ERROR: %s%n", ex.getMessage());
            }
        });
    }

    private static void startSysThread() {
        Executors.newSingleThreadExecutor(r -> new Thread(r, "SysThread")).submit(() -> {
            try {
                AudioCapturer capturer = new AudioCapturer(SYS_MIXER);

                SpeechTranslationConfig config = SpeechTranslationConfig.fromSubscription(AZURE_KEY, AZURE_REGION);
                config.setSpeechRecognitionLanguage("en-US");  // SIEMPRE ESPAÑOL ENTRANTE
                config.addTargetLanguage("es");               // SALIDA EN INGLÉS
                config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "100");

                PushAudioInputStream push = AudioInputStream.createPushStream(
                        AudioStreamFormat.getWaveFormatPCM(16000L, (short) 16, (short) 1));
                AudioConfig audioInput = AudioConfig.fromStreamInput(push);

                try (TranslationRecognizer recognizer = new TranslationRecognizer(config,  audioInput)) {
                    recognizer.recognized.addEventListener((s, e) -> {
                        if (e.getResult().getReason() == ResultReason.TranslatedSpeech) {
                            String translated = e.getResult().getTranslations().get("es");
                            String detectedLang = e.getResult().getProperties().getProperty(
                                    PropertyId.SpeechServiceConnection_AutoDetectSourceLanguageResult);
                            System.out.printf("\n[SysThread][✓] %s→es: %s%n", detectedLang, translated);

                            try {
                                InputStream audioStream = GoogleCloudTTS.synthesize(translated);
                                sysQueue.put(new AudioJob(audioStream, "USB PnP Audio Device"));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        }
                    });

                    recognizer.startContinuousRecognitionAsync().get();

                    while (true) {
                        byte[] chunk = capturer.nextChunk();

                        // No dejar que el audio traducido del micrófono vuelva a entrar aquí
                        synchronized (Main.class) {
                            if (micTtsPlaying) continue;
                        }

                        push.write(chunk);
                    }
                }
            } catch (Exception ex) {
                System.err.printf("[SysThread] ERROR: %s%n", ex.getMessage());
            }
        });
    }
}
