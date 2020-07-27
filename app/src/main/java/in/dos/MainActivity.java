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
import com.speak.Speak;
import com.speak.receiver.Receiver;
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

    Button btn_abort,btn_send,btn_receive;
    Speak speak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_abort = findViewById(R.id.btn_abort);
        btn_send = findViewById(R.id.btn_send);
        btn_receive = findViewById(R.id.btn_receive);
        speak = new Speak(new Configuration());

        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset());
            JSONArray jsonArray = obj.getJSONArray("data");
            speak.setJsonArray(jsonArray);
            Log.d("test","");
        } catch (JSONException e) {
            e.printStackTrace();
        }


        btn_abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak.stopListening();
            }
        });

        btn_receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receive();
            }
        });

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak.startSending("H");
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
                    Log.d("DATA RECEIVED", message);
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

    public void compress(String inputString) {
        try {
            // Encode a String into bytes
            Log.d("Input", inputString);
            byte[] input = inputString.getBytes("UTF-8");
            Log.d("Data", new String(input) + "  " + input.length);

            // Compress the bytes
            byte[] output = new byte[100];
            Deflater compresser = new Deflater();
            compresser.setInput(input);
            compresser.finish();
            int compressedDataLength = compresser.deflate(output);
            Log.d("TAG", "Compressed Data");
            Log.d("Data", new String(output) + "   " + compressedDataLength);
            compresser.end();

            // Decompress the bytes
            Inflater decompresser = new Inflater();
            decompresser.setInput(output, 0, compressedDataLength);
            byte[] result = new byte[100];
            int resultLength = decompresser.inflate(result);
            decompresser.end();
            Log.d("Result", new String(result) + "  " + resultLength);
            Log.d("Result", new String(result).charAt(resultLength - 1) + "");


            // Decode the bytes into a String
            String outputString = new String(result, 0, resultLength, "UTF-8");
        } catch (java.io.UnsupportedEncodingException ex) {
            // handle
        } catch (java.util.zip.DataFormatException ex) {
            // handle
        }

    }

    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream is = getApplicationContext().getAssets().open("test5.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}