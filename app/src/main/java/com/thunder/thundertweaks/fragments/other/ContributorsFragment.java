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
package com.thunder.thundertweaks.fragments.other;

import com.google.android.material.snackbar.Snackbar;

import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.fragments.recyclerview.RecyclerViewFragment;
import com.thunder.thundertweaks.utils.WebpageReader;
import com.thunder.thundertweaks.utils.other.Contributors;
import com.thunder.thundertweaks.views.recyclerview.ContributorView;
import com.thunder.thundertweaks.views.recyclerview.RecyclerViewItem;

import java.util.List;

/**
 * Created by willi on 23.07.16.
 */
public class ContributorsFragment extends RecyclerViewFragment {

    private WebpageReader mWebpageReader;
    private Snackbar mErrorBar;

    @Override
    public int getSpanCount() {
        return super.getSpanCount() + 1;
    }

    @Override
    protected boolean showViewPager() {
        return false;
    }

    @Override
    public void onViewFinished() {
        super.onViewFinished();
        if (mErrorBar != null) {
            error();
        }
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
    }

    @Override
    protected void postInit() {
        super.postInit();

        if (mWebpageReader == null) {
            showProgress();
            mWebpageReader = new WebpageReader(getActivity(), new WebpageReader.WebpageListener() {
                @Override
                public void onSuccess(String url, String raw, CharSequence html) {
                    if (!isAdded()) return;
                    hideProgress();
                    Contributors contributors = new Contributors(raw);
                    if (contributors.readable()) {
                        for (final Contributors.Contributor contributor : contributors.getContributors()) {
                            if (contributor.getLogin().equals("Grarak")) {
                                continue;
                            }
                            addItem(new ContributorView(contributor));
                        }
                    } else {
                        error();
                    }
                }

                @Override
                public void onFailure(String url) {
                    error();
                }
            });
            mWebpageReader.get("https://api.github.com/repos/Grarak/KernelAdiutor/contributors");
        }
    }

    private void error() {
        if (isAdded()) {
            hideProgress();
            mErrorBar = Snackbar.make(getRootView(), R.string.no_internet, Snackbar.LENGTH_INDEFINITE);
            mErrorBar.show();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWebpageReader != null) {
            mWebpageReader.cancel();
            mWebpageReader = null;
        }
        if (mErrorBar != null) {
            mErrorBar.dismiss();
            mErrorBar = null;
        }
    }
}
