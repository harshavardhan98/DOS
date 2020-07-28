package com.speak.receiver;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import com.speak.utils.Configuration;
import java.util.concurrent.ArrayBlockingQueue;

public class RecorderThread extends Thread {
    Configuration configuration;
    ArrayBlockingQueue arrayBlockingQueue;
    AudioRecord audioRecord;

    public RecorderThread(Configuration configuration, ArrayBlockingQueue arrayBlockingQueue){
        this.configuration = configuration;
        this.arrayBlockingQueue = arrayBlockingQueue;
    }

    @Override
    public void run() {

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.DEFAULT,
                configuration.getSamplingRate(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                88200);
        // Record data for every 0.5 secs. Recording 22050 samples
        audioRecord.startRecording();

        while(!this.isInterrupted()){
            short[] buffer = new short[22050];
            audioRecord.read(buffer, 0, 22050);
            try {
                arrayBlockingQueue.put(buffer);
            } catch (InterruptedException e) {
                e.printStackTrace();
                this.interrupt();
            }
        }
    }

    public void abort(){
        this.interrupt();
        if (audioRecord!=null && audioRecord.getState()==AudioRecord.STATE_INITIALIZED){
            audioRecord.stop();
            audioRecord.release();
        }
    }

}
