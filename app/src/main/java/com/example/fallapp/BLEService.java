package com.example.fallapp;


import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;



import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.os.Looper;

import com.google.android.gms.tasks.Task;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentralManager;
import com.welie.blessed.BluetoothCentralManagerCallback;
import com.welie.blessed.BluetoothPeripheral;
import com.welie.blessed.BluetoothPeripheralCallback;
import com.welie.blessed.ConnectionPriority;
import com.welie.blessed.GattStatus;
import com.welie.blessed.HciStatus;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;


import android.util.Log;

import android.telephony.SmsManager;


import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;


public class BLEService extends Service {

    String phoneNumber;

    private static final UUID fallServiceUUID = UUID.fromString("00005321-0000-1000-8000-00805f9b34fb");
    private static final UUID fallCharacteristicUUID = UUID.fromString("00000001-0000-1000-8000-00805f9b34fb");

    public BluetoothCentralManager central;

    //private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    //private int currentTimeCounter = 0;

    private FusedLocationProviderClient fusedLocationClient;


    private final BluetoothPeripheralCallback peripheralCallback = new BluetoothPeripheralCallback() {
        @Override
        public void onServicesDiscovered(@NotNull BluetoothPeripheral peripheral) {

            BluetoothGattCharacteristic fallCharacteristic = peripheral.getCharacteristic(fallServiceUUID, fallCharacteristicUUID);

            // Request a new connection priority
            peripheral.requestConnectionPriority(ConnectionPriority.HIGH);

            peripheral.setNotify(fallCharacteristic, true);

        }

        @Override
        public void onCharacteristicUpdate(@NotNull BluetoothPeripheral peripheral, @NotNull byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {

            if (status != GattStatus.SUCCESS) return;

            UUID characteristicUUID = characteristic.getUuid();
            BluetoothBytesParser parser = new BluetoothBytesParser(value);

            if (characteristicUUID.equals(fallCharacteristicUUID)) {

                parser = new BluetoothBytesParser(value);

                // Parse the flags
                int flags = parser.getIntValue(0x11);
                if (flags == 1) {
                    sendMessage();
                }
            }
        }

    };
    private final BluetoothCentralManagerCallback bluetoothCentralManagerCallback = new BluetoothCentralManagerCallback() {


        @Override
        public void onDisconnectedPeripheral(@NotNull final BluetoothPeripheral peripheral, final @NotNull HciStatus status) {


            // Reconnect to this device when it becomes available again
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    central.autoConnectPeripheral(peripheral, peripheralCallback);
                }
            }, 10000);

        }

        @Override
        public void onDiscoveredPeripheral(@NotNull BluetoothPeripheral peripheral, @NotNull ScanResult scanResult) {

            central.stopScan();
            central.connectPeripheral(peripheral, peripheralCallback);
        }
    };


    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        SharedPreferences sh = getSharedPreferences("FallPref", MODE_PRIVATE);
        SharedPreferences.Editor myEdit = sh.edit();

        myEdit.putBoolean("fallDetectionState", false);
        myEdit.apply();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        SharedPreferences sh = getSharedPreferences("FallPref", MODE_PRIVATE);

        phoneNumber = sh.getString("phoneNumber", "");

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this, "BLE Service")
                .setContentTitle("BLE Fall Detection")
                .setContentText("In case of fall will alert " + phoneNumber)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        central = new BluetoothCentralManager(getApplicationContext(), bluetoothCentralManagerCallback, handler);

        startScan();

        //return START_NOT_STICKY;
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("BLE Service", "Foreground notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);

        }
    }


    private void startScan() {

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                central.scanForPeripheralsWithServices(new UUID[]{fallServiceUUID});
            }
        }, 2000);
    }


    public void sendMessage() {

        //smgr.sendTextMessage(phoneNumber, null, "Help! I've' fallen at" + String.valueOf(location.getLongitude() + " ," + String.valueOf(location.getLatitude()) + ), null, null);
        //smgr.sendTextMessage(phoneNumber, null, "", null, null);
        Task<Location> locationTask = fusedLocationClient.getLastLocation();

        SmsManager smgr = SmsManager.getDefault();

        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                double latitude, longitude;

                latitude = location.getLatitude(); //40.712728; //
                longitude = location.getLongitude(); //-74.006015;


                String txtMessage = "Help! I've' fallen at " + String.valueOf(latitude) + ", " + String.valueOf(longitude);
                String googleMapsAddress = "https://www.google.com/maps/search/?api=1&query=" + String.valueOf(latitude) + "%2C" + String.valueOf(longitude);


                smgr.sendTextMessage(phoneNumber, null, txtMessage, null, null);
                smgr.sendTextMessage(phoneNumber, null, googleMapsAddress, null, null);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }






}