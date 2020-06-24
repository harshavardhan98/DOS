package com.speak.receiver;

import android.os.Handler;
import android.util.Log;

import com.speak.utils.Configuration;

import java.util.concurrent.ArrayBlockingQueue;

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
                byte[] buff = (byte[]) arrayBlockingQueue.take();
                // TODO Process data here
                Log.d("data at 14", String.valueOf(buff[14]));
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
