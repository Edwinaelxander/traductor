package main.java.org.example;

import java.io.InputStream;

public class AudioJob {
    public InputStream audioStream;
    public String deviceName;

    public AudioJob(InputStream audioStream, String deviceName) {
        this.audioStream = audioStream;
        this.deviceName = deviceName;
    }
}
