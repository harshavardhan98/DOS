package com.speak.receiver;

import com.speak.utils.Configuration;
import java.util.Arrays;

public class ReceiverUtils {

    final Double[] filterCoefficients = new Double[]{ 0.0006,0.0021,0.0039,0.0039,-0.0011,-0.0121,-0.0245,-0.0274,-0.0078,0.0409,
                                                      0.1115,0.1826,0.2274,0.2274,0.1826,0.1115,0.0409,-0.0078,-0.0274,-0.0245,
                                                     -0.0121,-0.0011,0.0039,0.0039,0.0021,0.0006 };

    Configuration configuration;



    public ReceiverUtils(Configuration configuration) {
        this.configuration = configuration;
    }

    public double[] removeSine(short[] transmittedData){
        double[] processedData = multiplySine(transmittedData);
        processedData = performConvolution(processedData);
        return processedData;
    }


    public double[] multiplySine(short[] transmittedData){
        final int dsize = transmittedData.length;
        int n = dsize-147;
        double[] processedData = new double[dsize];

        for(int i=0;i<n;i++){
            processedData[147+i] = transmittedData[i]*transmittedData[147+i];
        }
        return processedData;
    }


    // todo -> where is the extra 25 bits ??? and one index problem
    public double[] performConvolution(double[] sineProcessedData){

        final int n = filterCoefficients.length , m = sineProcessedData.length;
        double[] processedData = new double[m];
        Arrays.fill(processedData,0);

        for(int i=n-1;i<m;i++){
            for(int j=0;j<n;j++){
                processedData[i] = processedData[i]+filterCoefficients[j] * sineProcessedData[i-j+1];
            }
        }

        // addition of hyteresis
        for(int i=0;i<processedData.length;i++){
            processedData[i] = (processedData[i]>0)?1:-1;
        }
        return processedData;
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

    public double[] reduceBlockDataToBits(double[] sineRemovedData){
        final int blocks = (int) Math.floor((sineRemovedData.length-25)/147);
        double[] dataBit = new double[blocks];
        Arrays.fill(dataBit,0);
        dataBit[0] = -1;

        for(int i=1;i<blocks;i++){
            for(int j=0;j<147;j++){
                dataBit[i] = dataBit[i] + sineRemovedData[(i-1)*147 + j +25];
            }
            dataBit[i] = ((dataBit[i]/147)<0)?(-1*dataBit[i-1]):dataBit[i-1];
        }
        return dataBit;
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

    public int correlation(double[] inputData,String pseudoRandomSequence){

        final double threshold = 0.8;
        double[] corr = new double[inputData.length];
        Arrays.fill(corr,0);
        int startIndex = -1;
        double maxData = Double.MIN_VALUE;

        for(int i=0;i<inputData.length-127;i++){
            for(int j=0;j<127;j++){
                corr[i] = corr[i]  + (double)(pseudoRandomSequence.charAt(j)-'0')*inputData[i+j-1];
            }

            if(corr[i]>=threshold){
                startIndex = i;
                break;
            }
        }
        return startIndex;
    }
}
