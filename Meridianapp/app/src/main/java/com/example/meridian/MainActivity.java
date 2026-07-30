package com.example.meridian;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.UUID;

// Add location permission prompt at start

public class MainActivity extends AppCompatActivity {

    // Declare buttons
    Button bedroom1, bedroom2, bedroom3, bedroom4;

    Button hall, kitchen, allon, alloff;

    ImageView backgroundImage;


    private ImageView blConnect;

    private static final String TAG = "BLE_Main";

    // Nordic UART Service UUIDs (yours)
    private static final UUID SERVICE_UUID =
            UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca7e");
    private static final UUID CHARACTERISTIC_UUID =
            UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca5e");
    private BluetoothGattCharacteristic toggleCharacteristic;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic txCharacteristic;

    private BluetoothDevice lastDevice;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final long RECONNECT_DELAY_MS = 2000; // 2 seconds

    // Permission launcher
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        Boolean granted = true;
                        for (Boolean v : result.values()) {
                            if (!v) granted = false;
                        }
                        if (granted) {
                            startScan();
                        } else {
                            Toast.makeText(this, "Bluetooth permissions required", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Force landscape mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // Enable edge-to-edge layout (optional for smooth edges)
        EdgeToEdge.enable(this);

        // Set fullscreen immersive mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            // For Android 10 and below
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        // Load layout
        setContentView(R.layout.activity_main);

        // Remove extra padding applied by EdgeToEdge template
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            // Return insets without adding padding, so content fills full screen
            return WindowInsetsCompat.CONSUMED;
        });

        // Initialize the ImageView
        backgroundImage = findViewById(R.id.imageView4);

        // Initialize all buttons
        bedroom1 = findViewById(R.id.bedroom1);
        bedroom2 = findViewById(R.id.bedroom2);
        bedroom3 = findViewById(R.id.bedroom3);
        bedroom4 = findViewById(R.id.bedroom4);
        hall = findViewById(R.id.hall);
        kitchen = findViewById(R.id.kitchen);
        allon = findViewById(R.id.allon);
        alloff = findViewById(R.id.alloff);


