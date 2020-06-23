package in.dos.utils;

import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BitManipulationHelper {

    public ArrayList<String> getBits(final String input){
        String bitString = "";
        byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);

        for(byte i: inputBytes){
            bitString+= String.format("%8s", Integer.toBinaryString(i & 0xFF)).replace(' ', '0');
        }

        return new ArrayList<String>(Arrays.asList(bitString.split("")));
    }


    String xor(String c1, String c2){
        return (c1.equals(c2))?"0":"1";
    }


    Byte xor(Byte a, Byte b){ return a.equals(b) ? (byte)0 : (byte)1; }


    public ArrayList<String> generatePseudoRandomSequence(final String seed){
        String pseudoRandomSequence = "";
        List<String> seedArray = new ArrayList<>(Arrays.asList(seed.split("")));
        // todo: Check for Integer overflow
        int size= (int) (Math.pow(2,seed.length())-1);

        for(int i=0;i<size;i++){
            String xoredValue = xor(seedArray.get(0),seedArray.get(1));
            pseudoRandomSequence +=  seedArray.get(0);
            seedArray.remove(0);
            seedArray.add(xoredValue);
            Log.d("test",seedArray.toString());
        }

        return new ArrayList<String>(Arrays.asList(pseudoRandomSequence.split("")));
    }

    /*
    *   clear all;
        clc;
        close all;
        n_bits = 10;
        fs = 44.1e3;        %sampling rate - 44.1KHz for audio signal
        SF = 4;             %spreading factor
        b = randi([0 1],1,n_bits);
        c1 = PRBS_gen();     %PRBS generation
        [m,bit_size] = size(c1);
        Tb = 0.2;           %bit duration
        int_rate = Tb*fs;   %interpolation factor = fs/fb
        x = [];
        for i=1:n_bits      %interpolation of data bits
           for j = 1:int_rate
               x = [x b(i)];
           end
        end
        % [x_axis,y_axis]= fftdBplot(x,fs,1024*4);
        Tc = Tb/SF;          %bit duration of codes -> 0.05
        int_rate_c = Tc*fs  %interpolation of code words ->   2205
        c = [];
        for i=1:bit_size     %interpolation of code bits
           for j = 1:int_rate_c
               c = [c c1(i)];
           end
        end
        % figure;
        % [x_axis,y_axis] = fftdBplot(c,fs,1024*4);
        d = xor(x,c(1:88200));
        plot(x);
        figure;
        plot(d);

    * */

    public ArrayList<Byte> interpolateDataBits(ArrayList<String> bitString){

        ArrayList<Byte> interpolatedData = new ArrayList<>();
        // todo : Check whether we can apply ceil to interpolationRate

        for(int i=0;i<bitString.size();i++){
            Byte interpolatedByte = Byte.parseByte(bitString.get(i));
            for(int j = 0; j<Configuration.SAMPLES_PER_DATA_BIT; j++){
                interpolatedData.add(interpolatedByte);
            }
        }

        return interpolatedData;
    }

    public ArrayList<Byte> interpolateCodeBits(ArrayList<String> pseudoRandomSequence){

        ArrayList<Byte> interpolatedCode = new ArrayList<>();

        for(int i=0;i<pseudoRandomSequence.size();i++){
            Byte interpolatedByte = Byte.parseByte(pseudoRandomSequence.get(i));
            for(int j=0;j<Configuration.CODE_BIT_DURATION;j++){
                interpolatedCode.add(interpolatedByte);
            }
        }

        return interpolatedCode;
    }

    public ArrayList<Byte> encodeBits(ArrayList<Byte> interpolatedCodeBits, ArrayList<Byte> interpolatedDataBits){

        final int dataBitsLength = interpolatedDataBits.size();
        final int codeBitsLength = interpolatedCodeBits.size();

        ArrayList<Byte> encodedBits = new ArrayList<>();

        for(int i=0;i<dataBitsLength;i++){
            encodedBits.add(xor(interpolatedDataBits.get(i), interpolatedCodeBits.get(i%codeBitsLength)));
        }

        return encodedBits;
    }

}