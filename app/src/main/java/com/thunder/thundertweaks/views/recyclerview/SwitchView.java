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
package com.thunder.thundertweaks.views.recyclerview;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;

import com.unbound.UnboundKtweaks.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by willi on 05.05.16.
 */
public class SwitchView extends RecyclerViewItem {

    public interface OnSwitchListener {
        void onChanged(SwitchView switchView, boolean isChecked);
    }

    private AppCompatTextView mTitle;
    private AppCompatTextView mSummary;
    private SwitchCompat mSwitcher;

    private CharSequence mTitleText;
    private CharSequence mSummaryText;
    private CharSequence mSummaryOnText;
    private CharSequence mSummaryOffText;
    private boolean mChecked;
    private boolean mEnabled = true;
    private float mAlpha = 1f;
    private View mView;

    private List<OnSwitchListener> mOnSwitchListeners = new ArrayList<>();

    @Override
    public int getLayoutRes() {
        return R.layout.rv_switch_view;
    }

    @Override
    public void onCreateView(View view) {
        mView = view;
        mTitle = mView.findViewById(R.id.title);
        mSummary = mView.findViewById(R.id.summary);
        mSwitcher = mView.findViewById(R.id.switcher);

        super.onCreateView(mView);

        mView.setEnabled(mEnabled);

        mView.setOnClickListener(v -> {
            mSwitcher.setChecked(!mChecked);
            if (mSummary != null && mSummaryOnText != null && mSummaryOffText != null) {
                refresh();
            }
        });
        mSwitcher.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mChecked = isChecked;
            List<OnSwitchListener> applied = new ArrayList<>();
            for (OnSwitchListener onSwitchListener : mOnSwitchListeners) {
                if (applied.indexOf(onSwitchListener) == -1) {
                    onSwitchListener.onChanged(SwitchView.this, isChecked);
                    applied.add(onSwitchListener);
                }
            }
        });
    }

    public void setTitle(CharSequence title) {
        mTitleText = title;
        refresh();
    }

    public void setSummary(CharSequence summary) {
        mSummaryText = summary;
        refresh();
    }

    public void setSummaryOn(CharSequence summary) {
        mSummaryOnText = summary;
        refresh();
    }

    public void setSummaryOff(CharSequence summary) {
        mSummaryOffText = summary;
        refresh();
    }

    public void setChecked(boolean checked) {
        mChecked = checked;
        refresh();
    }

    public void setEnabled(boolean enable) {
        mEnabled = enable;
        refresh();
    }

    public void setAlpha(float alpha) {
        mAlpha = alpha;
        refresh();
    }

    public CharSequence getTitle() {
        return mTitleText;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void addOnSwitchListener(OnSwitchListener onSwitchListener) {
        mOnSwitchListeners.add(onSwitchListener);
    }

    public void clearOnSwitchListener() {
        mOnSwitchListeners.clear();
    }

    @Override
    protected void refresh() {
        super.refresh();
        if (mTitle != null) {
            if (mTitleText != null) {
                mTitle.setText(mTitleText);
                mTitle.setVisibility(View.VISIBLE);
            } else {
                mTitle.setVisibility(View.GONE);
            }
        }
        if (mSummary != null && mSummaryText != null) {
            mSummary.setText(mSummaryText);
        }
        if (mSummary != null && mSummaryOnText != null && mSummaryOffText != null){
            if (mChecked) {
                mSummary.setText(mSummaryOnText);
            }else {
                mSummary.setText(mSummaryOffText);
            }
        }
        if (mView != null) mView.setEnabled(mEnabled);
        if (mSwitcher != null) {
            mSwitcher.setChecked(mChecked);
            mSwitcher.setEnabled(mEnabled);
            mSwitcher.setAlpha(mAlpha);
        }
    }
}
