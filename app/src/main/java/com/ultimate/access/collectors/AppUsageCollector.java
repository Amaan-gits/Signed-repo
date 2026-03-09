package com.ultimate.access.collectors;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
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

    // 🔥 NEW: INNER CLASS FOR APP INFO WITH ICON
    public static class AppInfo {
        private String packageName;
        private String appName;
        private String versionName;
        private long versionCode;
        private boolean isSystemApp;
        private String iconBase64;
        private Date firstInstallTime;
        private Date lastUpdateTime;

        public AppInfo(String packageName, String appName, String versionName,
                       long versionCode, boolean isSystemApp, Drawable iconDrawable,
                       Date firstInstallTime, Date lastUpdateTime) {
            this.packageName = packageName;
            this.appName = appName;
            this.versionName = versionName != null ? versionName : "Unknown";
            this.versionCode = versionCode;
            this.isSystemApp = isSystemApp;
            this.firstInstallTime = firstInstallTime;
            this.lastUpdateTime = lastUpdateTime;

            // 🔥 CONVERT ICON TO BASE64
            this.iconBase64 = convertIconToBase64(iconDrawable);
        }

        // 🔥 HELPER METHOD TO CONVERT ICON
        private String convertIconToBase64(Drawable iconDrawable) {
            if (iconDrawable == null) return null;

            try {
                // Convert Drawable to Bitmap
                Bitmap bitmap;
                if (iconDrawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) iconDrawable).getBitmap();
                } else {
                    // Create a bitmap from drawable
                    bitmap = Bitmap.createBitmap(iconDrawable.getIntrinsicWidth(),
                            iconDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    // Canvas etc. - simplified version
                    return null;
                }

                // Compress bitmap to PNG
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 50, baos); // 50% quality
                byte[] imageBytes = baos.toByteArray();

                // Convert to Base64
                return Base64.encodeToString(imageBytes, Base64.DEFAULT);

            } catch (Exception e) {
                Log.e(TAG, "Icon conversion failed for " + packageName + ": " + e.getMessage());
                return null;
            }
        }

        // Getters
        public String getPackageName() { return packageName; }
        public String getAppName() { return appName; }
        public String getVersionName() { return versionName; }
        public long getVersionCode() { return versionCode; }
        public boolean isSystemApp() { return isSystemApp; }
        public String getIconBase64() { return iconBase64; }
        public Date getFirstInstallTime() { return firstInstallTime; }
        public Date getLastUpdateTime() { return lastUpdateTime; }

        // 🔥 CONVERT TO MAP FOR API
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("packageName", packageName);
            map.put("appName", appName);
            map.put("versionName", versionName);
            map.put("versionCode", versionCode);
            map.put("isSystemApp", isSystemApp);
            map.put("icon", iconBase64);
            map.put("firstInstallTime", firstInstallTime != null ? firstInstallTime.getTime() : null);
            map.put("lastUpdateTime", lastUpdateTime != null ? lastUpdateTime.getTime() : null);
            return map;
        }
    }

    // 🔥 MODIFIED: getInstalledAppsWithIcons() - NEW METHOD
    public List<AppInfo> getInstalledAppsWithIcons() {
        List<AppInfo> appsList = new ArrayList<>();
        Set<String> uniquePackages = new HashSet<>();

        try {
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : packages) {
                // DUPLICATE CHECK
                if (uniquePackages.contains(appInfo.packageName)) {
                    continue;
                }
                uniquePackages.add(appInfo.packageName);

                // Get app name
                String appName = packageManager.getApplicationLabel(appInfo).toString();
                boolean isSystemApp = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;

                // Get app icon
                Drawable iconDrawable = packageManager.getApplicationIcon(appInfo);

                // Get package info for version details
                String versionName = "Unknown";
                long versionCode = 0L;
                Date firstInstallTime = new Date(0);
                Date lastUpdateTime = new Date(0);

                try {
                    PackageInfo pInfo = packageManager.getPackageInfo(appInfo.packageName, 0);
                    versionName = pInfo.versionName != null ? pInfo.versionName : "Unknown";

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        versionCode = pInfo.getLongVersionCode();
                    } else {
                        versionCode = (long) pInfo.versionCode;
                    }

                    firstInstallTime = new Date(pInfo.firstInstallTime);
                    lastUpdateTime = new Date(pInfo.lastUpdateTime);

                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Package info not found for " + appInfo.packageName);
                }

                // Create AppInfo object
                AppInfo app = new AppInfo(
                        appInfo.packageName,
                        appName,
                        versionName,
                        versionCode,
                        isSystemApp,
                        iconDrawable,
                        firstInstallTime,
                        lastUpdateTime
                );

                appsList.add(app);
            }

            Log.d(TAG, "Total installed apps with icons: " + appsList.size());

        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps: " + e.getMessage());
        }

        return appsList;
    }

    // 🔥 BACKWARD COMPATIBILITY: Old method still works
    public List<Map<String, Object>> getInstalledApps() {
        List<Map<String, Object>> appsList = new ArrayList<>();
        Set<String> uniquePackages = new HashSet<>();

        try {
            List<ApplicationInfo> packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo appInfo : packages) {
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