package in.dos;


import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.speak.Speak;
import com.speak.sender.Sender;
import com.speak.utils.Configuration;


public class MainActivity extends AppCompatActivity {

    Button btn_abort;
    Speak speak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_abort = findViewById(R.id.btn_abort);

        speak = new Speak(new Configuration());
        speak.send("testdsf", new Sender.SenderCallBack() {
            @Override
            public void onSendComplete() {
                Log.d("TAG", "SEND COMPLETED");
            }
        });

        btn_abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speak.stopSending();
            }
        });

    }
}