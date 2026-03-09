package com.ultimate.access.collectors;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppUsageCollector {

    private static final String TAG = "AppUsageCollector";
    private Context context;
    private UsageStatsManager usageStatsManager;
    private PackageManager packageManager;

    public AppUsageCollector(Context context) {
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        }
        this.packageManager = context.getPackageManager();
    }

    public List<Map<String, Object>> getUsageStats() {
        List<Map<String, Object>> usageList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && usageStatsManager != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            long startTime = calendar.getTimeInMillis();
            long endTime = System.currentTimeMillis();

            try {
                Map<String, UsageStats> usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime);

                if (usageStatsMap != null) {
                    for (Map.Entry<String, UsageStats> entry : usageStatsMap.entrySet()) {
                        UsageStats stats = entry.getValue();

                        if (stats != null && stats.getTotalTimeInForeground() > 0) {
                            Map<String, Object> usage = new HashMap<>();
                            usage.put("packageName", stats.getPackageName());

                            try {
                                ApplicationInfo appInfo = packageManager.getApplicationInfo(stats.getPackageName(), 0);
                                usage.put("appName", packageManager.getApplicationLabel(appInfo).toString());
                            } catch (PackageManager.NameNotFoundException e) {
                                usage.put("appName", stats.getPackageName());
                            }

                            usage.put("totalTimeInForeground", stats.getTotalTimeInForeground());
                            usage.put("lastTimeUsed", new Date(stats.getLastTimeUsed()));
                            usage.put("firstTimeUsed", new Date(stats.getFirstTimeStamp()));

                            usageList.add(usage);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting usage stats: " + e.getMessage());
            }
        }

        return usageList;
    }

    public List<Map<String, Object>> getInstalledApps() {
        List<Map<String, Object>> appsList = new ArrayList<>();
        Set<String> uniquePackages = new HashSet<>();  // 🔥 DUPLICATE HATANE KE LIYE

        try {
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : packages) {
                // 🔥 DUPLICATE CHECK
                if (uniquePackages.contains(appInfo.packageName)) {
                    continue;
                }
                uniquePackages.add(appInfo.packageName);

                Map<String, Object> app = new HashMap<>();
                app.put("packageName", appInfo.packageName);
                app.put("appName", packageManager.getApplicationLabel(appInfo).toString());
                app.put("isSystemApp", (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);

                try {
                    PackageInfo pInfo = packageManager.getPackageInfo(appInfo.packageName, 0);
                    app.put("versionName", pInfo.versionName != null ? pInfo.versionName : "Unknown");

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        app.put("versionCode", pInfo.getLongVersionCode());
                    } else {
                        app.put("versionCode", (long) pInfo.versionCode);
                    }

                    app.put("firstInstallTime", new Date(pInfo.firstInstallTime));
                    app.put("lastUpdateTime", new Date(pInfo.lastUpdateTime));

                } catch (PackageManager.NameNotFoundException e) {
                    app.put("versionName", "Unknown");
                    app.put("versionCode", 0L);
                    app.put("firstInstallTime", new Date(0));
                    app.put("lastUpdateTime", new Date(0));
                }

                appsList.add(app);
            }

            Log.d(TAG, "Total installed apps: " + appsList.size());

        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps: " + e.getMessage());
        }

        return appsList;
    }
}