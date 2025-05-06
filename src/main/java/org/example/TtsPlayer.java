package main.java.org.example;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class TtsPlayer {

    public static void playOnDevice(InputStream mp3Stream, String deviceName) throws Exception {
        // 1) Primero decodifica MP3 a PCM con AudioSystem + SPI
        try (BufferedInputStream bis = new BufferedInputStream(mp3Stream);
             AudioInputStream mp3Audio = AudioSystem.getAudioInputStream(bis))
        {
            AudioFormat baseFormat = mp3Audio.getFormat();
            AudioFormat pcmFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            try (AudioInputStream pcmStream = AudioSystem.getAudioInputStream(pcmFormat, mp3Audio)) {

                // 2) Abre la línea en el mixer elegido
                Mixer.Info mixerInfo = null;
                for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
                    if (mi.getName().contains(deviceName)) {
                        mixerInfo = mi;
                        break;
                    }
                }
                if (mixerInfo == null) {
                    throw new IllegalArgumentException("Mixer no encontrado: " + deviceName);
                }

                DataLine.Info info = new DataLine.Info(SourceDataLine.class, pcmFormat);
                try (SourceDataLine line = (SourceDataLine)
                        AudioSystem.getMixer(mixerInfo).getLine(info))
                {
                    line.open(pcmFormat);
                    line.start();

                    // 3) Lee y escribe en el line
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = pcmStream.read(buffer, 0, buffer.length)) != -1) {
                        line.write(buffer, 0, read);
                    }
                    line.drain();
                }
            }
        }
    }
}