        blConnect = findViewById(R.id.blConnect);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No Bluetooth adapter");
            return;
        }

        checkPermissionsAndStart();

        setupToggleButton(bedroom1, "b1");
        setupToggleButton(bedroom2, "b2");
        setupToggleButton(bedroom3, "b3");
        setupToggleButton(bedroom4, "b4");
        setupToggleButton(hall, "h1");
        setupToggleButton(kitchen, "k1");

        allon.setOnClickListener(new View.OnClickListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onClick(View view) {

//                Toast toast = Toast.makeText(
//                        getApplicationContext(),
//                        "All ON pressed",
//                        Toast.LENGTH_SHORT
//                );
//                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
//                toast.show();


                // ✅ Set all buttons to visible (40%)
                float alphaOn = 0.0f;
                bedroom1.setAlpha(alphaOn);
                bedroom2.setAlpha(alphaOn);
                bedroom3.setAlpha(alphaOn);
                bedroom4.setAlpha(alphaOn);
                hall.setAlpha(alphaOn);
                kitchen.setAlpha(alphaOn);

                // ✅ Send BLE command
                if (toggleCharacteristic != null && bluetoothGatt != null) {
                    Log.d(TAG, "Writing toggle characteristic: 'all1'");
                    toggleCharacteristic.setValue("all1");
                    toggleCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    boolean success = bluetoothGatt.writeCharacteristic(toggleCharacteristic);
                    if (!success) {
                        Log.w(TAG, "Failed to initiate characteristic write for 'all1'");
                    }
                }
            }
        });

        alloff.setOnClickListener(new View.OnClickListener() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            @Override
            public void onClick(View view) {
//                Toast toast = Toast.makeText(
//                        getApplicationContext(),
//                        "All OFF pressed",
//                        Toast.LENGTH_SHORT
//                );
//                toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
//                toast.show();

                // ✅ Set all buttons to invisible (10%)
                float alphaOff = 0.0f;
                bedroom1.setAlpha(alphaOff);
                bedroom2.setAlpha(alphaOff);
                bedroom3.setAlpha(alphaOff);
                bedroom4.setAlpha(alphaOff);
                hall.setAlpha(alphaOff);
                kitchen.setAlpha(alphaOff);

                // ✅ Send BLE command
                if (toggleCharacteristic != null && bluetoothGatt != null) {
                    Log.d(TAG, "Writing toggle characteristic: 'all2'");
                    toggleCharacteristic.setValue("all2");
                    toggleCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    boolean success = bluetoothGatt.writeCharacteristic(toggleCharacteristic);
                    if (!success) {
                        Log.w(TAG, "Failed to initiate characteristic write for 'all2'");
                    }
                }
            }
        });


    }

    // Helper method for toggling
    private void setupToggleButton(Button button, String id) {
        final boolean[] isOn = {false};

        button.setAlpha(0.0f); // initially OFF

        button.setOnClickListener(view -> {
            isOn[0] = !isOn[0]; // toggle state
            String command = id + (isOn[0] ? "_on" : "_off");
            float alpha = isOn[0] ? 0.0f : 0.0f;

            button.setAlpha(alpha);
//
//            Toast toast = Toast.makeText(
//                    getApplicationContext(),
//                    id.toUpperCase() + (isOn[0] ? " ON" : " OFF"),
//                    Toast.LENGTH_SHORT
//            );
//            toast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100);
//            toast.show();

            if (toggleCharacteristic != null && bluetoothGatt != null) {
                Log.d(TAG, "Writing toggle characteristic: '" + command + "'");
                toggleCharacteristic.setValue(command);
                toggleCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                boolean success = bluetoothGatt.writeCharacteristic(toggleCharacteristic);
                if (!success) {
                    Log.w(TAG, "Failed to initiate characteristic write for toggle " + command);
                }
            }
        });
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN,
                });
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION
                });
                return;
            }
        }

        startScan();
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private void startScan() {
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            Log.e(TAG, "BLE scanner not available");
            return;
        }

        Log.i(TAG, "Starting BLE scan...");
        bleScanner.startScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @RequiresPermission(allOf = {Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT})
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name != null && name.contains("ESP32_Meridian")) { // change to your device name
                Log.i(TAG, "Target device found: " + name + " " + device.getAddress());
                bleScanner.stopScan(this);
                connectToDevice(device);
            }
        }
    };

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private void connectToDevice(BluetoothDevice device) {
        lastDevice = device;

        if (bluetoothGatt != null) {
            Log.w(TAG, "Closing old GATT before reconnecting");
            bluetoothGatt.close();
            bluetoothGatt = null;
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//            requestPermissionsLauncher.launch(new String[]{ Manifest.permission.BLUETOOTH_CONNECT });
//            return;
//        }

        Log.i(TAG, "Connecting to GATT...");
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }




    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            runOnUiThread(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Connected to GATT, discovering services...");
                    blConnect.setColorFilter(Color.GREEN);
                    gatt.discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Disconnected from GATT");
                    blConnect.setColorFilter(Color.RED);

                    // Attempt reconnect after delay
                    if (lastDevice != null) {
                        Log.i(TAG, "Attempting reconnect in " + RECONNECT_DELAY_MS + " ms");
                        handler.postDelayed(() -> connectToDevice(lastDevice), RECONNECT_DELAY_MS);
                    }
                }
            });
        }



        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            toggleCharacteristic = gatt.getService(SERVICE_UUID).getCharacteristic(CHARACTERISTIC_UUID);
            if (toggleCharacteristic != null) {
                Log.i(TAG, "Reading toggle characteristic once...");
                gatt.readCharacteristic(toggleCharacteristic);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            Log.i(TAG, "on charecterstic readin");
//            if (CHARACTERISTIC_UUID.equals(characteristic.getUuid()) && status == BluetoothGatt.GATT_SUCCESS) {
//                byte[] value = characteristic.getValue();
//                Log.i(TAG, "on charecterstic "+value);
//                if (value != null && value.length >= 4) {
//                    int currentVolume = ((value[3] & 0xFF) << 24) |
//                            ((value[2] & 0xFF) << 16) |
//                            ((value[1] & 0xFF) << 8)  |
//                            (value[0] & 0xFF);
//
//                    if (currentVolume < 0) currentVolume = 0;
//                    if (currentVolume > 100) currentVolume = 100;
//
//                    int finalVolume = currentVolume;
//                    runOnUiThread(() -> volumeSeekBar.setProgress(finalVolume));
//                }
//            }
        }
    };

}