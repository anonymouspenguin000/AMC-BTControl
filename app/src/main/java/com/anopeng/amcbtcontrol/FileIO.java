package com.anopeng.amcbtcontrol;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileIO {
    File file;
    public FileIO(Context ctx, String name) {
        file = new File(ctx.getFilesDir(), name);
    }
    public void write(String value) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(value.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String read() {
        byte[] content = new byte[(int) file.length()];
        try {
            FileInputStream fis = new FileInputStream(file);
            fis.read(content);
            return new String(content);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
