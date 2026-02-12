package com.ultimate.access.collectors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SensorCollector implements SensorEventListener {

    private Context context;
    private SensorManager sensorManager;
    private Map<String, float[]> lastSensorValues = new HashMap<>();
    private Map<String, Long> lastSensorTimestamps = new HashMap<>();
    private List<Sensor> registeredSensors = new ArrayList<>();

    public SensorCollector(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
    }

    public void registerSensors() {
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            registeredSensors.add(sensor);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String sensorName = event.sensor.getName();
        String sensorType = getSensorTypeName(event.sensor.getType());

        float[] values = new float[event.values.length];
        System.arraycopy(event.values, 0, values, 0, event.values.length);

        lastSensorValues.put(sensorName + "_" + sensorType, values);
        lastSensorTimestamps.put(sensorName + "_" + sensorType, System.currentTimeMillis());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    public void unregisterSensors() {
        sensorManager.unregisterListener(this);
        registeredSensors.clear();
    }

    public Map<String, float[]> getLatestSensorData() {
        return new HashMap<>(lastSensorValues);
    }

    private String getSensorTypeName(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER:
                return "ACCELEROMETER";
            case Sensor.TYPE_GYROSCOPE:
                return "GYROSCOPE";
            case Sensor.TYPE_MAGNETIC_FIELD:
                return "MAGNETIC_FIELD";
            case Sensor.TYPE_LIGHT:
                return "LIGHT";
            case Sensor.TYPE_PROXIMITY:
                return "PROXIMITY";
            case Sensor.TYPE_PRESSURE:
                return "PRESSURE";
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                return "TEMPERATURE";
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                return "HUMIDITY";
            case Sensor.TYPE_HEART_RATE:
                return "HEART_RATE";
            default:
                return "UNKNOWN";
        }
    }
}