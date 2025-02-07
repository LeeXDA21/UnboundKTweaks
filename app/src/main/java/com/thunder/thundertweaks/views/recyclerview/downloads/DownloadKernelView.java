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
package com.thunder.thundertweaks.views.recyclerview.downloads;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.utils.DownloadTask;
import com.thunder.thundertweaks.utils.Utils;
import com.thunder.thundertweaks.utils.ViewUtils;
import com.thunder.thundertweaks.utils.root.RootFile;
import com.thunder.thundertweaks.utils.root.RootUtils;
import com.thunder.thundertweaks.utils.tools.Recovery;
import com.thunder.thundertweaks.utils.tools.SupportedDownloads;
import com.thunder.thundertweaks.views.dialog.Dialog;
import com.thunder.thundertweaks.views.recyclerview.RecyclerViewItem;

import java.io.File;
import java.util.List;

/**
 * Created by willi on 07.07.16.
 */
public class DownloadKernelView extends RecyclerViewItem {

    private final Activity mActvity;
    private final SupportedDownloads.KernelContent.Download mDownload;

    private Drawable mDownloadDrawable;
    private Drawable mCancelDrawable;
    private DownloadTask mDownloadTask;
    private Thread mCheckMD5Task;

    private View mDownloadSection;
    private View mProgressParent;
    private TextView mProgressText;
    private ProgressBar mProgressBar;
    private FloatingActionButton mFabButton;
    private View mCheckMD5;
    private View mMismatchMD5;
    private View mInstallButton;

    private int mRecoverySelection;

    public DownloadKernelView(Activity activity, SupportedDownloads.KernelContent.Download download) {
        mActvity = activity;
        mDownload = download;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.rv_download_kernel_view;
    }

    @Override
    public void onCreateView(View view) {
        super.onCreateView(view);

        if (mDownloadDrawable == null) {
            mDownloadDrawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_download);
            DrawableCompat.setTint(mDownloadDrawable, Color.WHITE);
        }
        if (mCancelDrawable == null) {
            mCancelDrawable = ContextCompat.getDrawable(view.getContext(), R.drawable.ic_cancel);
            DrawableCompat.setTint(mCancelDrawable, Color.WHITE);
        }

        final TextView title = view.findViewById(R.id.title);
        TextView summary = view.findViewById(R.id.summary);
        TextView changelog = view.findViewById(R.id.changelog);
        mDownloadSection = view.findViewById(R.id.downloadSection);
        mProgressParent = view.findViewById(R.id.progressParent);
        mProgressText = view.findViewById(R.id.progressText);
        mProgressBar = view.findViewById(R.id.progressbar);
        mFabButton = view.findViewById(R.id.fab_button);
        mCheckMD5 = view.findViewById(R.id.checkmd5);
        mMismatchMD5 = view.findViewById(R.id.md5_mismatch);
        mInstallButton = view.findViewById(R.id.install);

        final CharSequence titleText = Utils.htmlFrom(mDownload.getName());
        title.setText(titleText);
        title.setMovementMethod(LinkMovementMethod.getInstance());

        String description = mDownload.getDescription();
        if (description != null) {
            summary.setText(Utils.htmlFrom(description));
            summary.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            summary.setVisibility(View.GONE);
        }

