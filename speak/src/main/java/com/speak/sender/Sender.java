package com.speak.sender;

import android.util.Log;
import com.speak.utils.BitManipulationHelper;
import com.speak.utils.Configuration;
import java.util.ArrayList;

public class Sender {

    public void sendData(final String input){

        BitManipulationHelper bitManipulationHelper = new BitManipulationHelper();

        ArrayList<String> inputBits = bitManipulationHelper.getBits(input);
        ArrayList<String> pseudoRandomSequence = bitManipulationHelper.generatePseudoRandomSequence(Configuration.SEED_VALUE);
        ArrayList<Byte> interpolatedDataBits = bitManipulationHelper.interpolateDataBits(inputBits);
        ArrayList<Byte> interpolatedCodeBits = bitManipulationHelper.interpolateCodeBits(pseudoRandomSequence);
        ArrayList<Byte> encodedBits = bitManipulationHelper.encodeBits(interpolatedCodeBits, interpolatedDataBits);

        ToneGeneratorThread toneGeneratorThread = new ToneGeneratorThread(encodedBits);
        toneGeneratorThread.execute();
        Log.d("Sender", "Tone Generation Started.");

    }

}
