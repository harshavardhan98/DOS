package in.dos.sender;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import in.dos.utils.BitManipulationHelper;
import in.dos.utils.Configuration;

public class Sender {

    public void sendData(final String input){

        BitManipulationHelper bitManipulationHelper = new BitManipulationHelper();

        ArrayList<String> inputBits = bitManipulationHelper.getBits(input);
        ArrayList<String> pseudoRandomSequence = bitManipulationHelper.generatePseudoRandomSequence(Configuration.SEED_VALUE);
        ArrayList<Byte> interpolatedDataBits = bitManipulationHelper.interpolateDataBits(inputBits);
        ArrayList<Byte> interpolatedCodeBits = bitManipulationHelper.interpolateCodeBits(pseudoRandomSequence);
        ArrayList<Byte> encodedBits = bitManipulationHelper.encodeBits(interpolatedCodeBits, interpolatedDataBits);

        ToneGenerator toneGenerator = new ToneGenerator(encodedBits);
        toneGenerator.execute();
        Log.d("Sender", "Tone Generation Started.");

    }

}
