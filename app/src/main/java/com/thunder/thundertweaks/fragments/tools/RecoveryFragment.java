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
package com.thunder.thundertweaks.fragments.tools;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.activities.FilePickerActivity;
import com.thunder.thundertweaks.fragments.BaseFragment;
import com.thunder.thundertweaks.fragments.recyclerview.RecyclerViewFragment;
import com.thunder.thundertweaks.utils.AppSettings;
import com.thunder.thundertweaks.utils.Utils;
import com.thunder.thundertweaks.utils.ViewUtils;
import com.thunder.thundertweaks.utils.root.RootFile;
import com.thunder.thundertweaks.utils.root.RootUtils;
import com.thunder.thundertweaks.utils.tools.Recovery;
import com.thunder.thundertweaks.views.dialog.Dialog;
import com.thunder.thundertweaks.views.recyclerview.CardView;
import com.thunder.thundertweaks.views.recyclerview.DescriptionView;
import com.thunder.thundertweaks.views.recyclerview.RecyclerViewItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 12.07.16.
 */
public class RecoveryFragment extends RecyclerViewFragment {

    private Dialog mRebootDialog;
    private Dialog mRebootConfirmDialog;
    private Dialog mAddDialog;
    private Dialog mFlashDialog;

    private List<Recovery> mCommands = new ArrayList<>();

    @Override
    public int getSpanCount() {
        return 1;
    }

