package com.speak.receiver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.speak.utils.Configuration;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ReceiverUtils {
    public static final String BLOCKS_KEY = "block_data";
    public static final String PREFIX_KEY = "prefix_data";

    final Double[] lpfCoefficients = new Double[]{ 0.0006,0.0021,0.0039,0.0039,-0.0011,-0.0121,-0.0245,-0.0274,-0.0078,0.0409,
                                                      0.1115,0.1826,0.2274,0.2274,0.1826,0.1115,0.0409,-0.0078,-0.0274,-0.0245,
                                                     -0.0121,-0.0011,0.0039,0.0039,0.0021,0.0006 };

    Configuration configuration;



    public ReceiverUtils(Configuration configuration) {
        this.configuration = configuration;
    }

    public Integer[] removeSine(short[] transmittedData, Double[] prefix){
        Double[] processedData = highPassFilter(transmittedData, lpfCoefficients);
        processedData = multiplySine(processedData, prefix);
        processedData = lowPassFilter(processedData, lpfCoefficients);
        return polarizeData(processedData);
    }

    public Double[] multiplySine(Double[] highPassFilterResult, Double[] prefix){
        final int dataSize = highPassFilterResult.length;
        Double[] processedData = new Double[dataSize];

        for(int i=0;i<configuration.getSamplesPerCodeBit();i++){
            processedData[i] = prefix[i]*highPassFilterResult[147+i];
        }
        for(int i=configuration.getSamplesPerCodeBit();i<dataSize;i++){
            processedData[i] = highPassFilterResult[i-configuration.getSamplesPerCodeBit()]*highPassFilterResult[i];
        }

        return processedData;
    }

    // todo -> where is the extra 25 bits ??? and one index problem
    public Double[] lowPassFilter(Double[] sineProcessedData, Double[] filterCoefficients){

        final int n = filterCoefficients.length , m = sineProcessedData.length;
        Double[] processedData = new Double[m];
        Arrays.fill(processedData,0);

        for(int i=n;i<m+n;i++){
            for(int j=0;j<n;j++){
                processedData[i-n] = processedData[i-n]+filterCoefficients[j] * sineProcessedData[i-j-1];
            }
        }
        return processedData;
    }

    public Double[] highPassFilter(short[] sineProcessedData, Double[] filterCoefficients){

        final int n = filterCoefficients.length , m = sineProcessedData.length;
        Double[] processedData = new Double[m];
        Arrays.fill(processedData,0);

        for(int i=n;i<m+n;i++){
            for(int j=0;j<n;j++){
                processedData[i-n] = processedData[i-n]+filterCoefficients[j] * sineProcessedData[i-j-1];
            }
            processedData[i-n] = sineProcessedData[i-n] - processedData[i-n];
        }
        return processedData;
    }

    public Integer[] polarizeData(Double[] unPolarizedData){

        Integer[] polarizedData = new Integer[unPolarizedData.length];
        for(int i=0;i<unPolarizedData.length;i++){
            polarizedData[i] = (unPolarizedData[i]>0)?1:-1;
        }
        return polarizedData;
    }

/*
    blocks = floor(dsize/147);
    y_eqc = y_eq(13:end-12);
    y_bit = zeros(1,blocks);
    y_bit(1) = -1;
    for i = 2:blocks
        for j = 1:147
        y_bit(i) = y_bit(i)+y_eqc((i-1)*147 + j);
        end
        y_bit(i) = y_bit(i)/147;
        end
  */

    public HashMap<String, Integer[]> reduceBlockDataToBits(Integer[] processedData, int startIndex, Integer[] prefix){
        ArrayList<Integer> blockData = new ArrayList<>();
        ArrayList<Integer> newPrefix = new ArrayList<>();
        HashMap<String, Integer[]> result = new HashMap<>();

        if(prefix.length>0){
            ArrayList<Integer> prefixArrayList = new ArrayList<>(Arrays.asList(prefix));
            prefixArrayList.addAll(Arrays.asList(processedData));
            processedData = prefixArrayList.toArray(new Integer[0]);
        }
        while(startIndex<processedData.length){
            if(startIndex + configuration.getSamplesPerCodeBit() -1 <= processedData.length){
                int sum = 0;
                for(int i=startIndex; i< startIndex+configuration.getSamplesPerCodeBit(); i++){
                    sum+=processedData[i];
                }
                startIndex+=configuration.getSamplesPerCodeBit();
                blockData.add((sum/configuration.getSamplesPerCodeBit() > 0)?1:-1);
            }else {
                for(int i=startIndex; i<processedData.length; i++){
                    newPrefix.add(processedData[i]);
                }
                startIndex+=configuration.getSamplesPerCodeBit();
            }
        }
        result.put(PREFIX_KEY, newPrefix.toArray(new Integer[0]));
        result.put(BLOCKS_KEY, blockData.toArray(new Integer[0]));
        return result;
    }

    /*
    *
        n= offset1 + 127 + offset2;
        corr = zeros(1,n);
        for i = 1:(n-127)
            for j = 1:127
                corr(i) = corr(i)+code(j)*data(i+j-1);
            end
        end
    * */

    public int correlation(Double[] inputData,String pseudoRandomSequence){

        final Double threshold = 0.8;
        Double[] corr = new Double[inputData.length];
        Arrays.fill(corr,0);
        int startIndex = -1;
        Double maxData = Double.MIN_VALUE;

        for(int i=0;i<inputData.length-127;i++){
            for(int j=0;j<127;j++){
                corr[i] = corr[i]  + (Integer)(pseudoRandomSequence.charAt(j)-'0')*inputData[i+j-1];
            }

            if(corr[i]>=threshold){
                startIndex = i;
                break;
            }
        }
        return startIndex;
    }
}
