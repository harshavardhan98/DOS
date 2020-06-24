package com.speak.receiver;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.speak.utils.Configuration;

import java.util.concurrent.ArrayBlockingQueue;

public class Receiver {

    ArrayBlockingQueue arrayBlockingQueue;
    DataProcessorThread dataProcessorThread;
    RecorderThread recorderThread;

    public void startRecordingAndProcessing( final Configuration configuration, final ReceiverCallBack receiverCallBack){

        Handler handler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                receiverCallBack.onDataReceived( (String) msg.obj);
                return false;
            }
        });
        stopExistingThreads();
        arrayBlockingQueue = new ArrayBlockingQueue<byte[]>(15);
        recorderThread = new RecorderThread(configuration, arrayBlockingQueue);
        dataProcessorThread = new DataProcessorThread(configuration, arrayBlockingQueue, receiverCallBack, handler);
        recorderThread.start();
        dataProcessorThread.start();

    }

    public void stopExistingThreads(){
        if (recorderThread != null && !recorderThread.isInterrupted()) {
            recorderThread.abort();
        }
        if (dataProcessorThread != null && !dataProcessorThread.isInterrupted()) {
            dataProcessorThread.abort();
        }
    }

    public interface ReceiverCallBack {
        void onDataReceived(String message);
    }

}
