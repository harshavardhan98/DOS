package com.speak;

import com.speak.sender.Sender;
import com.speak.utils.Configuration;

public class Speak {

    Configuration configuration;
    Sender sender;

    public Speak(Configuration configuration) {
        this.configuration = configuration;
        sender = new Sender();
    }

    public void send(String payLoad, Sender.SenderCallBack senderCallBack) {
        sender.sendData(payLoad, configuration, senderCallBack);
    }

    public void send(String payLoad) {
        sender.sendData(payLoad, configuration);
    }

    public void stopSending() {
        sender.stopExistingThread();
    }


    public void receive() {
    }
}
