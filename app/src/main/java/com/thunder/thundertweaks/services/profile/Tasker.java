/*
 * Copyright (C) 2015-2016 Willi Ye <williye97@gmail.com>
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
package com.thunder.thundertweaks.services.profile;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.services.boot.ApplyOnBoot;
import com.thunder.thundertweaks.utils.AppSettings;
import com.thunder.thundertweaks.utils.Log;
import com.thunder.thundertweaks.utils.Utils;
import com.thunder.thundertweaks.utils.kernel.cpu.CPUFreq;
import com.thunder.thundertweaks.utils.root.RootUtils;

/**
 * Created by willi on 21.07.16.
 */
public class Tasker extends BroadcastReceiver {

    public static final String EXTRA_BUNDLE = "com.twofortyfouram.locale.intent.extra.BUNDLE";
    public static final String EXTRA_STRING_BLURB = "com.twofortyfouram.locale.intent.extra.BLURB";
    public static final String ACTION_FIRE_SETTING = "com.twofortyfouram.locale.intent.action.FIRE_SETTING";
    public static final String BUNDLE_EXTRA_STRING_MESSAGE = "com.thunder.thundertweaks.tasker.extra.STRING_MESSAGE";
    public static final String BUNDLE_EXTRA_INT_VERSION_CODE = "com.thunder.thundertweaks.tasker.extra.INT_VERSION_CODE";
    public static final String DIVIDER = "wkefnewnfewp";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_FIRE_SETTING.equals(intent.getAction())) {
            return;
        }

        final Bundle bundle = intent.getBundleExtra(EXTRA_BUNDLE);
        if (isBundleValid(bundle)) {
            String commands = bundle.getString(BUNDLE_EXTRA_STRING_MESSAGE);
            if (commands != null) {
                String[] cs = commands.split(DIVIDER);
                Log.i("Applying " + cs[0]);
                if (AppSettings.isShowTaskerToast(context)) {
                    Utils.toast(context.getString(R.string.applying_profile, cs[0]), context, Toast.LENGTH_LONG);
                }

                if (cs.length > 1) {
                    RootUtils.SU su = new RootUtils.SU();
                    for (int i = 1; i < cs.length; i++) {
                        if (cs[i].isEmpty()) {
                            continue;
                        }
                        synchronized (this) {
                            CPUFreq.ApplyCpu applyCpu;
                            if (cs[i].startsWith("#") && (applyCpu =
                                    new CPUFreq.ApplyCpu(cs[i].substring(1))).toString() != null) {
                                for (String applyCpuCommand : ApplyOnBoot.getApplyCpu(applyCpu, su)) {
                                    Log.i("Run: " + applyCpuCommand);
                                    su.runCommand(applyCpuCommand);
                                }
                            } else {
                                Log.i("Run: " + cs[i]);
                                su.runCommand(cs[i]);
                            }
                        }
                    }
                    su.close();
                }
            }
        }
    }

    private boolean isBundleValid(Bundle bundle) {
        return null != bundle && !(!bundle.containsKey(BUNDLE_EXTRA_STRING_MESSAGE)
                || !bundle.containsKey(BUNDLE_EXTRA_INT_VERSION_CODE)) && !(2 != bundle.keySet().size()
                || "".equals(bundle.getString(BUNDLE_EXTRA_STRING_MESSAGE)))
                && bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 0) == bundle.getInt(BUNDLE_EXTRA_INT_VERSION_CODE, 1);
    }

}
