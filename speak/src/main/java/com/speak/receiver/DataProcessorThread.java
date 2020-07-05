package com.speak.receiver;

import android.os.Handler;
import android.util.Log;

import com.speak.utils.Configuration;

import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;

/*
  1. Filter define
  2. RSSI
  3. SineRemoval
  4. Low Pass filter & amplify
* */


public class DataProcessorThread extends Thread{
    Configuration configuration;
    ArrayBlockingQueue arrayBlockingQueue;
    Receiver.ReceiverCallBack receiverCallBack;
    Handler handler;

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
    }

    @Override
    public void run() {
        while(!this.isInterrupted()){
            try {
                short[] buff = (short[]) arrayBlockingQueue.take();
                // TODO Process data here
            } catch (InterruptedException e) {
                e.printStackTrace();
                this.interrupt();
            }
        }
    }

    public void abort(){
        this.interrupt();
    }
}