    @Override
    protected Drawable getTopFabDrawable() {
        Drawable drawable = DrawableCompat.wrap(
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_add));
        DrawableCompat.setTint(drawable, Color.WHITE);
        return drawable;
    }

    @Override
    protected boolean showTopFab() {
        return true;
    }

    @Override
    protected void init() {
        super.init();

        addViewPagerFragment(OptionsFragment.newInstance(this));

        if (mRebootDialog != null) {
            mRebootDialog.show();
        }
        if (mRebootConfirmDialog != null) {
            mRebootConfirmDialog.show();
        }
        if (mAddDialog != null) {
            mAddDialog.show();
        }
        if (mFlashDialog != null) {
            mFlashDialog.show();
        }
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
    }

    @Override
    protected void onTopFabClick() {
        super.onTopFabClick();

        add();
    }

    private void add() {
        mAddDialog = new Dialog(getActivity()).setItems(getResources().getStringArray(
                R.array.recovery_commands), (dialogInterface, i) -> {
            switch (i) {
                case 0:
                    addAction(Recovery.RECOVERY_COMMAND.WIPE_DATA, null);
                    break;
                case 1:
                    addAction(Recovery.RECOVERY_COMMAND.WIPE_CACHE, null);
                    break;
                case 2:
                    Intent intent = new Intent(getActivity(), FilePickerActivity.class);
                    intent.putExtra(FilePickerActivity.PATH_INTENT,
                            Environment.getExternalStorageDirectory().toString());
                    intent.putExtra(FilePickerActivity.EXTENSION_INTENT, ".zip");
                    startActivityForResult(intent, 0);
                    break;
            }
        }).setOnDismissListener(dialogInterface -> mAddDialog = null);
        mAddDialog.show();
    }

    private void addAction(Recovery.RECOVERY_COMMAND recovery_command, String path) {
        String summary = null;
        switch (recovery_command) {
            case WIPE_DATA:
                summary = getString(R.string.wipe_data);
                break;
            case WIPE_CACHE:
                summary = getString(R.string.wipe_cache);
                break;
            case FLASH_ZIP:
                summary = new File(path).getName();
                break;
        }

        final Recovery recovery = new Recovery(recovery_command, path == null ? null : new File(path));
        mCommands.add(recovery);

        CardView cardView = new CardView(getActivity());
        cardView.setOnMenuListener((cardView1, popupMenu) -> {
            popupMenu.getMenu().add(Menu.NONE, 0, Menu.NONE, getString(R.string.delete));
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 0) {
                    mCommands.remove(recovery);
                    removeItem(cardView1);
                }
                return false;
            });
        });

        DescriptionView descriptionView = new DescriptionView();
        if (path != null) {
            descriptionView.setTitle(getString(R.string.flash_zip));
        }
        descriptionView.setSummary(summary);

        cardView.addItem(descriptionView);
        addItem(cardView);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && data != null) {
            addAction(Recovery.RECOVERY_COMMAND.FLASH_ZIP,
                    data.getStringExtra(FilePickerActivity.RESULT_INTENT));
        }
    }

    private void flashNow(final int recoveryOption) {
        mFlashDialog = ViewUtils.dialogBuilder(getString(R.string.flash_now_confirm),
                (dialogInterface, i) -> {
                },
                (dialogInterface, i) -> {
                    String file = "/cache/recovery/" + mCommands.get(0).getFile(recoveryOption == 1 ?
                            Recovery.RECOVERY.TWRP : Recovery.RECOVERY.CWM);
                    RootFile recoveryFile = new RootFile(file);
                    recoveryFile.delete();
                    for (Recovery commands : mCommands) {
                        for (String command : commands.getCommands(recoveryOption == 1 ?
                                Recovery.RECOVERY.TWRP :
                                Recovery.RECOVERY.CWM))
                            recoveryFile.write(command, true);
                    }
                    RootUtils.runCommand("reboot recovery");
                },
                dialogInterface -> mFlashDialog = null, getActivity());
        mFlashDialog.show();
    }

    private void reboot() {
        mRebootDialog = new Dialog(getActivity()).setItems(getResources()
                        .getStringArray(R.array.recovery_reboot_options),
                (dialog, selection) -> {
                    mRebootConfirmDialog = ViewUtils.dialogBuilder(getString(R.string.sure_question),
                            (dialog1, which) -> {
                            },
                            (dialog12, which)
                                    -> RootUtils.runCommand(getActivity().getResources().getStringArray(
                                    R.array.recovery_reboot_values)[selection]),
                            dialog13 -> mRebootConfirmDialog = null, getActivity());
                    mRebootConfirmDialog.show();
                })
                .setOnDismissListener(dialog -> mRebootDialog = null);
        mRebootDialog.show();
    }

    public static class OptionsFragment extends BaseFragment {
        public static OptionsFragment newInstance(RecoveryFragment recoveryFragment) {
            OptionsFragment fragment = new OptionsFragment();
            fragment.mRecoveryFragment = recoveryFragment;
            return fragment;
        }

        private RecoveryFragment mRecoveryFragment;
        private int mRecoveryOption;

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_recovery_options, container, false);
            mRecoveryOption = AppSettings.getRecoveryOption(getActivity());

            LinearLayout layout = rootView.findViewById(R.id.layout);
            String[] options = getResources().getStringArray(R.array.recovery_options);

            final List<AppCompatCheckBox> checkBoxes = new ArrayList<>();
            for (int i = 0; i < options.length; i++) {
                AppCompatCheckBox checkBox = new AppCompatCheckBox(
                        new ContextThemeWrapper(getActivity(),
                                R.style.Base_Widget_AppCompat_CompoundButton_CheckBox_Custom), null, 0);
                checkBox.setText(options[i]);
                checkBox.setTextColor(Color.WHITE);
                checkBox.setChecked(i == mRecoveryOption);
                checkBox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                final int position = i;
                checkBox.setOnClickListener(view -> {
                    for (int i1 = 0; i1 < checkBoxes.size(); i1++) {
                        checkBoxes.get(i1).setChecked(position == i1);
                    }
                    AppSettings.saveRecoveryOption(position, getActivity());
                    mRecoveryOption = position;
                });

                checkBoxes.add(checkBox);
                layout.addView(checkBox);
            }

            rootView.findViewById(R.id.button).setOnClickListener(view -> {
                if (mRecoveryFragment != null && mRecoveryFragment.itemsSize() > 0) {
                    mRecoveryFragment.flashNow(mRecoveryOption);
                } else {
                    Utils.toast(R.string.add_action_first, getActivity());
                }
            });

            rootView.findViewById(R.id.reboot_button).setOnClickListener(v -> {
                if (mRecoveryFragment != null) {
                    mRecoveryFragment.reboot();
                }
            });

            return rootView;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCommands.clear();
    }
}
