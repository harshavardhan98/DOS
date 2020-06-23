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


    Byte xor(Byte a, Byte b) {
        return a.equals(b) ? (byte) 0 : (byte) 1;
    }

    public ArrayList<String> getBits(final String input) {
        String bitString = "";
        byte[] inputBytes = input.getBytes();

        for (byte i : inputBytes) {
            bitString += String.format("%8s", Integer.toBinaryString(i & 0xFF)).replace(' ', '0');
        }

        return new ArrayList<String>(Arrays.asList(bitString.split("")));
    }

    public ArrayList<String> generatePseudoRandomSequence(final String seed) {
        String pseudoRandomSequence = "";
        List<String> seedArray = new ArrayList<>(Arrays.asList(seed.split("")));
        // todo: Check for Integer overflow
        int size = (int) (Math.pow(2, seed.length()) - 1);

        for (int i = 0; i < size; i++) {
            String xoredValue = xor(seedArray.get(0), seedArray.get(1));
            pseudoRandomSequence += seedArray.get(0);
            seedArray.remove(0);
            seedArray.add(xoredValue);
        }

        return new ArrayList<String>(Arrays.asList(pseudoRandomSequence.split("")));
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

    public ArrayList<Byte> encodeBits(ArrayList<Byte> interpolatedCodeBits, ArrayList<Byte> interpolatedDataBits) {

        final int dataBitsLength = interpolatedDataBits.size();
        final int codeBitsLength = interpolatedCodeBits.size();

        ArrayList<Byte> encodedBits = new ArrayList<>();

        for (int i = 0; i < dataBitsLength; i++) {
            encodedBits.add(xor(interpolatedDataBits.get(i), interpolatedCodeBits.get(i % codeBitsLength)));
        }

        return encodedBits;
    }

}