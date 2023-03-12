package com.example.amcbtcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {

    ImageView joystick;
    ImageView joystickArea;
    float dX, dY, j_str;
    int j_ang, r_dir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        joystick = findViewById(R.id.joystick);
        joystick.setOnTouchListener(this);

        joystickArea = findViewById(R.id.joystickArea);

        findViewById(R.id.btnRotateCW).setOnTouchListener(this);
        findViewById(R.id.btnRotateCCW).setOnTouchListener(this);

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

        /*FileIO settFile = new FileIO(getApplicationContext(), "settings.txt");
        String settStr = settFile.read();
        for (String entry : settStr.split(",")) {
            String[] pair = entry.split("=");
            if (pair[0] != "" && pair[1] != "") settings.put(pair[0], pair[1]);
        }*/
        HashMap<String, String> storedSettings = Utils.fetchSettings(getApplicationContext());
        for (String key : storedSettings.keySet()) {
            settings.put(key, storedSettings.get(key));
        }
        for (String key : settings.keySet()) {
            Log.d("Setting", key + "=" + settings.get(key) + (storedSettings.containsKey(key) ? "^^^" : ""));
        }

        Timer timer = new Timer();
        TimerTask checker = new TimerTask() {
            @Override
            public void run() {
                if (r_dir == -1) Log.d("CMD", settings.get("cmd_rtccw"));
                else if (r_dir == 1) Log.d("CMD", settings.get("cmd_rtcw"));
                else if (j_str > 0.1) {
                    int dg_ar_ang = Integer.parseInt(settings.get("diag_area"));
                    if (numInRange(-135 - dg_ar_ang / 2, dg_ar_ang, j_ang)) Log.d("CMD", settings.get("cmd_fwlt"));
                    else if (numInRange(-45 - dg_ar_ang / 2, dg_ar_ang, j_ang)) Log.d("CMD", settings.get("cmd_fwrt"));
                    else if (numInRange(45 - dg_ar_ang / 2, dg_ar_ang, j_ang)) Log.d("CMD", settings.get("cmd_bwrt"));
                    else if (numInRange(135 - dg_ar_ang / 2, dg_ar_ang, j_ang)) Log.d("CMD", settings.get("cmd_bwlt"));
                    else if (numInRange(-180, 45, j_ang) || numInRange(135, 45, j_ang)) Log.d("CMD", settings.get("cmd_lt"));
                    else if (numInRange(-135, 90, j_ang)) Log.d("CMD", settings.get("cmd_fw"));
                    else if (numInRange(-45, 90, j_ang)) Log.d("CMD", settings.get("cmd_rt"));
                    else if (numInRange(45, 90, j_ang)) Log.d("CMD", settings.get("cmd_bw"));
                }
            }
        };
        timer.schedule(checker, 0L, Integer.parseInt(settings.get("repeat")));
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
                    joystickMove((int)Math.round(angle * (180 / Math.PI)), strength);

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
                Intent myIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(myIntent);
                break;
            case R.id.btnBluetooth:
                Log.d("Page", "Bluetooth");
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
}