        List<String> changelogs = mDownload.getChangelogs();
        if (changelogs.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String change : changelogs) {
                if (stringBuilder.length() == 0) {
                    stringBuilder.append("\u2022").append(" ").append(change);
                } else {
                    stringBuilder.append("<br>").append("\u2022").append(" ").append(change);
                }
            }
            changelog.setText(Utils.htmlFrom(stringBuilder.toString()));
            changelog.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            changelog.setVisibility(View.GONE);
        }

        mMismatchMD5.setVisibility(View.GONE);
        mInstallButton.setVisibility(View.GONE);
        if (mCheckMD5Task == null) {
            mDownloadSection.setVisibility(View.VISIBLE);
            mCheckMD5.setVisibility(View.GONE);
        }
        mProgressParent.setVisibility(mDownloadTask == null ? View.INVISIBLE : View.VISIBLE);
        mFabButton.setImageDrawable(mDownloadTask == null ? mDownloadDrawable : mCancelDrawable);
        mFabButton.setOnClickListener(view1 -> {
            if (mDownloadTask == null) {
                mFabButton.setImageDrawable(mCancelDrawable);
                mProgressParent.setVisibility(View.VISIBLE);
                mMismatchMD5.setVisibility(View.GONE);
                mInstallButton.setVisibility(View.GONE);
                mProgressBar.setProgress(0);
                mProgressText.setText("");

                mProgressBar.setIndeterminate(true);
                mDownloadTask = new DownloadTask(mActvity, new DownloadTask.OnDownloadListener() {

                    private int mPercentage;

                    @Override
                    public void onUpdate(String url, int currentSize, int totalSize) {
                        int percentage = Math.round(currentSize * 100f / totalSize);
                        if (mPercentage != percentage) {
                            mPercentage = percentage;
                            mProgressBar.setIndeterminate(false);
                            double current = Utils.roundTo2Decimals(currentSize / 1024D / 1024D);
                            double total = Utils.roundTo2Decimals(totalSize / 1024D / 1024D);
                            mProgressBar.setProgress(mPercentage);
                            mProgressText.setText(view1.getContext().getString(R.string.downloading_counting,
                                    String.valueOf(current), String.valueOf(total)) + view1.getContext()
                                    .getString(R.string.mb));
                        }
                    }

                    @Override
                    public void onSuccess(String url, String path) {
                        mFabButton.setImageDrawable(mDownloadDrawable);
                        mProgressParent.setVisibility(View.INVISIBLE);
                        mDownloadTask = null;
                        checkMD5(mDownload.getMD5sum(), path, mDownload.getInstallMethod());
                    }

                    @Override
                    public void onCancel(String url) {
                        mFabButton.setImageDrawable(mDownloadDrawable);
                        mProgressParent.setVisibility(View.INVISIBLE);
                        mMismatchMD5.setVisibility(View.GONE);
                        mDownloadTask = null;
                    }

                    @Override
                    public void onFailure(String url) {
                        Utils.toast(view1.getContext().getString(R.string.download_error, titleText),
                                view1.getContext());
                        mFabButton.setImageDrawable(mDownloadDrawable);
                        mProgressParent.setVisibility(View.INVISIBLE);
                        mDownloadTask = null;
                    }
                });
                mDownloadTask.get(mDownload.getUrl(),
                        Utils.getInternalDataStorage() + "/downloads/" + titleText.toString() + ".zip");
            } else {
                mFabButton.setImageDrawable(mDownloadDrawable);
                mProgressParent.setVisibility(View.INVISIBLE);
                mDownloadTask.cancel();
                mDownloadTask = null;
            }
        });
    }

    private void checkMD5(final String md5, final String path, final String installMethod) {
        if (mCheckMD5Task == null) {
            mDownloadSection.setVisibility(View.GONE);
            mCheckMD5Task = new Thread(() -> {
                final boolean match = Utils.checkMD5(md5, new File(path));
                mActvity.runOnUiThread(()
                        -> postMD5Check(match, path, installMethod));
            });
            mCheckMD5Task.start();
        }
    }

    private void postMD5Check(boolean match, final String path, final String installMethod) {
        if (match) {
            mInstallButton.setVisibility(View.VISIBLE);
            mInstallButton.setOnClickListener(view -> {
                if (!Utils.existFile(path)) {
                    Utils.toast(view.getContext().getString(R.string.went_wrong),
                            view.getContext());
                    return;
                }
                if (installMethod != null) {
                    ViewUtils.dialogBuilder(view.getContext().getString(R.string.sure_question),
                            (dialogInterface, i) -> {
                            },
                            (dialogInterface, i) -> {
                                RootUtils.runCommand(installMethod.replace("$FILE", path));
                                RootUtils.runCommand("rm -f " + path);
                                RootUtils.runCommand("reboot");
                            },
                            dialogInterface -> {
                            }, view.getContext()).setTitle(view.getContext().getString(
                            R.string.install)).show();
                } else {
                    mRecoverySelection = 0;
                    String[] items = view.getResources().getStringArray(R.array.downloads_recovery);
                    new Dialog(view.getContext()).setSingleChoiceItems(items, 0,
                            (dialogInterface, i) -> mRecoverySelection = i).setPositiveButton(view.getContext().getString(R.string.ok),
                            (dialogInterface, i) -> {
                                if (mRecoverySelection == 2) {
                                    Utils.toast(view.getContext().getString(
                                            R.string.file_location, path), view.getContext());
                                    return;
                                }

                                Recovery recovery = new Recovery(
                                        Recovery.RECOVERY_COMMAND.FLASH_ZIP, new File(path));
                                Recovery.RECOVERY type = mRecoverySelection == 1 ?
                                        Recovery.RECOVERY.TWRP : Recovery.RECOVERY.CWM;
                                RootFile recoveryFile = new RootFile("/cache/recovery/"
                                        + recovery.getFile(type));
                                for (String command : recovery.getCommands(type)) {
                                    recoveryFile.write(command, true);
                                }
                                RootUtils.runCommand("reboot recovery");
                            }).show();
                }
            });
        } else {
            mMismatchMD5.setVisibility(View.VISIBLE);
        }
        mCheckMD5.setVisibility(View.GONE);
        mDownloadSection.setVisibility(View.VISIBLE);
        mCheckMD5Task = null;
    }

    public void pause() {
        if (mDownloadTask != null) {
            mDownloadTask.pause();
        }
    }

    public void resume() {
        if (mDownloadTask != null) {
            mDownloadTask.resume();
        }
    }

    public void cancel() {
        if (mDownloadTask != null) {
            mDownloadTask.cancel();
        }
    }

    @Override
    protected boolean cardCompatible() {
        return false;
    }
}
