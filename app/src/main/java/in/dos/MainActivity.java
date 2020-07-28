package in.dos;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.speak.Speak;
import com.speak.receiver.Receiver;
import com.speak.sender.Sender;
import com.speak.utils.Configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.Deflater;
import java.util.zip.Inflater;


public class MainActivity extends AppCompatActivity {

    Button btn_send,btn_receive,btn_stop_send,btn_stop_receive;
    TextView tv_data;
    EditText edt_data;
    Speak speak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        initView();
    }

    void init(){
        speak = new Speak(new Configuration());
    }

    void initView(){
        edt_data = findViewById(R.id.edt_query);
        tv_data = findViewById(R.id.tv_data);
        btn_send = findViewById(R.id.btn_send);
        btn_stop_send = findViewById(R.id.btn_stop_send);
        btn_receive = findViewById(R.id.btn_receive);
        btn_stop_receive = findViewById(R.id.btn_stop_receive);

        setButtonListeners();
    }

    void setButtonListeners(){

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak.startSending(edt_data.getText().toString(), new Sender.SenderCallBack() {
                    @Override
                    public void onSendComplete() {
                        Toast.makeText(MainActivity.this, "Send Completed", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btn_receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receive();
            }
        });

        btn_stop_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak.stopSending();
            }
        });

        btn_stop_receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               speak.stopListening();
            }
        });

    }

    public void receive() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }else{
            speak.startListening(new Receiver.ReceiverCallBack() {
                @Override
                public void onDataReceived(String message) {
                    tv_data.setText(message);
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                //If user granted permission on mic, continue with listening
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    receive();
                }
                break;
            }
        }
    }
}