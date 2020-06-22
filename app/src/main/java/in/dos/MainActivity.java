package in.dos;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import in.dos.utils.BitManipulationHelper;

public class MainActivity extends AppCompatActivity {

    BitManipulationHelper bitManipulationHelper = new BitManipulationHelper();

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Log.d("tag",bitManipulationHelper.getBits("Harsha").toString());
//        Log.d("tag",bitManipulationHelper.generatePseudoRandomSequence("1111").toString());
        Log.d("tag",bitManipulationHelper.interpolateDataBits(bitManipulationHelper.getBits("Harsha")).toString());
        Log.d("tag",bitManipulationHelper.interpolateCodeBits(bitManipulationHelper.generatePseudoRandomSequence("1111")).toString());
        Log.d("tag","testing");
    }
}