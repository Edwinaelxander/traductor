package main.java.org.example;

import javax.sound.sampled.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioCapturer {
    private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
    private TargetDataLine line;

    /**
     * Construye un capturador que lee de `mixerName` en 16 kHz mono.
     */
    public AudioCapturer(String mixerName) throws LineUnavailableException {
        AudioFormat fmt = new AudioFormat(16000, 16, 1, true, false);
        Mixer.Info selected = null;
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            if (mi.getName().equalsIgnoreCase(mixerName)) {
                selected = mi; break;
            }
        }
        Mixer mixer = AudioSystem.getMixer(selected);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, fmt);
        line = (TargetDataLine) mixer.getLine(info);
        line.open(fmt);
        line.start();

        Thread t = new Thread(() -> {
            byte[] buf = new byte[4096];
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    int r = line.read(buf, 0, buf.length);
                    if (r > 0) {
                        byte[] chunk = new byte[r];
                        System.arraycopy(buf, 0, chunk, 0, r);
                        queue.put(chunk);
                    }
                }
            } catch (InterruptedException e) {
            }
        }, "AudioCapturer-" + mixerName);
        t.setDaemon(true);
        t.start();
    }

    /** Bloquea hasta que haya un chunk disponible. */
    public byte[] nextChunk() throws InterruptedException {
        return queue.take();
    }

    public void stop() {
        line.stop();
        line.close();
    }
}
