package main.java.org.example;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.JavaSoundAudioDevice;
import javazoom.jl.player.advanced.AdvancedPlayer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class Mp3ToPcmConverter {
    public static byte[] convert(InputStream mp3) throws Exception {
        ByteArrayOutputStream pcmOut = new ByteArrayOutputStream();

        JavaSoundAudioDevice device = new JavaSoundAudioDevice() {
            @Override
            protected void writeImpl(short[] samples, int offs, int len) throws JavaLayerException {
                for (int i = 0; i < len; i++) {
                    short s = samples[offs + i];
                    pcmOut.write(s & 0xFF);
                    pcmOut.write((s >> 8) & 0xFF);
                }
            }
        };

        AdvancedPlayer player = new AdvancedPlayer(mp3, device);
        player.play();
        return pcmOut.toByteArray();
    }
}
