package com.example.amcbtcontrol;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    ImageView joystick;
    ImageView joystickArea;

    ListView btDeviceList;
    float dX, dY, j_str;
    int j_ang, r_dir;

    boolean btDvcListOpen = false;
    boolean connected = false;
    final int RQ_BT_PM = 1;

    BluetoothAdapter BTA;
    BluetoothSocket BTS;
    OutputStream BTOS;

    //Timer BTLoop;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_CONNECT}, RQ_BT_PM);

        joystick = findViewById(R.id.joystick);
        joystick.setOnTouchListener(this);

        joystickArea = findViewById(R.id.joystickArea);

        btDeviceList = findViewById(R.id.btDeviceList);

        findViewById(R.id.btnRotateCW).setOnTouchListener(this);
        findViewById(R.id.btnRotateCCW).setOnTouchListener(this);

        BTA = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_main, menu);
        return true;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (view.equals(joystick)) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = view.getX() - motionEvent.getRawX();
                    dY = view.getY() - motionEvent.getRawY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    float areaRadius = joystickArea.getWidth() / 2;
                    float radius = joystick.getWidth() / 2;
                    float maxDistance = areaRadius - radius;

                    float newX = motionEvent.getRawX() + dX;
                    float newY = motionEvent.getRawY() + dY;
                    float distanceX = newX - joystickArea.getX() - areaRadius + radius;
                    float distanceY = newY - joystickArea.getY() - areaRadius + radius;
                    float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);

                    float angle = (float) Math.atan2(distanceY, distanceX);
                    float strength = (float) Math.min(distance, maxDistance) / maxDistance;
                    joystickMove((int) Math.round(angle * (180 / Math.PI)), strength);

                    if (distance > maxDistance) {
                        newX = joystickArea.getX() + areaRadius - radius + (float) (Math.cos(angle) * maxDistance);
                        newY = joystickArea.getY() + areaRadius - radius + (float) (Math.sin(angle) * maxDistance);
                    }

                    view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start();
                    break;
                case MotionEvent.ACTION_UP:
                    view.animate()
                            .x(joystickArea.getX() + joystickArea.getWidth() / 2 - view.getWidth() / 2)
                            .y(joystickArea.getY() + joystickArea.getHeight() / 2 - view.getHeight() / 2)
                            .setDuration(100)
                            .start();
                    joystickMove(0, 0);
                    break;
            }
            return true;
        }
        return btnAction(view, motionEvent.getAction());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnSettings:
                if (connected) {
                    Toast.makeText(getApplicationContext(), "Please disconnect", Toast.LENGTH_SHORT).show();
                    break;
                }
                Intent myIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(myIntent);
                break;
            case R.id.btnBluetooth:
                if (connected) {
                    try {
                        BTS.close();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                    }
                } else if (btDvcListOpen) {
                    btDeviceList.setVisibility(View.INVISIBLE);
                    btDvcListOpen = false;
                } else listBluetoothDevices();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void joystickMove(int angle, float strength) {
        j_ang = angle;
        j_str = strength;

        TextView j_ang_txt = findViewById(R.id.coordsText);
        j_ang_txt.setText(String.valueOf(j_ang));
    }

    private boolean btnAction(View view, int action) {
        if (action == MotionEvent.ACTION_DOWN) switch (view.getId()) {
            case R.id.btnRotateCW:
                r_dir = 1;
                break;
            case R.id.btnRotateCCW:
                r_dir = -1;
                break;
            default:
                return false;
        }
        else if (action == MotionEvent.ACTION_UP) r_dir = 0;

        return true;
    }

    private boolean numInRange(float start, float dist, float num) {
        return num >= start && num <= start + dist;
    }

    private void listBluetoothDevices() {
        Log.d("Click bt", "");
        if (BTA == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not available", Toast.LENGTH_SHORT).show();
            return;
        }
        if (BTA.isEnabled()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
                btDvcListOpen = true;
                btDeviceList.setVisibility(View.VISIBLE);
                Set<BluetoothDevice> pairedDevices = BTA.getBondedDevices();
                ArrayList<BluetoothDevice> pairedDevicesList = new ArrayList<>();
                ArrayList<String> list = new ArrayList<>();
                for (BluetoothDevice btd : pairedDevices) {
                    pairedDevicesList.add(btd);
                    String name = btd.getName();
                    list.add(name == "" ? btd.getAddress() : name);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, list);
                btDeviceList.setAdapter(adapter);

                btDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (
                                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                                ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                        ) {
                            Log.d("Position:", String.valueOf(position));
                            BluetoothDevice device = pairedDevicesList.get(position);
                            UUID deviceUUID = device.getUuids()[0].getUuid();
                            try {
                                BTS = device.createRfcommSocketToServiceRecord(deviceUUID);
                                Log.d("UUID:", deviceUUID.toString());
                                BTS.connect();
                                BTOS = BTS.getOutputStream();
                                runCast();

                                connected = true;
                            } catch (IOException e) {
                                Log.d("Conn?", connected ? "1" : "0");
                                e.printStackTrace();
                                Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
            } else {
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.BLUETOOTH_CONNECT }, RQ_BT_PM);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Bluetooth is off", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        btDeviceList.setVisibility(View.INVISIBLE);
        btDvcListOpen = false;
    }

    private void runCast() {
        if (connected) return;

        //BTLoop.cancel();

        HashMap<String, String> settings = new HashMap<>();
        settings.put("cmd_fw", "W");
        settings.put("cmd_bw", "X");
        settings.put("cmd_lt", "A");
        settings.put("cmd_rt", "D");
        settings.put("cmd_fwlt", "Q");
        settings.put("cmd_fwrt", "E");
        settings.put("cmd_bwlt", "Z");
        settings.put("cmd_bwrt", "C");
        settings.put("cmd_rtcw", "L");
        settings.put("cmd_rtccw", "J");
        settings.put("repeat", "50");
        settings.put("diag_area", "25");

        HashMap<String, String> storedSettings = Utils.fetchSettings(getApplicationContext());
        for (String key : storedSettings.keySet()) {
            settings.put(key, storedSettings.get(key));
        }

        Timer timer = new Timer();
        TimerTask checker = new TimerTask() {
            @Override
            public void run() {
                if (connected) {
                    /*if (r_dir == -1) Log.d("CMD", settings.get("cmd_rtccw"));
                    else if (r_dir == 1) Log.d("CMD", settings.get("cmd_rtcw"));
                    else if (j_str > 0.1) {
                        int dg_ar_ang = Integer.parseInt(settings.get("diag_area"));
                        if (numInRange(-135 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                            Log.d("CMD", settings.get("cmd_fwlt"));
                        else if (numInRange(-45 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                            Log.d("CMD", settings.get("cmd_fwrt"));
                        else if (numInRange(45 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                            Log.d("CMD", settings.get("cmd_bwrt"));
                        else if (numInRange(135 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                            Log.d("CMD", settings.get("cmd_bwlt"));
                        else if (numInRange(-180, 45, j_ang) || numInRange(135, 45, j_ang))
                            Log.d("CMD", settings.get("cmd_lt"));
                        else if (numInRange(-135, 90, j_ang)) Log.d("CMD", settings.get("cmd_fw"));
                        else if (numInRange(-45, 90, j_ang)) Log.d("CMD", settings.get("cmd_rt"));
                        else if (numInRange(45, 90, j_ang)) Log.d("CMD", settings.get("cmd_bw"));
                    }*/
                    try {
                        if (r_dir == -1) BTOS.write(settings.get("cmd_rtccw").getBytes());
                        else if (r_dir == 1) BTOS.write(settings.get("cmd_rtcw").getBytes());
                        else if (j_str > 0.1) {
                            int dg_ar_ang = Integer.parseInt(settings.get("diag_area"));
                            if (numInRange(-135 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                                BTOS.write(settings.get("cmd_fwlt").getBytes());
                            else if (numInRange(-45 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                                BTOS.write(settings.get("cmd_fwrt").getBytes());
                            else if (numInRange(45 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                                BTOS.write(settings.get("cmd_bwrt").getBytes());
                            else if (numInRange(135 - dg_ar_ang / 2, dg_ar_ang, j_ang))
                                BTOS.write(settings.get("cmd_bwlt").getBytes());
                            else if (numInRange(-180, 45, j_ang) || numInRange(135, 45, j_ang))
                                BTOS.write(settings.get("cmd_lt").getBytes());
                            else if (numInRange(-135, 90, j_ang)) BTOS.write(settings.get("cmd_fw").getBytes());
                            else if (numInRange(-45, 90, j_ang)) BTOS.write(settings.get("cmd_rt").getBytes());
                            else if (numInRange(45, 90, j_ang)) BTOS.write(settings.get("cmd_bw").getBytes());
                        }
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                        timer.cancel();
                        connected = false;
                    }
                }
            }
        };
        timer.schedule(checker, 0L, Integer.parseInt(settings.get("repeat")));
    }
}
