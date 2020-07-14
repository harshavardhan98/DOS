package com.speak.receiver;

import android.os.Handler;

import com.speak.utils.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

/*
  1. Filter define
  2. RSSI
  3. SineRemoval
  4. Low Pass filter & amplify
* */


//1 1 1 1 1 1 1 1 1 1 1 1 . 1 1 1 1 1 1 1 1 . 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1


public class DataProcessorThread extends Thread{
    Configuration configuration;
    ArrayBlockingQueue arrayBlockingQueue;
    Receiver.ReceiverCallBack receiverCallBack;
    Handler handler;
    ReceiverUtils receiverUtils;
    ProcessState processState;
    int dataStartIndex;

    public DataProcessorThread(
            Configuration configuration,
            ArrayBlockingQueue arrayBlockingQueue,
            Receiver.ReceiverCallBack receiverCallBack,
            Handler handler)
    {
        this.configuration = configuration;
        this.arrayBlockingQueue = arrayBlockingQueue;
        this.receiverCallBack = receiverCallBack;
        this.handler = handler;
        receiverUtils = new ReceiverUtils(configuration);
        processState = ProcessState.initialCarrierSync;
        dataStartIndex = -1;
    }

    // p(i) = sum(data(i:(i+bit_delay-1)))-sum(data((i+bit_delay):(i-1+2*bit_delay)));

    @Override
    public void run() {
        Double[] prefix = new Double[configuration.getSamplesPerCodeBit()];
        Integer[] processedDataPrefix = new Integer[4 * configuration.getSamplesPerCodeBit()];
        Arrays.fill(prefix,-1);
        Arrays.fill(processedDataPrefix,-1);
        while(!this.isInterrupted()){
            try {
                short[] buff = (short[]) arrayBlockingQueue.take();

                // TODO Process data here
                Integer[] processedData = receiverUtils.removeSine(buff, prefix);
                for(int i =0; i<prefix.length; i++){
                    prefix[i] = (double) buff[i];
                }

                processedData = combineArrays(processedDataPrefix, processedData);
                if(processState==ProcessState.initialCarrierSync){
                    for(int i=0; i<processedData.length - 4 * configuration.getSamplesPerCodeBit(); i++){
                        if(checkIfPreamble(i,processedData))break;
                    }
                }
                for(int i =0; i<processedDataPrefix.length; i++){
                    processedDataPrefix[i] = processedData[processedData.length - processedDataPrefix.length + i];
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                this.interrupt();
            }
        }
    }

    public void abort(){
        this.interrupt();
    }

    private boolean checkIfPreamble(int startIndex, Integer[] processedData){
        int power1, power2;
        power1 = getSubArraySum(startIndex,startIndex+configuration.getSamplesPerCodeBit()-1, processedData)
                - getSubArraySum(startIndex+configuration.getSamplesPerCodeBit(), startIndex+ 2*configuration.getSamplesPerCodeBit()-1, processedData);
        if (power1>=200){
            power2 = getSubArraySum(startIndex + 2*configuration.getSamplesPerCodeBit(),startIndex+3*configuration.getSamplesPerCodeBit()-1, processedData)
                    - getSubArraySum(startIndex+3*configuration.getSamplesPerCodeBit(), startIndex+ 4*configuration.getSamplesPerCodeBit()-1, processedData);
            if(Math.abs(power1-power2) <20){
                dataStartIndex = startIndex;
                return true;
            }
        }
        return false;
    }

    private int getSubArraySum(int startIndex, int endIndex, Integer[] array){
        int sum = 0;
        for(int i = startIndex; i<=endIndex; i++){
            sum+=array[i];
        }
        return sum;
    }

    private Integer[] combineArrays(Integer[] prefix, Integer[] suffix){
        ArrayList<Integer> combinedArrayList = new ArrayList(Arrays.asList(prefix));
        combinedArrayList.addAll(Arrays.asList(suffix));
        Integer[] combinedArray = combinedArrayList.toArray(new Integer[0]);
        return combinedArray;
    }
}
