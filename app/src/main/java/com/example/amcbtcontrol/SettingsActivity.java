package com.example.amcbtcontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.util.HashMap;

public class SettingsActivity extends AppCompatActivity {
    HashMap<String, String> settings;
    int[] ids = {
            R.id.inpCmdForward,
            R.id.inpCmdBackward,
            R.id.inpCmdLeft,
            R.id.inpCmdRight,
            R.id.inpCmdFwLeft,
            R.id.inpCmdFwRight,
            R.id.inpCmdBwLeft,
            R.id.inpCmdBwRight,
            R.id.inpCmdClockwise,
            R.id.inpCmdCounterclock,
            R.id.inpRepeat,
            R.id.inpDiagArea
    };
    String[] paramNames = {
            "cmd_fw",
            "cmd_bw",
            "cmd_lt",
            "cmd_rt",
            "cmd_fwlt",
            "cmd_fwrt",
            "cmd_bwlt",
            "cmd_bwrt",
            "cmd_rtcw",
            "cmd_rtccw",
            "repeat",
            "diag_area"
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = Utils.fetchSettings(getApplicationContext());
        for (int i = 0; i < ids.length; i++) {
            EditText currView = findViewById(ids[i]);
            String currParamName = paramNames[i];
            currView.setText(settings.get(currParamName));
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btnSave:
                String err = "";
                for (int i = 0; i < ids.length; i++) {
                    EditText currInput = findViewById(ids[i]);
                    String currInputVal = currInput.getText().toString();
                    String currParamName = paramNames[i];

                    boolean _ct = false;
                    switch (currParamName) {
                        case "diag_area":
                            if (currInputVal.length() > 0) if (Integer.parseInt(currInputVal) > 90) {
                                err = "Diag - Max 90";
                                _ct = true;
                            }
                            break;
                    }
                    if (_ct) continue;
                    settings.put(currParamName, currInputVal);
                }

                Utils.saveSettings(getApplicationContext(), settings);

                Toast.makeText(this, err != "" ? err : "Saved", Toast.LENGTH_SHORT).show();

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
}
