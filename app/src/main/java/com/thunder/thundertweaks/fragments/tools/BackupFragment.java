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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.activities.FilePickerActivity;
import com.thunder.thundertweaks.fragments.DescriptionFragment;
import com.thunder.thundertweaks.fragments.recyclerview.RecyclerViewFragment;
import com.thunder.thundertweaks.utils.Device;
import com.thunder.thundertweaks.utils.Utils;
import com.thunder.thundertweaks.utils.ViewUtils;
import com.thunder.thundertweaks.utils.tools.Backup;
import com.thunder.thundertweaks.views.dialog.Dialog;
import com.thunder.thundertweaks.views.recyclerview.DescriptionView;
import com.thunder.thundertweaks.views.recyclerview.RecyclerViewItem;
import com.thunder.thundertweaks.views.recyclerview.TitleView;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by willi on 09.07.16.
 */
public class BackupFragment extends RecyclerViewFragment {

    private boolean mPermissionDenied;

    private Dialog mOptionsDialog;
    private Dialog mBackupFlashingDialog;
    private Backup.PARTITION mBackupPartition;
    private Dialog mItemOptionsDialog;
    private Dialog mDeleteDialog;
    private Dialog mRestoreDialog;

    @Override
    protected boolean showTopFab() {
        return true;
    }

    @Override
    protected Drawable getTopFabDrawable() {
        Drawable drawable = DrawableCompat.wrap(
                ContextCompat.getDrawable(getActivity(), R.drawable.ic_add));
        DrawableCompat.setTint(drawable, Color.WHITE);
        return drawable;
    }

    @Override
    public int getSpanCount() {
        int span = Utils.isTablet(getActivity()) ? Utils.getOrientation(getActivity()) ==
                Configuration.ORIENTATION_LANDSCAPE ? 4 : 3 : Utils.getOrientation(getActivity()) ==
                Configuration.ORIENTATION_LANDSCAPE ? 3 : 2;
        if (itemsSize() != 0 && span > itemsSize()) {
            span = itemsSize();
        }
        return span;
    }

