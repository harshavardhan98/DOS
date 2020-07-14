package com.speak.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BitManipulationHelper {

    Configuration configuration;

    public BitManipulationHelper(Configuration configuration) {
        this.configuration = configuration;
    }

    String xor(String c1, String c2) {
        return (c1.equals(c2)) ? "0" : "1";
    }


    Byte polarisedXor(Byte a, Byte b) {
        return a.equals(b) ? (byte) -1 : (byte) 1;
    }

    public ArrayList<String> convertAsciiToBinary(final String input) {
        String bitString = "";
        byte[] inputBytes = input.getBytes();

        for (byte i : inputBytes) {
            bitString += String.format("%8s", Integer.toBinaryString(i & 0xFF)).replace(' ', '0');
        }

        return new ArrayList<String>(Arrays.asList(bitString.split("(?!^)")));
    }

    public ArrayList<String> generatePseudoRandomSequence() {
        String seed = configuration.getSeedValue();
        String pseudoRandomSequence = "";
        List<String> seedArray = new ArrayList<>(Arrays.asList(seed.split("(?!^)")));
        // todo: Check for Integer overflow
        int size = (int) (Math.pow(2, seed.length()-1) - 1);

        for (int i = 0; i < size; i++) {
            String xoredValue = xor(seedArray.get(0), seedArray.get(1));
            pseudoRandomSequence += seedArray.get(0);
            seedArray.remove(0);
            seedArray.add(xoredValue);
        }

        return new ArrayList<String>(Arrays.asList(pseudoRandomSequence.split("(?!^)")));
    }

    public ArrayList<Byte> interpolateDataBits(ArrayList<String> bitString) {

        ArrayList<Byte> interpolatedData = new ArrayList<>();
        // todo : Check whether we can apply ceil to interpolationRate

        for (int i = 0; i < bitString.size(); i++) {
            Byte interpolatedByte = Byte.parseByte(bitString.get(i));
            for (int j = 0; j < configuration.getSamplesPerDataBit(); j++) {
                interpolatedData.add(interpolatedByte);
            }
        }

        return interpolatedData;
    }

    public ArrayList<Byte> interpolateCodeBits(ArrayList<String> pseudoRandomSequence) {

        ArrayList<Byte> interpolatedCode = new ArrayList<>();

        for (int i = 0; i < pseudoRandomSequence.size(); i++) {
            Byte interpolatedByte = Byte.parseByte(pseudoRandomSequence.get(i));
            for (int j = 0; j < configuration.getSamplesPerCodeBit(); j++) {
                interpolatedCode.add(interpolatedByte);
            }
        }

        return interpolatedCode;
    }

    public void addPreamble(ArrayList<Byte> encodedBits,ArrayList<String> pseudoRandomSequence){

        // adding preamble before PRBS bits and data bits
        int[] preamble = new int[]{1,0,0,1,1,0};
        for (int i=0; i<preamble.length; i++){
            for (int j=0; j<configuration.getSamplesPerCodeBit(); j++){
                encodedBits.add((byte) (2*preamble[i]-1));
            }
        }

        //todo Ask Kamal if this would be necessary
//        // adding -1 bits for 200ms to sync receiver even during receiver delays
//        for(int i=0;i<configuration.getSamplingRate()*0.2;i++){
//            encodedBits.add((byte)-1);
//        }

        for(int i=0;i<pseudoRandomSequence.size();i++){
            byte sampleBit = pseudoRandomSequence.get(i).equals("0")?(byte)-1:(byte)1;
            for(int j=0;j<configuration.getSamplesPerCodeBit();j++){
                encodedBits.add(sampleBit);
            }
        }
    }

    public ArrayList<Byte> encodeBits(ArrayList<Byte> interpolatedCodeBits,
                                      ArrayList<Byte> interpolatedDataBits,
                                      ArrayList<String> pseudoRandomSequence) {

        final int dataBitsLength = interpolatedDataBits.size();
        final int codeBitsLength = interpolatedCodeBits.size();

        ArrayList<Byte> encodedBits = new ArrayList<>();
        addPreamble(encodedBits,pseudoRandomSequence);
        for (int i = 0; i < dataBitsLength; i++) {
            encodedBits.add(polarisedXor(interpolatedDataBits.get(i), interpolatedCodeBits.get(i % codeBitsLength)));
            // xored output is polarised ( 0's will be replaced by -1)
        }
        // 423360+0.2*44100+127*147 -> 4,50,849
        return encodedBits;
    }
}