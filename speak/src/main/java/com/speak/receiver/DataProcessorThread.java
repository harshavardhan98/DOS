package com.speak.receiver;

import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;

import com.speak.Speak;
import com.speak.utils.Configuration;

import org.json.JSONArray;
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
    Integer minSum=0,maxSum=0;
    int sign=1;
    ArrayList<Integer> blockSum = new ArrayList<>();


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
        Double[] prefix1 = new Double[configuration.getSamplesPerCodeBit()];
        Double[] lpfPrefix1 = new Double[configuration.getSamplesPerCodeBit()];
        Double[] hpfPrefix1 = new Double[configuration.getSamplesPerCodeBit()];
        Double[] prefix2 = new Double[configuration.getSamplesPerCodeBit()];
        Double[] lpfPrefix2 = new Double[configuration.getSamplesPerCodeBit()];
        Double[] hpfPrefix2 = new Double[configuration.getSamplesPerCodeBit()];
        Integer[] processedDataPrefix = new Integer[5 * configuration.getSamplesPerCodeBit()];
        Integer[] blockPrefix = new Integer[0];
        Arrays.fill(prefix1,0.0);
        Arrays.fill(lpfPrefix1,0.0);
        Arrays.fill(hpfPrefix1,0.0);
        Arrays.fill(prefix2,0.0);
        Arrays.fill(lpfPrefix2,0.0);
        Arrays.fill(hpfPrefix2,0.0);
        Arrays.fill(processedDataPrefix,-1);

//        while(!this.isInterrupted())
//        int ind1 = -1;
//        short[] testBuffer = Speak.data;
//        Integer[] processedData = receiverUtils.removeSine(testBuffer, prefix1,hpfPrefix1,lpfPrefix1);
//        processedData = combineArrays(processedDataPrefix, processedData);
//        for(int i=0; i<processedData.length - 5 * configuration.getSamplesPerCodeBit(); i++){
//            if(checkIfPreamble(i,processedData)) {
//                ind1 = dataStartIndex;
//                Log.e("","");
//                HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
//                        (dataStartIndex+configuration.getSamplesPerCodeBit()*4),
//                                blockPrefix1);
//                blockPrefix1 = data.get(ReceiverUtils.PREFIX_KEY);
//                checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY));
//                Log.e("fxf","sf");
//                break;
////                HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
////                        (dataStartIndex+configuration.getSamplesPerCodeBit()*4),
////                        blockPrefix);
////                blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
////                if(checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY))){
////                    if(retrieveData(new Integer[]{})){
////                        abort();
////                    }
////                    Log.d("data",finalBinaryData);
////                    Log.d("data",finalData);
////                }
////                break;
//            }
//        }
//
//        ArrayList<Integer> testData = new ArrayList<>();
//        testData = blocks;
//        blocks = new ArrayList<>();
//        blocks.add(-1);
//
//        int ind2 = -1;
//        Integer[] pd2 = new Integer[0];
//        for(int k=0;k<3;k++){
//            short[] testBuffer2 = new short[Speak.data.length/3];
//
//            for(int j=0;j<testBuffer2.length;j++)
//                testBuffer2[j] = testBuffer[k*testBuffer2.length+j];
//
//            Integer[] processedData2 = receiverUtils.removeSine(testBuffer2, prefix2,hpfPrefix2,lpfPrefix2);
//            if(k==0){
//                processedData2 = combineArrays(processedDataPrefix, processedData2);
//                for(int i=0; i<processedData2.length - 5 * configuration.getSamplesPerCodeBit(); i++) {
//                    if (checkIfPreamble(i, processedData2)) {
//                        ind2 = dataStartIndex;
//                        Log.e("", "");
//                        HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData2,
//                                (dataStartIndex+configuration.getSamplesPerCodeBit()*4),
//                                blockPrefix2);
//                        blockPrefix2 = data.get(ReceiverUtils.PREFIX_KEY);
//                        checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY));
//                        break;
//                    }
//                }
//            }else{
//                HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData2,
//                        0,
//                        blockPrefix2);
//                blockPrefix2 = data.get(ReceiverUtils.PREFIX_KEY);
//                checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY));
//            }
//            pd2 = combineArrays(pd2,processedData2);
////            if(k==0){
////                processedData2 = combineArrays(processedDataPrefix, processedData2);
////                for(int i=0; i<processedData2.length - 5 * configuration.getSamplesPerCodeBit(); i++){
////                    if(checkIfPreamble(i,processedData2)) {
////                        HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData2,
////                                (dataStartIndex+configuration.getSamplesPerCodeBit()*4),
////                                blockPrefix);
////                        blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
////                        checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY));
////                        break;
////                    }
////                }
////            }else{
////                HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData2,
////                        (dataStartIndex+configuration.getSamplesPerCodeBit()*4),
////                        blockPrefix);
////                blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
////                checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY));
////            }
//            for(int i =0; i<processedDataPrefix.length; i++){
//                processedDataPrefix[i] = processedData2[processedData2.length - processedDataPrefix.length + i];
//            }
//        }
//
//        for(int i=0;i<blocks.size();i++){
//            int var = testData.get(i);
//            testData.set(i,var-blocks.get(i));
//        }
//
//        Log.d("dfsd","testing");
//        Log.d("dfsd","testing");
//        Log.d("dfsd","testing");




        short[] testBuffer = Speak.data;
        {
            try {
                Log.d("data","Waiting for data");
                Log.d("data","State: "+processState.toString());
                //short[] buff = (short[]) arrayBlockingQueue.take();
                short[] buff = testBuffer;


                Log.d("data","Got data "+buff[0]);
                // TODO Process data here
                Integer[] processedData = receiverUtils.removeSine(buff, prefix1,hpfPrefix1,lpfPrefix1);


                if(processState==ProcessState.initialCarrierSync){
                    processedData = combineArrays(processedDataPrefix, processedData);
                    for(int i=0; i<processedData.length - 5 * configuration.getSamplesPerCodeBit(); i++){
                        if(checkIfPreamble(i,processedData)) {
//                            HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
//                                    (dataStartIndex+configuration.getSamplesPerCodeBit()*4),
//                                    blockPrefix);
                            HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
                                    (dataStartIndex),
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
                            0,
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
        blockSum.add(power1);
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

            if(sum<minSum){
                minSum = sum;
            }
            if(sum>maxSum){
                maxSum = sum;
            }

            if(Math.abs(sum)>0.7*prbsSequence.size()){
                if(sum>0){
                    sign=1;
                }else
                    sign=0;
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

        for(int i=index;i<blocks.size();i++){
            blocks.set(i,blocks.get(i-1)*blocks.get(i));
        }

        while(blocks.size()>=configuration.getSpreadingFactor()){
            int sum=0;
            for(int j=0;j<configuration.getSpreadingFactor();j++){
                int val = (int) (blocks.get(0)*(2*Integer.parseInt(prbsSequence.get(prbsSequenceIndex))-1)*Math.pow(-1,sign));
                prbsSequenceIndex = (prbsSequenceIndex+1)%prbsSequence.size();
                blocks.remove(0);
                sum+=val;
            }

            finalBinaryData +=(sum/((double)configuration.getSpreadingFactor())>0)?"1":"0";
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
