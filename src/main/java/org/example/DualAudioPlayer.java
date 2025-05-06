package main.java.org.example;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class DualAudioPlayer {

    private final SourceDataLine headphonesLine;
    private final SourceDataLine vbCableLine;

    public DualAudioPlayer() throws LineUnavailableException {
        headphonesLine = getOutputLine("USB PnP Audio Device");  // Tus audífonos
        vbCableLine = getOutputLine("VB-Audio Virtual Cable");   // VB-Cable Output
    }

    private SourceDataLine getOutputLine(String deviceName) throws LineUnavailableException {
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            if (mixerInfo.getName().contains(deviceName)) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                AudioFormat format = getAudioFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                SourceDataLine line = (SourceDataLine) mixer.getLine(info);
                line.open(format);
                line.start();
                return line;
            }
        }
        throw new LineUnavailableException("Dispositivo no encontrado: " + deviceName);
    }

    public void play(byte[] audioData) {
        // Envía el mismo audio traducido a ambos dispositivos
        if (headphonesLine != null) {
            headphonesLine.write(audioData, 0, audioData.length);
        }
        if (vbCableLine != null) {
            vbCableLine.write(audioData, 0, audioData.length);
        }
    }

    public void close() {
        if (headphonesLine != null) headphonesLine.close();
        if (vbCableLine != null) vbCableLine.close();
    }

    private AudioFormat getAudioFormat() {
        return new AudioFormat(16000, 16, 1, true, false); // Mono 16kHz, PCM signed
    }
}
