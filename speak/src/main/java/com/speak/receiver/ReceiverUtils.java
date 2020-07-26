package com.speak.receiver;

import com.speak.utils.Configuration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;


public class ReceiverUtils {
    public static final String BLOCKS_KEY = "block_data";
    public static final String PREFIX_KEY = "prefix_data";

    final Double[] lpfCoefficients = new Double[]{
                                                    -0.0005,-0.0018,-0.0044,-0.0078,-0.0108,-0.0105,-0.0036,0.0129,0.0397,0.0744,
                                                     0.1110,0.1419,0.1596,0.1596,0.1419,0.1110,0.0744,0.0397,0.0129,-0.0036,
                                                    -0.0105,-0.0108,-0.0078,-0.0044,-0.0018,-0.0005 };

    final Double[] hpfCoefficients = new Double[] {
                                                    0.0010,-0.0009,-0.0021,0.0065,-0.0051,-0.0074,0.0238,-0.0219,-0.0153,0.0715,
                                                    -0.0882, -0.0213, 0.5598 ,0.5598,-0.0213,-0.0882,0.0715,-0.0153,-0.0219,0.0238,
                                                    -0.0074,-0.0051,0.0065,-0.0021,-0.0009,0.0010
                                                  };


    Configuration configuration;



    public ReceiverUtils(Configuration configuration) {
        this.configuration = configuration;
    }

    public Integer[] removeSine(short[] transmittedData, Double[] prefix,Double[] hpfPrefix,Double[] lpfPrefix){
        Double[] processedData = highPassFilter(transmittedData, hpfCoefficients,hpfPrefix);
        processedData = multiplySine(processedData, prefix);
        processedData = lowPassFilter(processedData, lpfCoefficients,lpfPrefix);
        return polarizeData(processedData);
    }

    public Double[] multiplySine(Double[] highPassFilterResult, Double[] prefix){
        final int dataSize = highPassFilterResult.length;
        Double[] processedData = new Double[dataSize];

        for(int i=0;i<dataSize;i++){
            if(i-configuration.getSamplesPerCodeBit()<0){
                processedData[i] = prefix[i]*highPassFilterResult[i];
            }else{
                processedData[i] = highPassFilterResult[i-configuration.getSamplesPerCodeBit()]*highPassFilterResult[i];
            }
        }
        for(int i =0; i<prefix.length; i++){
            prefix[i] = highPassFilterResult[highPassFilterResult.length - prefix.length+i];
        }

        return processedData;
    }


    public Double[] lowPassFilter(Double[] sineProcessedData, Double[] filterCoefficients,Double[] lpfPrefix){

        final int n = filterCoefficients.length , m = sineProcessedData.length;
        Double[] processedData = new Double[m];
        Arrays.fill(processedData,0.0);

        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(i-j<0){
                    processedData[i] = processedData[i]+filterCoefficients[j] * lpfPrefix[lpfPrefix.length+(i-j)];
                }else{
                    processedData[i] = processedData[i]+filterCoefficients[j] * sineProcessedData[i-j];
                }
            }
        }

        for(int i =0; i<lpfPrefix.length; i++){
            lpfPrefix[i] = sineProcessedData[sineProcessedData.length-lpfPrefix.length+i];
        }
        return processedData;
    }

    public Double[] highPassFilter(short[] transmittedData, Double[] filterCoefficients,Double[] hpfPrefix){

        final int n = filterCoefficients.length , m = transmittedData.length;
        Double[] processedData = new Double[m];
        Arrays.fill(processedData,0.0);

        for(int i=0;i<m;i++){
            for(int j=0;j<n;j++){
                if(i-j<0){
                    processedData[i] = processedData[i]+filterCoefficients[j] * hpfPrefix[hpfPrefix.length+(i-j)];
                }else{
                    processedData[i] = processedData[i]+filterCoefficients[j] * transmittedData[i-j];
                }
            }
            processedData[i] = transmittedData[i] - processedData[i];
        }

        for(int i =0; i<hpfPrefix.length; i++){
            hpfPrefix[i] = (double) transmittedData[transmittedData.length-hpfPrefix.length+i];
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
            if(startIndex + configuration.getSamplesPerCodeBit() -1 < processedData.length){
                int sum = 0;
                for(int i=startIndex; i< startIndex+configuration.getSamplesPerCodeBit(); i++){
                    sum+=processedData[i];
                }
                startIndex+=configuration.getSamplesPerCodeBit();
                blockData.add((sum/(double)configuration.getSamplesPerCodeBit() > 0.0)?1:-1);
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

    private Double[] combineArrays(Double[] prefix, Double[] suffix){
        ArrayList<Double> combinedArrayList = new ArrayList(Arrays.asList(prefix));
        combinedArrayList.addAll(Arrays.asList(suffix));
        Double[] combinedArray = combinedArrayList.toArray(new Double[0]);
        return combinedArray;
    }
}
