package com.example.fallapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends AppCompatActivity {
    EditText phoneNumberInput;
    ToggleButton startButton;

    String phoneNumber;
    boolean serviceOn;

    SharedPreferences sharedpreferences;

    String[] perms = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_BACKGROUND_LOCATION","android.permission.ACCESS_FOREGROUND_LOCATION","android.permission.SEND_SMS", "android.permission.BLUETOOTH_ADMIN", "android.permission.BLUETOOTH"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        phoneNumberInput = (EditText) findViewById(R.id.phoneNumberInput);
        startButton = (ToggleButton) findViewById(R.id.startButton);

        startButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                    SharedPreferences sh = getSharedPreferences("FallPref", MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sh.edit();
                    myEdit.putString("phoneNumber", phoneNumberInput.getText().toString());
                    myEdit.putBoolean("fallDetectionState", true);
                    myEdit.apply();
                    //start the service

                    ActivityCompat.requestPermissions(MainActivity.this,perms,4);


                    startBLEService();

                } else {
                    // The toggle is disabled

                    SharedPreferences sh = getSharedPreferences("FallPref", MODE_PRIVATE);
                    SharedPreferences.Editor myEdit = sh.edit();
                    myEdit.putBoolean("fallDetectionState", false);
                    myEdit.apply();

                    //stop service
                    stopBLEService();
                }
            }


        });
        SharedPreferences sh = getSharedPreferences("FallPref",MODE_PRIVATE);

        serviceOn = sh.getBoolean("fallDetectionState",false);
        phoneNumber = sh.getString("phoneNumber", "");

        phoneNumberInput.setText(phoneNumber);

        startButton.setChecked(serviceOn);
    }

    @Override
    protected void onResume(){
        super.onResume();
        SharedPreferences sh = getSharedPreferences("FallPref",MODE_PRIVATE);

        serviceOn = sh.getBoolean("fallDetectionState",false);
        phoneNumber = sh.getString("phoneNumber", "");

        phoneNumberInput.setText(phoneNumber);

    }

    @Override
    protected void onPause(){
        super.onPause();

        SharedPreferences sh = getSharedPreferences("FallPref", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sh.edit();

        // write all the data entered by the user in SharedPreference and apply
        myEdit.putString("phoneNumber", phoneNumberInput.getText().toString());

        myEdit.apply();


    }

    @Override
    protected void onDestroy() {
        super.onPause();
        super.onDestroy();
    }

    public void startBLEService(){
        Intent intent = new Intent(this, BLEService.class);
        startForegroundService(intent);
    }

    public void stopBLEService(){
        Intent intent = new Intent(this, BLEService.class);
        stopService(intent);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantsResults){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED  && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {




            startButton.setChecked(false);

            SharedPreferences sh = getSharedPreferences("FallPref", MODE_PRIVATE);
            SharedPreferences.Editor myEdit = sh.edit();
            myEdit.putBoolean("fallDetectionState", false);
            myEdit.apply();


            Context context = getApplicationContext();
            CharSequence text = "Check Permissions!";
            int duration = Toast.LENGTH_LONG;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();


            return;
        }


    }
}