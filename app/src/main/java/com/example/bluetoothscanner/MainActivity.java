package com.example.bluetoothscanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_ACCESS_FINE_LOCATION = 1;
    public static final int REQUEST_ACCESS_BACKGROUND_LOCATION = 1;
    public static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private String deviceName="MI Portable Bluetooth Speaker";
    private ImageView speakerImageView;
    private Button scanningBtn;
    private BluetoothAdapter bluetoothAdapter;
    ProgressDialog pd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        scanningBtn = findViewById(R.id.scanningBtn);
        speakerImageView = findViewById(R.id.speakerImage);
        checkBluetoothState();
        pd = new ProgressDialog(this);
        pd.setMessage("Wait please");

        scanningBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                    BluetoothDevice deviceToFind = isAlreadyPaired();
                    if(deviceToFind!=null){
                        System.out.println("Device already paired");
                        deviceToFind.createBond();
                    }else{
                        System.out.println("Device not paired");
                        if (checkLocationPermission()) {
                            pd.show();
                            bluetoothAdapter.startDiscovery();
                        }
                    }

                } else {
                    checkBluetoothState();
                }
            }
        });

        checkLocationPermission();
    }



    private BluetoothDevice isAlreadyPaired() {

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        BluetoothDevice devicePaired = null;
        List<String> s = new ArrayList<String>();
        for(BluetoothDevice bt : pairedDevices){
            s.add(bt.getName());
            devicePaired = bt;
        }
        if(s.contains(deviceName)){

            return devicePaired;
        }
        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(devicesFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(devicesFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
        registerReceiver(devicesFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST));
        registerReceiver(devicesFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(devicesFoundReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(devicesFoundReceiver);
    }

    public static boolean isConnected(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("isConnected", (Class[]) null);
            boolean connected = (boolean) m.invoke(device, (Object[]) null);
            return connected;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean checkLocationPermission() {
        if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ACCESS_FINE_LOCATION
            );
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                            REQUEST_ACCESS_BACKGROUND_LOCATION);
                }
            }
            return false;
        } else {
            return true;
        }
    }

    private void checkBluetoothState() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on your device!", Toast.LENGTH_SHORT).show();
        } else {
            if (bluetoothAdapter.isEnabled()) {
                if (bluetoothAdapter.isDiscovering()) {
                    Toast.makeText(this, "Device discovering process...", Toast.LENGTH_SHORT).show();
                } else {
                   // Toast.makeText(this, "Bluetooth is enabled", Toast.LENGTH_SHORT).show();
                    scanningBtn.setEnabled(true);
                }
            } else {
                Toast.makeText(this, "You need enable Bluetooth", Toast.LENGTH_SHORT).show();
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            checkBluetoothState();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Access fine location allowed. You can scan Bluetooth devices", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Access fine location forbidden. You can scan Bluetooth devices", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_ACCESS_BACKGROUND_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Access background location allowed. You can scan Bluetooth devices", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this,
                        "Access background location forbidden. You can scan Bluetooth devices", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private final BroadcastReceiver devicesFoundReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getName()!=null && device.getName().equalsIgnoreCase((deviceName))){
                    bluetoothAdapter.cancelDiscovery();
                    try {

                      boolean bondCreated =  createBond(device);
                        pd.dismiss();
                      System.out.println("Bond created -> " + bondCreated);
                      if (bondCreated){
                          speakerImageView.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.spkeaerwaiting));
                      }
                    } catch (Exception e) {
                        speakerImageView.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.spkeaeroff));

                        e.printStackTrace();
                    }
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                scanningBtn.setText("Scan Bluetooth Devices");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                scanningBtn.setText("Scanning in progress...");
            }
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                System.out.println("Bond state changed " + action);
                if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR) == BluetoothDevice.BOND_BONDED) {
                    System.out.println("Device Bonded successfully");
                    speakerImageView.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.speaker));
                }else{
                    System.out.println("Device Bonded failed");
                    speakerImageView.setImageDrawable(MainActivity.this.getResources().getDrawable(R.drawable.spkeaerwaiting));
                }

            }
            else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)){
                System.out.println("Action pairing request " + action);
            }
        }
    };

    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }
}
