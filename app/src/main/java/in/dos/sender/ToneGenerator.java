package in.dos.sender;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;

import in.dos.utils.Configuration;

public class ToneGenerator extends AsyncTask<Integer, Integer, Void> {

    AudioTrack audioTrack;
    ArrayList<Byte> encodedBits;

    @Override
    protected Void doInBackground(Integer... integers) {

        int bufferSize = AudioTrack.getMinBufferSize(Configuration.SAMPLING_RATE,
                                                     AudioFormat.CHANNEL_OUT_MONO,
                                                     AudioFormat.ENCODING_PCM_16BIT);

        Log.d("TAG","buffer size: "+bufferSize);

        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                                    Configuration.SAMPLING_RATE,
                                    AudioFormat.CHANNEL_OUT_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT,
                                    bufferSize,
                                    AudioTrack.MODE_STREAM);
        audioTrack.play();

        //TODO Add preamble
        for( int startIndex=0; startIndex*Configuration.SAMPLES_PER_DATA_BIT < encodedBits.size(); startIndex++){
            playTone(startIndex*Configuration.SAMPLES_PER_DATA_BIT);
        }

        Log.d("TAG","Sound play success");
        audioTrack.release();
        return null;
    }

    //Called to play tone of specific frequency for specific duration
    public void playTone(int startIndex) {

        double samples[] = new double[Configuration.SAMPLES_PER_DATA_BIT];

        byte modulatedWaveData[] = new byte[2 * Configuration.SAMPLES_PER_DATA_BIT];

        double angle = (2 * Math.PI * Configuration.CARRIER_FREQUENCY) / Configuration.SAMPLING_RATE;

        for( int n=0; n<Configuration.SAMPLES_PER_DATA_BIT; n++){
            samples[n] = Math.sin( n*angle );
        }

        for (int i=0; i<Configuration.SAMPLES_PER_DATA_BIT; i++){

            // Amplitude * sin( 2 * pi * f * n ) / fs
            // f -> Carrier frequency
            // fs -> Sampling frequency

            final short sampleValue = (short) ((encodedBits.get(startIndex+i) * samples[i]));
            final int index = 2*i;
            modulatedWaveData[index] = (byte) (sampleValue & 0x00ff);
            modulatedWaveData[index + 1] = (byte) ((sampleValue & 0xff00) >>> 8);
        }

        try {
            // Play the track
            audioTrack.write(modulatedWaveData, 0, modulatedWaveData.length);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    public void setEncodedBits(ArrayList<Byte> encodedBits) {
        this.encodedBits = encodedBits;
    }

    ToneGenerator(ArrayList<Byte> encodedBits){
        this.encodedBits = encodedBits;
    }

}
