package com.speak.sender;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.speak.utils.Configuration;

import java.util.ArrayList;


public class ToneGeneratorThread extends Thread {

    AudioTrack audioTrack;
    ArrayList<Byte> encodedBits;
    Configuration configuration;
    Handler handler;

    @Override
    public void run() {

        int bufferSize = AudioTrack.getMinBufferSize(configuration.getSamplingRate(),
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                configuration.getSamplingRate(),
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM);
        audioTrack.play();

        //TODO Add preamble
        for (int startIndex = 0; startIndex * configuration.getSamplesPerDataBit()< encodedBits.size() && !this.isInterrupted(); startIndex++) {
            playTone(startIndex * configuration.getSamplesPerDataBit());
            Log.d("ToneGen", "play tone called");
        }

        audioTrack.release();
        audioTrack = null;
        if (handler != null) {
            handler.sendMessage(new Message());
        }
    }

    //Called to play tone of specific frequency for specific duration
    public void playTone(int startIndex) {

        double samples[] = new double[configuration.getSamplesPerDataBit()];
        byte modulatedWaveData[] = new byte[2 * configuration.getSamplesPerDataBit()];
        double angle = (2 * Math.PI * configuration.getCarrierFrequency()) / configuration.getSamplingRate();

        for (int n = 0; n < configuration.getSamplesPerDataBit(); n++) {
            samples[n] = Math.sin(n * angle);
        }

        for (int i = 0; i < configuration.getSamplesPerDataBit(); i++) {

            // Amplitude * sin( 2 * pi * f * n ) / fs
            // f -> Carrier frequency
            // fs -> Sampling frequency

            final short sampleValue = (short) ((encodedBits.get(startIndex + i) * 32767 * samples[i]));
            final int index = 2 * i;
            modulatedWaveData[index] = (byte) (sampleValue & 0x00ff);
            modulatedWaveData[index + 1] = (byte) ((sampleValue & 0xff00) >>> 8);
        }

        try {
            // Play the track
            audioTrack.write(modulatedWaveData, 0, modulatedWaveData.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEncodedBits(ArrayList<Byte> encodedBits) {
        this.encodedBits = encodedBits;
    }

    ToneGeneratorThread(ArrayList<Byte> encodedBits, Configuration configuration, Handler handler) {
        this.encodedBits = encodedBits;
        this.configuration = configuration;
        this.handler = handler;
    }

    ToneGeneratorThread(ArrayList<Byte> encodedBits, Configuration configuration) {
        this.encodedBits = encodedBits;
        this.configuration = configuration;
    }

    public void abort() {
        this.interrupt();
        if (audioTrack != null) {
            audioTrack.stop();
        }
    }

}
