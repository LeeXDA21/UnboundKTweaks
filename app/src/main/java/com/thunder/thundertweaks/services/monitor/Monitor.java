/*
 * Copyright (C) 2017 Willi Ye <williye97@gmail.com>
 *
 * This file is part of Kernel Adiutor.
 *
 * Kernel Adiutor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Kernel Adiutor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Kernel Adiutor.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.thunder.thundertweaks.services.monitor;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import com.unbound.UnboundKtweaks.BuildConfig;
import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.activities.MainActivity;
import com.thunder.thundertweaks.activities.NavigationActivity;
import com.thunder.thundertweaks.database.Settings;
import com.thunder.thundertweaks.fragments.tools.DataSharingFragment;
import com.thunder.thundertweaks.utils.AppSettings;
import com.thunder.thundertweaks.utils.Device;
import com.thunder.thundertweaks.utils.Utils;
import com.thunder.thundertweaks.utils.server.ServerCreateDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by willi on 01.12.16.
 */

public class Monitor extends Service {

    private static final String PACKAGE = Monitor.class.getCanonicalName();
    public static final String ACTION_DISABLE = PACKAGE + ".ACTION.DISABLE";

    private static final String CHANNEL_ID = "monitor_notification_channel";

    private int mLevel;
    private long mTime;
    private List<Long> mTimes = new ArrayList<>();
    private ServerCreateDevice mServerCreateDevice = new ServerCreateDevice("https://www.grarak.com");
    private boolean mScreenOn;
    private boolean mCalculating;

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;

            if (charging || !mScreenOn) {
                mLevel = 0;
                mTime = 0;
            } else {
                mCalculating = true;

                long time = SystemClock.elapsedRealtime();
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

                if (mLevel != 0 && mLevel > level && mTime != 0 && mTime < time && mLevel - level > 0) {
                    long seconds = TimeUnit.SECONDS.convert((time - mTime) / (mLevel - level),
                            TimeUnit.MILLISECONDS);
                    if (seconds >= 100) {
                        mTimes.add(seconds);

                        if (mTimes.size() % 15 == 0) {
                            postCreate(mTimes.toArray(new Long[mTimes.size()]));
                            if (mTimes.size() >= 100) {
                                mTimes.clear();
                            }
                        }
                    }
                }
                mLevel = level;
                mTime = time;
            }

            mCalculating = false;
        }
    };

    private BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mScreenOn = intent.getAction().equals(Intent.ACTION_SCREEN_ON);
            if (!mScreenOn && !mCalculating) {
                mLevel = 0;
                mTime = 0;
            }
        }
    };

    private void postCreate(final Long[] times) {
        if (mLevel < 15 || !AppSettings.isDataSharing(this)) return;

        new Thread(() -> {
            try {
                JSONObject data = new JSONObject();
                data.put("android_id", Utils.getAndroidId(Monitor.this));
                data.put("android_version", Device.getVersion());
                data.put("kernel_version", Device.getKernelVersion(true, false));
                data.put("app_version", BuildConfig.VERSION_NAME);
                data.put("board", Device.getBoard());
                data.put("model", Device.getModel());
                data.put("vendor", Device.getVendor());
                data.put("cpuinfo", Utils.encodeString(Device.CPUInfo.getInstance().getCpuInfo()));
                data.put("fingerprint", Device.getFingerprint());

                JSONArray commands = new JSONArray();
                Settings settings = new Settings(Monitor.this);
                for (Settings.SettingsItem item : settings.getAllSettings()) {
                    commands.put(item.getSetting());
                }
                data.put("commands", commands);

                JSONArray batteryTimes = new JSONArray();
                for (long time : times) {
                    batteryTimes.put(time);
                }
                data.put("times", batteryTimes);

                try {
                    long time = 0;
                    for (int i = 0; i < 100000; i++) {
                        time += Utils.computeSHAHash(Utils.getRandomString(16));
                    }
                    data.put("cpu", time);
                } catch (Exception ignored) {
                }

                mServerCreateDevice.postDeviceCreate(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }


    private IBinder mBinder = new MonitorBinder();

    public class MonitorBinder extends Binder {
        public void onSettingsChange() {
            if (mTimes != null) {
                mTimes.clear();
                mLevel = 0;
                mTime = 0;
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.data_sharing), NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(notificationChannel);

            int disableIntentFlags, contentIntentFlags;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                disableIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
                contentIntentFlags = PendingIntent.FLAG_IMMUTABLE;
            } else {
                disableIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
                contentIntentFlags = 0;
            }

            PendingIntent disableIntent = PendingIntent.getBroadcast(this, 1,
                    new Intent(this, DisableReceiver.class),
                    disableIntentFlags);


            Intent launchIntent = new Intent(this, MainActivity.class);
            launchIntent.setAction(Intent.ACTION_VIEW);
            launchIntent.putExtra(NavigationActivity.INTENT_SECTION,
                    DataSharingFragment.class.getCanonicalName());
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                    launchIntent, contentIntentFlags);

            /*
            Notification.Builder builder =
                    new Notification.Builder(this, CHANNEL_ID);
            builder.setContentTitle(getString(R.string.data_sharing))
                    .setContentText(getString(R.string.data_sharing_summary_notification))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(contentIntent)
                    .addAction(0, getString(R.string.disable), disableIntent)
                    .setPriority(Notification.PRIORITY_MIN);

            startForeground(NotificationId.MONITOR, builder.build());
            */
        }

        registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(mScreenReceiver, screenFilter);

        mScreenOn = Utils.isScreenOn(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBatteryReceiver);
        unregisterReceiver(mScreenReceiver);
    }

    public static class DisableReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, Intent intent) {
            AppSettings.saveDataSharing(false, context);
            context.stopService(new Intent(context, Monitor.class));

            context.sendBroadcast(new Intent(ACTION_DISABLE));
        }

    }


}
