package com.speak;

import com.speak.receiver.Receiver;
import com.speak.sender.Sender;
import com.speak.utils.Configuration;

import org.json.JSONArray;

public class Speak {

    Configuration configuration;
    Sender sender;
    Receiver receiver;
    public static short[] data=new short[0];

    public Speak(Configuration configuration) {
        this.configuration = configuration;
        sender = new Sender();
        receiver = new Receiver();
    }

    public void startSending(String payLoad, Sender.SenderCallBack senderCallBack) {
        sender.sendData(payLoad, configuration, senderCallBack);
    }

    public void startSending(String payLoad) {
        sender.sendData(payLoad, configuration);
    }

    public void stopSending() {
        sender.stopExistingThread();
    }

    public void startListening(Receiver.ReceiverCallBack receiverCallBack){
        receiver.startRecordingAndProcessing(configuration, receiverCallBack);
    }

    public void stopListening(){
        receiver.stopExistingThreads();
    }

}
