package com.speak.receiver;

import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;

import com.speak.Speak;
import com.speak.utils.Configuration;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    ArrayList<String> prbsSequence;
    ArrayList<Integer> blocks;
    String finalBinaryData;
    String finalData;
    Integer prbsSequenceIndex;
    JSONArray jsonArray;

    public void setJsonArray(JSONArray jsonArray) {
        this.jsonArray = jsonArray;
    }

    public DataProcessorThread(
            Configuration configuration,
            ArrayBlockingQueue arrayBlockingQueue,
            Receiver.ReceiverCallBack receiverCallBack,
            Handler handler,
            ArrayList<String> prbsSequence)
    {
        this.configuration = configuration;
        this.arrayBlockingQueue = arrayBlockingQueue;
        this.receiverCallBack = receiverCallBack;
        this.handler = handler;
        receiverUtils = new ReceiverUtils(configuration);
        processState = ProcessState.initialCarrierSync;
        blocks = new ArrayList<>();
        blocks.add(-1);
        dataStartIndex = -1;
        this.prbsSequence = prbsSequence;
        prbsSequenceIndex = 0;
        finalBinaryData = "";
        finalData ="";
    }

    // p(i) = sum(data(i:(i+bit_delay-1)))-sum(data((i+bit_delay):(i-1+2*bit_delay)));

    @Override
    public void run() {
        Double[] prefix = new Double[configuration.getSamplesPerCodeBit()];
        Double[] lpfPrefix = new Double[configuration.getSamplesPerCodeBit()];
        Double[] hpfPrefix = new Double[configuration.getSamplesPerCodeBit()];
        Integer[] processedDataPrefix = new Integer[5 * configuration.getSamplesPerCodeBit()];
        Integer[] blockPrefix = new Integer[0];
        Arrays.fill(prefix,0.0);
        Arrays.fill(lpfPrefix,0.0);
        Arrays.fill(hpfPrefix,0.0);
        Arrays.fill(processedDataPrefix,-1);
        //while(!this.isInterrupted())
        {
            try {
                Log.d("data","Waiting for data");
                Log.d("data","State: "+processState.toString());
                //short[] buff = (short[]) arrayBlockingQueue.take();
                short[] buff = new short[0];
                buff = Speak.data;
                Log.d("data","Got data "+buff[0]);
                // TODO Process data here
                Integer[] processedData = receiverUtils.removeSine(buff, prefix,hpfPrefix,lpfPrefix);


                if(processState==ProcessState.initialCarrierSync){
                    processedData = combineArrays(processedDataPrefix, processedData);
                    for(int i=0; i<processedData.length - 5 * configuration.getSamplesPerCodeBit(); i++){
                        if(checkIfPreamble(i,processedData)) {
                            HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
                                                                                                (dataStartIndex+configuration.getSamplesPerCodeBit()*4),
                                                                                                blockPrefix);
                            blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
                            if(checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY))){
                                if(retrieveData(new Integer[]{})){
                                    abort();
                                }
                                Log.d("data",finalBinaryData);
                                Log.d("data",finalData);
                            }
                            break;
                        }
                    }
                    for(int i =0; i<processedDataPrefix.length; i++){
                        processedDataPrefix[i] = processedData[processedData.length - processedDataPrefix.length + i];
                    }
                }
                else if(processState == ProcessState.CodeSync){
                    HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
                                                  0,
                                                            blockPrefix);

                    blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
                    if(checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY))){
                        if(retrieveData(new Integer[]{})){
                            abort();
                        }
                    }
                }else if(processState == ProcessState.DataRecovery){
                    HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
                            (dataStartIndex+configuration.getSamplesPerCodeBit()*4)%processedData.length,
                            blockPrefix);

                    blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
                    if(retrieveData(data.get(ReceiverUtils.BLOCKS_KEY))){
                            abort();
                    }

                }

            } catch (Exception e) {
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
            int maxIndex= startIndex,maxPower=power1;
            for(int i=1;i<=configuration.getSamplesPerCodeBit();i++){
                power1 = getSubArraySum(startIndex+i,startIndex+i+configuration.getSamplesPerCodeBit()-1, processedData)
                        - getSubArraySum(startIndex+i+configuration.getSamplesPerCodeBit(), startIndex+i+ 2*configuration.getSamplesPerCodeBit()-1, processedData);
                if(power1>maxPower){
                    maxPower=power1;
                    maxIndex = i;
                }
            }
            startIndex = maxIndex;
            power2 = getSubArraySum(startIndex + 2*configuration.getSamplesPerCodeBit(),startIndex+3*configuration.getSamplesPerCodeBit()-1, processedData)
                    - getSubArraySum(startIndex+3*configuration.getSamplesPerCodeBit(), startIndex+ 4*configuration.getSamplesPerCodeBit()-1, processedData);
            if(Math.abs(maxPower-power2) <20){
                dataStartIndex = startIndex;
                processState = ProcessState.CodeSync;
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

    private boolean checkPRBS(Integer[] data){
        int index = blocks.size();
        blocks.addAll(Arrays.asList(data));

        for(int i=index;i<blocks.size();i++){
            blocks.set(i,blocks.get(i-1)*blocks.get(i));
        }

        while(blocks.size()>=prbsSequence.size()){
            int sum=0;
            for(int i=0;i<prbsSequence.size();i++){
                sum+= (2*Integer.parseInt(prbsSequence.get(i))-1)*blocks.get(i);
            }
            Log.d("SUM",sum+" ");
            if(sum>0.7*prbsSequence.size()){
                for(int j=0;j<prbsSequence.size();j++){
                    blocks.remove(0);
                }
                processState = ProcessState.DataRecovery;
                return true;
            }
            blocks.remove(0);
        }

        return false;
    }


    public boolean retrieveData(Integer[] data){

        int index = blocks.size();
        blocks.addAll(Arrays.asList(data));

        while(blocks.size()>=configuration.getSpreadingFactor()){
            int sum=0;
            for(int j=0;j<configuration.getSpreadingFactor();j++){
                int val = blocks.get(0)*(Integer.parseInt(prbsSequence.get(prbsSequenceIndex)));
                prbsSequenceIndex = (prbsSequenceIndex+1)%prbsSequence.size();
                blocks.remove(0);
                sum+=val;
            }

            finalBinaryData +=(sum/60.0>0)?"1":"0";
            if(checkFinalData()) return true;
        }

        return false;
    }

    public boolean checkFinalData(){
        if(finalBinaryData.length()==8){
            int charCode = Integer.parseInt(finalBinaryData, 2);
            String str = new Character((char)charCode).toString();
            finalData +=str;
            if(str=="$") return true;
            Log.d("data",str);
            finalBinaryData = "";
        }
        return false;
    }


}
