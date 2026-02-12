package com.ultimate.access.collectors;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

public class BatteryCollector {

    private Context context;
    private BatteryManager batteryManager;
    private IntentFilter ifilter;
    private Intent batteryStatus;

    public BatteryCollector(Context context) {
        this.context = context;
        this.batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        this.ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        this.batteryStatus = context.registerReceiver(null, ifilter);
    }

    public int getBatteryLevel() {
        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale != -1) {
                return (level * 100) / scale;
            }
        }
        return -1;
    }

    public int getBatteryTemperature() {
        if (batteryStatus != null) {
            int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            if (temp != -1) {
                return temp / 10; // Convert to Celsius
            }
        }
        return -1;
    }

    public int getBatteryVoltage() {
        if (batteryStatus != null) {
            return batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        }
        return -1;
    }

    public String getBatteryStatus() {
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    return "CHARGING";
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    return "DISCHARGING";
                case BatteryManager.BATTERY_STATUS_FULL:
                    return "FULL";
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    return "NOT_CHARGING";
                default:
                    return "UNKNOWN";
            }
        }
        return "UNKNOWN";
    }

    public String getBatteryHealth() {
        if (batteryStatus != null) {
            int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
            switch (health) {
                case BatteryManager.BATTERY_HEALTH_GOOD:
                    return "GOOD";
                case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                    return "OVERHEAT";
                case BatteryManager.BATTERY_HEALTH_DEAD:
                    return "DEAD";
                case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                    return "OVER_VOLTAGE";
                case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                    return "FAILURE";
                default:
                    return "UNKNOWN";
            }
        }
        return "UNKNOWN";
    }

    public boolean isCharging() {
        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
        }
        return false;
    }
}
