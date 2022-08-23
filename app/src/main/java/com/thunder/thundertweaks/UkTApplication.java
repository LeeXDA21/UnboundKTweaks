package com.thunder.thundertweaks;

import android.app.Application;
import android.content.Intent;

import com.google.android.material.color.DynamicColors;
import com.thunder.thundertweaks.activities.BaseActivity;
import com.thunder.thundertweaks.activities.SecurityActivity;
import com.thunder.thundertweaks.activities.StartActivity;

public class UkTApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        DynamicColors.applyToActivitiesIfAvailable(this);
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
    }
}
