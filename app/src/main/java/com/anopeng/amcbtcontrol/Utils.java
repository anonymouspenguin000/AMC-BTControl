package com.anopeng.amcbtcontrol;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;

public class Utils {
    public static HashMap<String, String> fetchSettings(Context context) {
        HashMap<String, String> settings = new HashMap<>();

        FileIO settFile = new FileIO(context, "settings.txt");
        String settStr = settFile.read();

        for (String entry : settStr.split(",")) {
            String[] pair = entry.split("=");
            if (pair.length >= 2 && pair[0] != "" && pair[1] != "") settings.put(pair[0], pair[1]);
        }

        return settings;
    }
    public static void saveSettings(Context context, HashMap<String, String> settings) {
        ArrayList<String> entries = new ArrayList<>();
        for (String key : settings.keySet()) {
            String val = settings.get(key);
            if (val == "") continue;
            entries.add(key + "=" + val);
        }
        String toStore = String.join(",", entries);

        FileIO settFile = new FileIO(context, "settings.txt");
        settFile.write(toStore);
    }
}
