package com.speak.receiver;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.speak.utils.Configuration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;


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
    String receivedBinaryData;
    String receivedData;
    Integer prbsSequenceIndex;
    Integer peakPolarity;
    ArrayList<Short> exptData = new ArrayList<>();

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
        processState = ProcessState.carrierSync;
        blocks = new ArrayList<>();
        blocks.add(-1);
        dataStartIndex = -1;
        this.prbsSequence = prbsSequence;
        prbsSequenceIndex = 0;
        receivedBinaryData = "";
        receivedData ="";
    }

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

        while(!this.isInterrupted()){
            try {
                Log.d("state","State: "+processState.toString());
                short[] buff = (short[]) arrayBlockingQueue.take();
                Integer[] processedData = receiverUtils.removeSine(buff, prefix,hpfPrefix,lpfPrefix);

                if(processState==ProcessState.carrierSync){
                    processedData = receiverUtils.combineArrays(processedDataPrefix, processedData);
                    for(int i=0; i<processedData.length - 5 * configuration.getSamplesPerCodeBit(); i++){
                        if(checkIfPreamble(i,processedData)) {
//                            updateExptData(buff);
                            HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
                                                                                                dataStartIndex,
                                                                                                blockPrefix);
                            blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
                            if(checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY))){
                                if(retrieveData(new Integer[]{})){
                                    sendMessageAndAbort();
                                }
                            }
                            break;
                        }
                    }
                    for(int i =0; i<processedDataPrefix.length; i++){
                        processedDataPrefix[i] = processedData[processedData.length - processedDataPrefix.length + i];
                    }
                }
                else if(processState == ProcessState.CodeSync){

//                    updateExptData(buff);
                    HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
                                                  0,
                                                            blockPrefix);
                    blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
                    if(checkPRBS(data.get(ReceiverUtils.BLOCKS_KEY))){
                        if(retrieveData(new Integer[]{})){
                            sendMessageAndAbort();
                        }
                    }
                }else if(processState == ProcessState.DataRecovery){

//                    updateExptData(buff);
                    HashMap<String,Integer[]> data = receiverUtils.reduceBlockDataToBits(processedData,
                                                        0,
                                                        blockPrefix);

                    blockPrefix = data.get(ReceiverUtils.PREFIX_KEY);
                    if(retrieveData(data.get(ReceiverUtils.BLOCKS_KEY))){
                            sendMessageAndAbort();
                    }

                }

            } catch (InterruptedException e) {
                e.printStackTrace();
                this.interrupt();
            }
        }
    }

    private void updateExptData(short[] buff){

        for(int m=0;m<buff.length;m++){
            exptData.add(buff[m]);
        }

        if(exptData.size()>9*configuration.getSamplingRate()/2){
            Log.d("tag","");
            Log.d("tag","");
        }
    }

    private void sendMessageAndAbort(){
        Message msg = new Message();
        msg.obj = receivedData;
        handler.sendMessage(msg);
        abort();
    }

    public void abort(){
        this.interrupt();
    }

    private boolean checkIfPreamble(int startIndex, Integer[] processedData){
        int power1, power2;
        power1 = receiverUtils.getSubArraySum(startIndex,startIndex+configuration.getSamplesPerCodeBit()-1, processedData)
                - receiverUtils.getSubArraySum(startIndex+configuration.getSamplesPerCodeBit(), startIndex+ 2*configuration.getSamplesPerCodeBit()-1, processedData);
        if (power1>=200){
            int maxIndex= startIndex,maxPower=power1;
            for(int i=1;i<=configuration.getSamplesPerCodeBit();i++){
                power1 = receiverUtils.getSubArraySum(startIndex+i,startIndex+i+configuration.getSamplesPerCodeBit()-1, processedData)
                        - receiverUtils.getSubArraySum(startIndex+i+configuration.getSamplesPerCodeBit(), startIndex+i+ 2*configuration.getSamplesPerCodeBit()-1, processedData);
                if(power1>maxPower){
                    maxPower=power1;
                    maxIndex = i;
                }
            }
            startIndex = maxIndex;
            power2 = receiverUtils.getSubArraySum(startIndex + 2*configuration.getSamplesPerCodeBit(),startIndex+3*configuration.getSamplesPerCodeBit()-1, processedData)
                    - receiverUtils.getSubArraySum(startIndex+3*configuration.getSamplesPerCodeBit(), startIndex+ 4*configuration.getSamplesPerCodeBit()-1, processedData);
            if(Math.abs(maxPower-power2) <20){
                dataStartIndex = startIndex;
                processState = ProcessState.CodeSync;
                return true;
            }
        }
        return false;
    }

    private boolean checkPRBS(Integer[] data){
        int index = blocks.size();
        blocks.addAll(Arrays.asList(data));

        // Set the current bits based on previous bits
        for(int i=index;i<blocks.size();i++){
            blocks.set(i,blocks.get(i-1)*blocks.get(i));
        }

        // Check for prbs code synchronisation
        while(blocks.size()>=prbsSequence.size()){
            int sum=0;
            for(int i=0;i<prbsSequence.size();i++){
                sum+= (2*Integer.parseInt(prbsSequence.get(i))-1)*blocks.get(i);
            }

            if(Math.abs(sum)>0.7*prbsSequence.size()){
                peakPolarity=(sum>0)?1:0;
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

    private boolean retrieveData(Integer[] data){

        int index = blocks.size();
        blocks.addAll(Arrays.asList(data));

        for(int i=index;i<blocks.size();i++){
            blocks.set(i,blocks.get(i-1)*blocks.get(i));
        }

        while(blocks.size()>=configuration.getSpreadingFactor()){
            int sum=0;
            for(int j=0;j<configuration.getSpreadingFactor();j++){
                int val = (int) (blocks.get(0)*(2*Integer.parseInt(prbsSequence.get(prbsSequenceIndex))-1)*Math.pow(-1,peakPolarity));
                prbsSequenceIndex = (prbsSequenceIndex+1)%prbsSequence.size();
                blocks.remove(0);
                sum+=val;
            }

            receivedBinaryData +=(sum/(double)configuration.getSpreadingFactor() > 0)?"1":"0";
            if(convertBinaryToAscii()) return true;
        }

        return false;
    }

    private boolean convertBinaryToAscii(){
        if(receivedBinaryData.length()==8){
            int charCode = Integer.parseInt(receivedBinaryData, 2);
            String str = new Character((char)charCode).toString();
            // check for terminal character
            if(str.equals(configuration.getTerminalChar())) {
                return true;
            }
            receivedData +=str;
            Log.d("bdata", receivedBinaryData);
            Log.d("data", receivedData);
            receivedBinaryData = "";
        }
        return false;
    }
}
