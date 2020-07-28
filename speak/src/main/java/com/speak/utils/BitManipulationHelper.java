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

    public ArrayList<String> convertAsciiToBinary(String input) {
        String bitString = "";
        // Terminal character added to detect end of transmitted data in receiver side
        input +=configuration.getTerminalChar();
        byte[] inputBytes = input.getBytes();

        for (byte i : inputBytes) {
            bitString += String.format("%8s", Integer.toBinaryString(i & 0xFF)).replace(' ', '0');
        }
        // Extra padding to avoid data loss
        bitString+="1";
        return new ArrayList<String>(Arrays.asList(bitString.split("(?!^)")));
    }

    public ArrayList<String> generatePseudoRandomSequence() {
        String seed = configuration.getSeedValue();
        String pseudoRandomSequence = "";
        List<String> seedArray = new ArrayList<>(Arrays.asList(seed.split("(?!^)")));
        int size = (int) (Math.pow(2, seed.length()-1) - 1);

        for (int i = 0; i < size; i++) {
            String xoredValue = xor(seedArray.get(0), seedArray.get(1));
            pseudoRandomSequence += seedArray.get(0);
            seedArray.remove(0);
            seedArray.add(xoredValue);
        }

        return new ArrayList<String>(Arrays.asList(pseudoRandomSequence.split("(?!^)")));
    }


    public ArrayList<Byte> interpolateBits(ArrayList<String> bitString, Integer interpolationSize) {
        ArrayList<Byte> interpolatedData = new ArrayList<>();
        for (int i = 0; i < bitString.size(); i++) {
            Byte interpolatedByte = Byte.parseByte(bitString.get(i));
            for (int j = 0; j < interpolationSize; j++) {
                interpolatedData.add(interpolatedByte);
            }
        }
        return interpolatedData;
    }


    public void addPreamble(ArrayList<Byte> encodedBits,ArrayList<String> pseudoRandomSequence){
        int[] preamble = new int[]{1,0,0,1,1,0};

        // adding the preamble
        for (int i=0; i<preamble.length; i++){
            for (int j=0; j<configuration.getSamplesPerCodeBit(); j++){
                encodedBits.add((byte) (2*preamble[i]-1));
            }
        }

        // adding the prbs sequence
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

        // Encoding the data by xoring the data and prbs code bits
        for (int i = 0; i < dataBitsLength; i++) {
            encodedBits.add(polarisedXor(interpolatedDataBits.get(i), interpolatedCodeBits.get(i % codeBitsLength)));
            // xored output is polarised ( 0's will be replaced by -1)
        }
        return encodedBits;
    }
}