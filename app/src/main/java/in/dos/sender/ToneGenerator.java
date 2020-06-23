package in.dos.sender;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;
import in.dos.utils.Configuration;

public class ToneGenerator extends AsyncTask<Integer, Integer, Void> {

    AudioTrack audioTrack;

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

        playTone(Configuration.CARRIER_FREQUENCY-8000,1);

        Log.d("TAG","Sound play success");
        audioTrack.release();
        return null;
    }

    //Called to play tone of specific frequency for specific duration
    public void playTone(double freqOfTone, double duration) {
        //Calculate number of samples in given duration
        double dnumSamples = duration * Configuration.SAMPLING_RATE;
        dnumSamples = Math.ceil(dnumSamples);
        int numSamples = (int) dnumSamples;
        double sample[] = new double[numSamples];
        //Every sample 16bit
        byte generatedSnd[] = new byte[2 * numSamples];
        //Fill the sample array with sin of given frequency
        double anglePadding = (freqOfTone * 2 * Math.PI) / (Configuration.SAMPLING_RATE);
        double angleCurrent = 0;
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(angleCurrent);
            angleCurrent += anglePadding;
        }
        //Convert to 16 bit pcm (pulse code modulation) sound array
        //assumes the sample buffer is normalized.
        int idx = 0;
        int i = 0 ;
        //Amplitude ramp as a percent of sample count
        int ramp = numSamples / 20 ;
        //Ramp amplitude up (to avoid clicks)
        for (i = 0; i< ramp; ++i) {
            double dVal = sample[i];
            //Ramp up to maximum
            final short val = (short) ((dVal * 32767 * i/ramp));
            //In 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        // Max amplitude for most of the samples
        for (i = i; i< numSamples - ramp; ++i) {
            double dVal = sample[i];
            //Scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            //In 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        //Ramp amplitude down
        for (i = i; i< numSamples; ++i) {
            double dVal = sample[i];
            //Ramp down to zero
            final short val = (short) ((dVal * 32767 * (numSamples-i)/ramp ));
            //In 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        try {
            // Play the track
            audioTrack.write(generatedSnd, 0, generatedSnd.length);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
