/*
 * Copyright (C) 2015-2017 Willi Ye <williye97@gmail.com>
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
package com.thunder.thundertweaks.fragments.other;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;

import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.hardware.fingerprint.FingerprintManagerCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.preference.SwitchPreferenceCompat;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.recyclerview.widget.RecyclerView;

import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.activities.BannerResizerActivity;
import com.thunder.thundertweaks.activities.MainActivity;
import com.thunder.thundertweaks.activities.NavigationActivity;
import com.thunder.thundertweaks.services.boot.ApplyOnBootService;
import com.thunder.thundertweaks.utils.AppSettings;
import com.thunder.thundertweaks.utils.Themes;
import com.thunder.thundertweaks.utils.Utils;
import com.thunder.thundertweaks.utils.ViewUtils;
import com.thunder.thundertweaks.utils.root.RootUtils;
import com.thunder.thundertweaks.views.BorderCircleView;
import com.thunder.thundertweaks.views.dialog.Dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by willi on 13.08.16.
 */

public class SettingsFragment extends PreferenceFragmentCompat implements
        Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private static final String KEY_RESET_DATA = "reset_data";
    //private static final String KEY_UPDATE_NOTIFICATION = "app_update_notif";
    //private static final String KEY_CHECK_UPDATE = "check_update";
    private static final String KEY_FORCE_ENGLISH = "forceenglish";
    //private static final String KEY_USER_INTERFACE = "user_interface";
    private static final String KEY_DARK_THEME = "darktheme";
    //private static final String KEY_MATERIAL_ICON = "materialicon";
    private static final String KEY_BANNER_RESIZER = "banner_resizer";
    private static final String KEY_HIDE_BANNER = "hide_banner";
    private static final String KEY_PRIMARY_COLOR = "primary_color";
    private static final String KEY_ACCENT_COLOR = "accent_color";
    private static final String KEY_SECTIONS_ICON = "section_icons";
    private static final String KEY_APPLY_ON_BOOT_TEST = "applyonboottest";
    private static final String KEY_DEBUGGING_CATEGORY = "debugging_category";
    private static final String KEY_LOGCAT = "logcat";
    private static final String KEY_LAST_KMSG = "lastkmsg";
    private static final String KEY_DMESG = "dmesg";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_SET_PASSWORD = "set_password";
    private static final String KEY_DELETE_PASSWORD = "delete_password";
    private static final String KEY_FINGERPRINT = "fingerprint";
    private static final String KEY_SECTIONS = "sections";

    private Preference mFingerprint;

    private String mOldPassword;
    private String mDeletePassword;
    private int mColorSelection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        if (rootView != null) {
            Objects.requireNonNull(rootView).setPadding(rootView.getPaddingLeft(),
                    Math.round(ViewUtils.getActionBarSize(Objects.requireNonNull(requireActivity()))),
                    rootView.getPaddingRight(), rootView.getPaddingBottom());
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOldPassword != null) {
            editPasswordDialog(mOldPassword);
        }
        if (mDeletePassword != null) {
            deletePasswordDialog(mDeletePassword);
        }
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.settings);

        SwitchPreferenceCompat forceEnglish = (SwitchPreferenceCompat) findPreference(KEY_FORCE_ENGLISH);
        if (Resources.getSystem().getConfiguration().locale.getLanguage().startsWith("en")) {
            getPreferenceScreen().removePreference(forceEnglish);
        } else {
            forceEnglish.setOnPreferenceChangeListener(this);
        }