    @Override
    protected void init() {
        super.init();

        if (Backup.getBootPartition() != null) {
            addViewPagerFragment(DescriptionFragment.newInstance(getString(R.string.boot_partition),
                    Backup.getBootPartition()));
        }
        if (Backup.getRecoveryPartition() != null) {
            addViewPagerFragment(DescriptionFragment.newInstance(getString(R.string.recovery_partition),
                    Backup.getRecoveryPartition()));
        }
        if (Backup.getFotaPartition() != null) {
            addViewPagerFragment(DescriptionFragment.newInstance(getString(R.string.fota_partition),
                    Backup.getFotaPartition()));
        }

        if (mOptionsDialog != null) {
            mOptionsDialog.show();
        }
        if (mBackupFlashingDialog != null) {
            mBackupFlashingDialog.show();
        }
        if (mBackupPartition != null) {
            backup(mBackupPartition);
        }
        if (mItemOptionsDialog != null) {
            mItemOptionsDialog.show();
        }
        if (mDeleteDialog != null) {
            mDeleteDialog.show();
        }
        if (mRestoreDialog != null) {
            mRestoreDialog.show();
        }
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        requestPermission(0, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void reload() {
        getHandler().postDelayed(() -> {
            clearItems();
            reload(new ReloadHandler<>());
        }, 250);
    }

    @Override
    protected void load(List<RecyclerViewItem> items) {
        super.load(items);

        if (Backup.getBootPartition() != null) {
            List<RecyclerViewItem> boot = new ArrayList<>();
            itemInit(boot, Backup.PARTITION.BOOT);
            if (boot.size() > 0) {
                TitleView titleView = new TitleView();
                titleView.setText(getString(R.string.boot_partition));
                items.add(titleView);
                items.addAll(boot);
            }
        }
        if (Backup.getRecoveryPartition() != null) {
            List<RecyclerViewItem> recovery = new ArrayList<>();
            itemInit(recovery, Backup.PARTITION.RECOVERY);
            if (recovery.size() > 0) {
                TitleView titleView = new TitleView();
                titleView.setText(getString(R.string.recovery_partition));
                items.add(titleView);
                items.addAll(recovery);
            }
        }
        if (Backup.getFotaPartition() != null) {
            List<RecyclerViewItem> fota = new ArrayList<>();
            itemInit(fota, Backup.PARTITION.FOTA);
            if (fota.size() > 0) {
                TitleView titleView = new TitleView();
                titleView.setText(getString(R.string.fota_partition));
                items.add(titleView);
                items.addAll(fota);
            }
        }
    }

    private void itemInit(List<RecyclerViewItem> items, final Backup.PARTITION partition) {
        File file = new File(Backup.getPath(partition));
        if (file.exists()) {
            for (final File image : file.listFiles()) {
                if (image.isFile()) {
                    DescriptionView descriptionView = new DescriptionView();
                    descriptionView.setTitle(image.getName().replace(".img", ""));
                    descriptionView.setSummary((image.length() / 1024L / 1024L) + getString(R.string.mb));
                    descriptionView.setOnItemClickListener(item -> {
                        mItemOptionsDialog = new Dialog(getActivity())
                                .setItems(getResources().getStringArray(R.array.backup_item_options),
                                        (dialogInterface, i) -> {
                                            switch (i) {
                                                case 0:
                                                    restore(partition, image, false);
                                                    break;
                                                case 1:
                                                    delete(image);
                                                    break;
                                            }
                                        })
                                .setOnDismissListener(dialogInterface -> mItemOptionsDialog = null);
                        mItemOptionsDialog.show();
                    });

                    items.add(descriptionView);
                }
            }
        }
    }

    @Override
    protected void onTopFabClick() {
        super.onTopFabClick();
        if (mPermissionDenied) {
            Utils.toast(R.string.permission_denied_write_storage, getActivity());
            return;
        }

        mOptionsDialog = new Dialog(getActivity()).setItems(getResources().getStringArray(
                R.array.backup_options),
                (dialogInterface, i) -> {
                    switch (i) {
                        case 0:
                            showBackupFlashingDialog(null);
                            break;
                        case 1:
                            Intent intent = new Intent(getActivity(), FilePickerActivity.class);
                            intent.putExtra(FilePickerActivity.PATH_INTENT,
                                    Environment.getExternalStorageDirectory().toString());
                            intent.putExtra(FilePickerActivity.EXTENSION_INTENT, ".img");
                            startActivityForResult(intent, 0);
                            break;
                    }
                })
                .setOnDismissListener(dialogInterface -> mOptionsDialog = null);
        mOptionsDialog.show();
    }

    private void showBackupFlashingDialog(final File file) {
        final LinkedHashMap<String, Backup.PARTITION> menu = getPartitionMenu();
        mBackupFlashingDialog = new Dialog(getActivity()).setItems(menu.keySet().toArray(
                new String[menu.size()]),
                (dialogInterface, i) -> {
                    Backup.PARTITION partition = menu.values().toArray(new Backup.PARTITION[menu.size()])[i];
                    if (file != null) {
                        restore(partition, file, true);
                    } else {
                        backup(partition);
                    }
                })
                .setOnDismissListener(dialogInterface -> mBackupFlashingDialog = null);
        mBackupFlashingDialog.show();
    }

    private void restore(final Backup.PARTITION partition, final File file, final boolean flashing) {
        mRestoreDialog = ViewUtils.dialogBuilder(getString(R.string.sure_question),
                (dialogInterface, i) -> {
                },
                (dialogInterface, i)
                        -> showDialog(new RestoreTask(getActivity(), flashing, file, partition)),
                dialogInterface -> mRestoreDialog = null, getActivity());
        mRestoreDialog.show();
    }

    private static class RestoreTask extends DialogLoadHandler<BackupFragment> {
        private File mFile;
        private Backup.PARTITION mPartition;

        private RestoreTask(Context context, boolean flashing,
                            File file, Backup.PARTITION partition) {
            super(null, context.getString(flashing ? R.string.flashing : R.string.restoring));
            mFile = file;
            mPartition = partition;
        }

        @Override
        public Void doInBackground(BackupFragment fragment) {
            Backup.restore(mFile, mPartition);
            return null;
        }
    }

    private void delete(final File file) {
        mDeleteDialog = ViewUtils.dialogBuilder(getString(R.string.sure_question),
                (dialogInterface, i) -> {
                },
                (dialogInterface, i) -> {
                    file.delete();
                    reload();
                },
                dialogInterface -> mDeleteDialog = null, getActivity());
        mDeleteDialog.show();
    }

    @Override
    public void onPermissionDenied(int request) {
        super.onPermissionDenied(request);
        if (request == 0) {
            mPermissionDenied = true;
            Utils.toast(R.string.permission_denied_write_storage, getActivity());
        }
    }

    @Override
    public void onPermissionGranted(int request) {
        super.onPermissionGranted(request);
        if (request == 0) {
            mPermissionDenied = false;
            reload();
        }
    }

    private void backup(final Backup.PARTITION partition) {
        mBackupPartition = partition;
        ViewUtils.dialogEditText(partition == Backup.PARTITION.BOOT ? Device.getKernelVersion(false) : null,
                (dialogInterface, i) -> {
                },
                text -> {
                    if (text.isEmpty()) {
                        Utils.toast(R.string.name_empty, getActivity());
                        return;
                    }

                    if (!text.endsWith(".img")) {
                        text += ".img";
                    }
                    if (Utils.existFile(Backup.getPath(partition) + "/" + text)) {
                        Utils.toast(getString(R.string.already_exists, text), getActivity());
                        return;
                    }

                    showDialog(new BackupTask(getActivity(), text, partition));
                }, getActivity())
                .setOnDismissListener(dialogInterface
                        -> mBackupPartition = null).show();
    }

    private static class BackupTask extends DialogLoadHandler<BackupFragment> {
        private String mName;
        private Backup.PARTITION mPartition;

        private BackupTask(Context context, String name, Backup.PARTITION partition) {
            super(null, context.getString(R.string.backing_up));
            mName = name;
            mPartition = partition;
        }

        @Override
        public Void doInBackground(BackupFragment fragment) {
            Backup.backup(mName, mPartition);
            return null;
        }

        @Override
        public void onPostExecute(BackupFragment fragment, Void aVoid) {
            super.onPostExecute(fragment, aVoid);
            fragment.reload();
        }
    }

    private LinkedHashMap<String, Backup.PARTITION> getPartitionMenu() {
        LinkedHashMap<String, Backup.PARTITION> partitions = new LinkedHashMap<>();
        if (Backup.getBootPartition() != null) {
            partitions.put(getString(R.string.boot_partition), Backup.PARTITION.BOOT);
        }
        if (Backup.getRecoveryPartition() != null) {
            partitions.put(getString(R.string.recovery_partition), Backup.PARTITION.RECOVERY);
        }
        if (Backup.getFotaPartition() != null) {
            partitions.put(getString(R.string.fota_partition), Backup.PARTITION.FOTA);
        }
        return partitions;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 0 && data != null) {
            showBackupFlashingDialog(new File(data.getStringExtra(FilePickerActivity.RESULT_INTENT)));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPermissionDenied = false;
    }
}
