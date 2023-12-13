package com.example.indoornavigation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class Calibration extends AppCompatActivity {

    EditText stepN, mosaicN;

    Button calibration;

    double stepLength = 0.5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration);

        mosaicN = findViewById(R.id.edtNumberMosaic);
        stepN = findViewById(R.id.edtNumberStep);
        calibration = findViewById(R.id.calibrateBtn);

        calibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    double mosaicNumber = Double.parseDouble(mosaicN.getText().toString());
                    double stepNumber =  Double.parseDouble(stepN.getText().toString());
                    stepLength = mosaicNumber*0.3/stepNumber;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(Calibration.this, InitPosition.class);
                            intent.putExtra("stepLength", stepLength);
                            startActivity(intent);
                            finish();
                            overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);
                        }
                    },600);
                }catch (Exception ex){
                    Toast.makeText(getApplicationContext(), "Empty inputs, fill and try again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