/*
        if (Utils.hideStartActivity()) {
            ((PreferenceCategory) findPreference(KEY_USER_INTERFACE))
                    .removePreference(findPreference(KEY_MATERIAL_ICON));
        } else {
            findPreference(KEY_MATERIAL_ICON).setOnPreferenceChangeListener(this);
        }
*/
        findPreference(KEY_RESET_DATA).setOnPreferenceClickListener(this);
        //findPreference(KEY_UPDATE_NOTIFICATION).setOnPreferenceChangeListener(this);
        //findPreference(KEY_CHECK_UPDATE).setOnPreferenceClickListener(this);
        findPreference(KEY_DARK_THEME).setOnPreferenceChangeListener(this);
        findPreference(KEY_BANNER_RESIZER).setOnPreferenceClickListener(this);
        findPreference(KEY_HIDE_BANNER).setOnPreferenceChangeListener(this);
        findPreference(KEY_PRIMARY_COLOR).setOnPreferenceClickListener(this);
        findPreference(KEY_ACCENT_COLOR).setOnPreferenceClickListener(this);
        findPreference(KEY_SECTIONS_ICON).setOnPreferenceChangeListener(this);
        findPreference(KEY_APPLY_ON_BOOT_TEST).setOnPreferenceClickListener(this);
        findPreference(KEY_LOGCAT).setOnPreferenceClickListener(this);

        if (Utils.existFile("/proc/last_kmsg") || Utils.existFile("/sys/fs/pstore/console-ramoops")) {
            findPreference(KEY_LAST_KMSG).setOnPreferenceClickListener(this);
        } else {
            ((PreferenceCategory) findPreference(KEY_DEBUGGING_CATEGORY)).removePreference(
                    findPreference(KEY_LAST_KMSG));
        }

        findPreference(KEY_DMESG).setOnPreferenceClickListener(this);
        findPreference(KEY_SET_PASSWORD).setOnPreferenceClickListener(this);
        findPreference(KEY_DELETE_PASSWORD).setOnPreferenceClickListener(this);

        if (!FingerprintManagerCompat.from(Objects.requireNonNull(requireActivity())).isHardwareDetected()) {
            ((PreferenceCategory) findPreference(KEY_SECURITY_CATEGORY)).removePreference(
                    findPreference(KEY_FINGERPRINT));
        } else {
            mFingerprint = findPreference(KEY_FINGERPRINT);
            mFingerprint.setEnabled(!AppSettings.getPassword(getActivity()).isEmpty());
        }

        NavigationActivity activity = (NavigationActivity) getActivity();
        PreferenceCategory sectionsCategory = (PreferenceCategory) findPreference(KEY_SECTIONS);
        for (NavigationActivity.NavigationFragment navigationFragment : Objects.requireNonNull(activity).getFragments()) {
            Class<? extends Fragment> fragmentClass = navigationFragment.mFragmentClass;
            int id = navigationFragment.mId;

            if (fragmentClass != null && fragmentClass != SettingsFragment.class) {
                SwitchPreferenceCompat switchPreference = new SwitchPreferenceCompat(
                        new ContextThemeWrapper(getActivity(), R.style.Preference_SwitchPreferenceCompat_Material));
                switchPreference.setSummary(getString(id));
                switchPreference.setKey(fragmentClass.getSimpleName() + "_enabled");
                switchPreference.setChecked(AppSettings.isFragmentEnabled(fragmentClass, getActivity()));
                switchPreference.setOnPreferenceChangeListener(this);
                switchPreference.setPersistent(true);
                sectionsCategory.addPreference(switchPreference);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        boolean checked = (boolean) o;
        String key = preference.getKey();
        switch (key) {
            //case KEY_UPDATE_NOTIFICATION:
                //AppSettings.saveBoolean("show_update_notif", checked, getActivity());
                //return true;
            case KEY_FORCE_ENGLISH:
            case KEY_DARK_THEME:
                Objects.requireNonNull(requireActivity()).finish();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(NavigationActivity.INTENT_SECTION,
                        SettingsFragment.class.getCanonicalName());
                startActivity(intent);
                return true;
/*
            case KEY_MATERIAL_ICON:
                Utils.setStartActivity(checked, getActivity());
                return true;
*/
            case KEY_HIDE_BANNER:
                return true;
            case KEY_SECTIONS_ICON:
                ((NavigationActivity) Objects.requireNonNull(requireActivity())).appendFragments();
                return true;
            default:
                if (key.endsWith("_enabled")) {
                    ((NavigationActivity) Objects.requireNonNull(requireActivity())).appendFragments();
                    return true;
                }
                break;
        }
        return false;
    }

    private static class MessengerHandler extends Handler {

        private final Context mContext;

        private MessengerHandler(Context context) {
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.arg1 == 1 && mContext != null) {
                Utils.toast(R.string.nothing_apply, mContext);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case KEY_RESET_DATA:
                resetDataDialog();
                return true;
            //case KEY_CHECK_UPDATE:
                //AppUpdaterTask.appCheckDialogAllways(getActivity());
                //return true;
            case KEY_BANNER_RESIZER:
                Intent intent = new Intent(getActivity(), BannerResizerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case KEY_PRIMARY_COLOR:
                colorDialog(true);
                return true;
            case KEY_ACCENT_COLOR:
                colorDialog(false);
                return true;
            case KEY_APPLY_ON_BOOT_TEST:
                if (Utils.isServiceRunning(ApplyOnBootService.class, Objects.requireNonNull(requireActivity()))) {
                    Utils.toast(R.string.apply_on_boot_running, getActivity());
                } else {
                    Intent intent2 = new Intent(getActivity(), ApplyOnBootService.class);
                    intent2.putExtra("messenger", new Messenger(new MessengerHandler(getActivity())));
                    Utils.startService(Objects.requireNonNull(requireActivity()), intent2);
                }
                return true;
            case KEY_LOGCAT:
                new Execute(getActivity()).execute("logcat -d > /sdcard/logcat.txt");
                return true;
            case KEY_LAST_KMSG:
                if (Utils.existFile("/proc/last_kmsg")) {
                    new Execute(getActivity()).execute("cat /proc/last_kmsg > /sdcard/last_kmsg.txt");
                } else if (Utils.existFile("/sys/fs/pstore/console-ramoops")) {
                    new Execute(getActivity()).execute("cat /sys/fs/pstore/console-ramoops > /sdcard/last_kmsg.txt");
                }
                return true;
            case KEY_DMESG:
                new Execute(getActivity()).execute("dmesg > /sdcard/dmesg.txt");
                return true;
            case KEY_SET_PASSWORD:
                editPasswordDialog(AppSettings.getPassword(getActivity()));
                return true;
            case KEY_DELETE_PASSWORD:
                deletePasswordDialog(AppSettings.getPassword(getActivity()));
                return true;
        }
        return false;
    }

    private static class Execute extends AsyncTask<String, Void, Void> {
        private final ProgressDialog mProgressDialog;

        private Execute(Context context) {
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage(context.getString(R.string.executing));
            mProgressDialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            RootUtils.runCommand(params[0]);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mProgressDialog.dismiss();
        }
    }

    private void resetDataDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(Objects.requireNonNull(requireActivity()));
        alert.setTitle(getString(R.string.reset_data_title));
        alert.setMessage(getString(R.string.reset_data_dialog1));
        alert.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
        });
        alert.setPositiveButton(getString(R.string.ok), (dialog, id) -> {

            AlertDialog.Builder alert2 = new AlertDialog.Builder(Objects.requireNonNull(requireActivity()));
            alert2.setTitle(getString(R.string.reset_data_dialog2_title));
            alert2.setMessage(getString(R.string.reset_data_dialog2_message));
            alert2.setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                RootUtils.runCommand("rm -rf /data/.thundertweaks");
                RootUtils.runCommand("pm clear com.thunder.thundertweaks");
            });
            alert2.setPositiveButton(getString(R.string.ok), (dialog1, id1) -> {
                RootUtils.runCommand("rm -rf /data/.thundertweaks");
                RootUtils.runCommand("pm clear com.thunder.thundertweaks && reboot");
            });
            alert2.show();
        });
        alert.show();
    }

    private void editPasswordDialog(final String oldPass) {
        mOldPassword = oldPass;

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        int padding = Math.round(getResources().getDimension(R.dimen.dialog_padding));
        linearLayout.setPadding(padding, padding, padding, padding);

        final AppCompatEditText oldPassword = new AppCompatEditText(Objects.requireNonNull(requireActivity()));
        if (!oldPass.isEmpty()) {
            oldPassword.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            oldPassword.setHint(getString(R.string.old_password));
            linearLayout.addView(oldPassword);
        }

        final AppCompatEditText newPassword = new AppCompatEditText(Objects.requireNonNull(requireActivity()));
        newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        newPassword.setHint(getString(R.string.new_password));
        linearLayout.addView(newPassword);

        final AppCompatEditText confirmNewPassword = new AppCompatEditText(Objects.requireNonNull(requireActivity()));
        confirmNewPassword.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        confirmNewPassword.setHint(getString(R.string.confirm_new_password));
        linearLayout.addView(confirmNewPassword);

        new Dialog(getActivity()).setView(linearLayout)
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> {
                })
                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                    if (!oldPass.isEmpty() && !Objects.requireNonNull(oldPassword.getText()).toString().equals(Utils
                            .decodeString(oldPass))) {
                        Utils.toast(getString(R.string.old_password_wrong), getActivity());
                        return;
                    }

                    if (Objects.requireNonNull(newPassword.getText()).toString().isEmpty()) {
                        Utils.toast(getString(R.string.password_empty), getActivity());
                        return;
                    }

                    if (!newPassword.getText().toString().equals(Objects.requireNonNull(confirmNewPassword.getText())
                            .toString())) {
                        Utils.toast(getString(R.string.password_not_match), getActivity());
                        return;
                    }

                    if (newPassword.getText().toString().length() > 32) {
                        Utils.toast(getString(R.string.password_too_long), getActivity());
                        return;
                    }

                    AppSettings.savePassword(Utils.encodeString(newPassword.getText()
                            .toString()), getActivity());
                    if (mFingerprint != null) {
                        mFingerprint.setEnabled(true);
                    }
                })
                .setOnDismissListener(dialogInterface -> mOldPassword = null).show();
    }

    private void deletePasswordDialog(final String password) {
        if (password.isEmpty()) {
            Utils.toast(getString(R.string.set_password_first), getActivity());
            return;
        }

        mDeletePassword = password;

        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setGravity(Gravity.CENTER);
        int padding = Math.round(getResources().getDimension(R.dimen.dialog_padding));
        linearLayout.setPadding(padding, padding, padding, padding);

        final AppCompatEditText mPassword = new AppCompatEditText(Objects.requireNonNull(requireActivity()));
        mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        mPassword.setHint(getString(R.string.password));
        linearLayout.addView(mPassword);

        new Dialog(getActivity()).setView(linearLayout)
                .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> {
                    if (!Objects.requireNonNull(mPassword.getText()).toString().equals(Utils.decodeString(password))) {
                        Utils.toast(getString(R.string.password_wrong), getActivity());
                        return;
                    }

                    AppSettings.resetPassword(getActivity());
                    if (mFingerprint != null) {
                        mFingerprint.setEnabled(false);
                    }
                })
                .setOnDismissListener(dialogInterface -> mDeletePassword = null).show();
    }

    private void colorDialog(boolean primaryColor) {
        mColorSelection = -1;
        List<String> colors = new ArrayList<>(primaryColor ?
                Themes.sPrimaryColors : Themes.sAccentColors);
        String counterPartColor = primaryColor ?
                Themes.getAccentColor(getActivity()) : Themes.getPrimaryColor(getActivity());
        String counterPartColorName = counterPartColor.replaceAll("[A-Z].+", "");
        for (int i = 0; i < colors.size(); i++) {
            if (colors.get(i).contains(counterPartColorName)) {
                colors.remove(i);
                break;
            }
        }

        int selection = colors.indexOf(primaryColor ?
                Themes.getPrimaryColor(getActivity()) : Themes.getAccentColor(getActivity()));
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) getResources().getDimension(R.dimen.dialog_padding);
        linearLayout.setPadding(padding, padding, padding, padding);

        final List<BorderCircleView> circles = new ArrayList<>();

        LinearLayout subView = null;
        for (int i = 0; i < colors.size(); i++) {
            if (subView == null || i % 5 == 0) {
                subView = new LinearLayout(getActivity());
                subView.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                linearLayout.addView(subView);
            }

            BorderCircleView circle = new BorderCircleView(getActivity());
            circle.setChecked(i == selection);
            circle.setCircleColor(ContextCompat.getColor(Objects.requireNonNull(requireActivity()),
                    Themes.getColor(colors.get(i), Objects.requireNonNull(requireActivity()))));
            circle.setBorderColor(ContextCompat.getColor(Objects.requireNonNull(requireActivity()),
                    Themes.getColor(counterPartColor, Objects.requireNonNull(requireActivity()))));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            int margin = (int) getResources().getDimension(R.dimen.color_dialog_margin);
            params.setMargins(margin, margin, margin, margin);
            circle.setLayoutParams(params);
            circle.setOnClickListener(v -> {
                for (BorderCircleView borderCircleView : circles) {
                    if (v == borderCircleView) {
                        borderCircleView.setChecked(true);
                        mColorSelection = circles.indexOf(borderCircleView);
                    } else {
                        borderCircleView.setChecked(false);
                    }
                }
            });

            circles.add(circle);
            subView.addView(circle);
        }

        new Dialog(getActivity()).setView(linearLayout)
                .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                })
                .setPositiveButton(getString(R.string.ok), (dialog, which) -> {
                    if (mColorSelection >= 0) {
                        if (primaryColor) {
                            Themes.savePrimaryColor(colors.get(mColorSelection), getActivity());
                        } else {
                            Themes.saveAccentColor(colors.get(mColorSelection), getActivity());
                        }
                    }
                    Objects.requireNonNull(requireActivity()).finish();
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra(NavigationActivity.INTENT_SECTION,
                            SettingsFragment.class.getCanonicalName());
                    startActivity(intent);
                })
                .setOnDismissListener(dialog -> mColorSelection = -1).show();
    }

    @Override
    protected RecyclerView.Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
        return new PreferenceGroupAdapter(preferenceScreen) {
            @SuppressLint("RestrictedApi")
            @Override
            public void onBindViewHolder(PreferenceViewHolder holder, int position) {
                super.onBindViewHolder(holder, position);
                Preference preference = getItem(position);
                if (preference instanceof PreferenceCategory)
                    setZeroPaddingToLayoutChildren(holder.itemView);
                else {
                    View iconFrame = holder.itemView.findViewById(R.id.icon_frame);
                    if (iconFrame != null) {
                        iconFrame.setVisibility(preference.getIcon() == null ? View.GONE : View.VISIBLE);
                    }
                }
            }
        };
    }
    private void setZeroPaddingToLayoutChildren(View view) {
        if (!(view instanceof ViewGroup))
            return;
        ViewGroup viewGroup = (ViewGroup) view;
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            setZeroPaddingToLayoutChildren(viewGroup.getChildAt(i));
            viewGroup.setPaddingRelative(0, viewGroup.getPaddingTop(), viewGroup.getPaddingEnd(), viewGroup.getPaddingBottom());
        }
    }
}
