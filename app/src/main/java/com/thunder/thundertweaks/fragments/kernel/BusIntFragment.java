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
package com.thunder.thundertweaks.fragments.kernel;

import com.unbound.UnboundKtweaks.R;
import com.thunder.thundertweaks.fragments.ApplyOnBootFragment;
import com.thunder.thundertweaks.fragments.recyclerview.RecyclerViewFragment;
import com.thunder.thundertweaks.utils.AppSettings;
import com.thunder.thundertweaks.utils.Utils;
import com.thunder.thundertweaks.utils.kernel.bus.VoltageInt;
import com.thunder.thundertweaks.views.recyclerview.CardView;
import com.thunder.thundertweaks.views.recyclerview.DescriptionView;
import com.thunder.thundertweaks.views.recyclerview.RecyclerViewItem;
import com.thunder.thundertweaks.views.recyclerview.SeekBarView;
import com.thunder.thundertweaks.views.recyclerview.SwitchView;
import com.thunder.thundertweaks.views.recyclerview.TitleView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by morogoku on 06.11.18.
 */
public class BusIntFragment extends RecyclerViewFragment {

    private List<SeekBarView> mVoltages = new ArrayList<>();
    private SeekBarView mSeekbarProf = new SeekBarView();

    private static float mVoltMinValue = -100000f;
    private static float mVoltMaxValue = 25000f;
    private static int mVoltStep = 6250;
    public static int mDefZeroPosition = (Math.round(mVoltMaxValue - mVoltMinValue) / mVoltStep) - (Math.round(mVoltMaxValue) / mVoltStep);

    @Override
    protected void init() {
        super.init();

        addViewPagerFragment(ApplyOnBootFragment.newInstance(this));
    }

    @Override
    protected void addItems(List<RecyclerViewItem> items) {
        mVoltages.clear();

        final List<String> freqs = VoltageInt.getFreqs();
        final List<String> voltages = VoltageInt.getVoltages();
        final List<String> voltagesStock = VoltageInt.getStockVoltages();

        if (freqs != null && voltages != null && voltagesStock != null && freqs.size() == voltages.size()) {

            CardView freqCard = new CardView(getActivity());
            freqCard.setTitle(getString(R.string.busInt_volt_title));

            DescriptionView busInt = new DescriptionView();
            busInt.setSummary(getString(R.string.busInt_volt_summary));
            freqCard.addItem(busInt);

            List<String> progress = new ArrayList<>();
            for (float i = mVoltMinValue; i < (mVoltMaxValue + mVoltStep); i += mVoltStep) {
                String global = String.valueOf(i / VoltageInt.getOffset());
                progress.add(global);
            }

            seekbarProfInit(mSeekbarProf, freqs, voltages, voltagesStock, progress);

            freqCard.addItem(mSeekbarProf);

            Boolean enableGlobal = AppSettings.getBoolean("busInt_global_volts", true, getActivity());

            SwitchView voltControl = new SwitchView();
            voltControl.setTitle(getString(R.string.cpu_manual_volt));
            voltControl.setSummaryOn(getString(R.string.cpu_manual_volt_summaryOn));
            voltControl.setSummaryOff(getString(R.string.cpu_manual_volt_summaryOff));
            voltControl.setChecked(enableGlobal);
            voltControl.addOnSwitchListener((switchView, isChecked) -> {
                if (isChecked) {
                    AppSettings.saveBoolean("busInt_global_volts", true, getActivity());
                    AppSettings.saveBoolean("busInt_individual_volts", false, getActivity());
                    reload();
                } else {
                    AppSettings.saveBoolean("busInt_global_volts", false, getActivity());
                    AppSettings.saveBoolean("busInt_individual_volts", true, getActivity());
                    AppSettings.saveInt("busInt_seekbarPref_value", mDefZeroPosition, getActivity());
                    reload();
                }
            });

            freqCard.addItem(voltControl);

            if (freqCard.size() > 0) {
                items.add(freqCard);
            }

            TitleView tunables = new TitleView();
            tunables.setText(getString(R.string.voltages));
            items.add(tunables);

            for (int i = 0; i < freqs.size(); i++) {
                SeekBarView seekbar = new SeekBarView();
                seekbarInit(seekbar, freqs.get(i), voltages.get(i), voltagesStock.get(i));
                mVoltages.add(seekbar);
            }
        }
        items.addAll(mVoltages);
    }

    private void seekbarProfInit(SeekBarView seekbar, final List<String> freqs, final List<String> voltages,
                                 final List<String> voltagesStock, List<String> progress) {

        Boolean enableSeekbar = AppSettings.getBoolean("busInt_global_volts", true, getActivity());
        int global = AppSettings.getInt("busInt_seekbarPref_value", mDefZeroPosition, getActivity());

        int value = 0;
        for (int i = 0; i < progress.size(); i++) {
            if (i == global){
                value = i;
                break;
            }
        }

        seekbar.setTitle(getString(R.string.cpu_volt_profile));
        seekbar.setSummary(getString(R.string.cpu_volt_profile_summary));
        seekbar.setUnit(getString(R.string.mv));
        seekbar.setItems(progress);
        seekbar.setProgress(value);
        seekbar.setEnabled(enableSeekbar);
        if(!enableSeekbar) seekbar.setAlpha(0.4f);
        else seekbar.setAlpha(1f);
        seekbar.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {
            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                for (int i = 0; i < voltages.size(); i++) {
                    String volt = String.valueOf(Utils.strToFloat(voltagesStock.get(i)) + Utils.strToFloat(value));
                    String freq = freqs.get(i);
                    VoltageInt.setVoltage(freq, volt, getActivity());
                    AppSettings.saveInt("busInt_seekbarPref_value", position, getActivity());
                }
                getHandler().postDelayed(BusIntFragment.this::reload, 200);
            }
            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });
    }

    private void seekbarInit(SeekBarView seekbar, final String freq, String voltage,
                             String voltageStock) {

        int mOffset = VoltageInt.getOffset();
        float mMin = (Utils.strToFloat(voltageStock) - (- mVoltMinValue / 1000)) * mOffset;
        float mMax = ((Utils.strToFloat(voltageStock) + (mVoltMaxValue / 1000)) * mOffset) + mVoltStep;

        List<String> progress = new ArrayList<>();
        for(float i = mMin ; i < mMax; i += mVoltStep){
            String string = String.valueOf(i / mOffset);
            progress.add(string);
        }

        int value = 0;
        for (int i = 0; i < progress.size(); i++) {
           if (Objects.equals(progress.get(i), voltage)){
                value = i;
                break;
           }
        }

        Boolean enableSeekbar = AppSettings.getBoolean("busInt_individual_volts", false, getActivity());

        seekbar.setTitle(freq + " " + getString(R.string.mhz));
        seekbar.setSummary(getString(R.string.def) + ": " + voltageStock + " " + getString(R.string.mv));
        seekbar.setUnit(getString(R.string.mv));
        seekbar.setItems(progress);
        seekbar.setProgress(value);
        seekbar.setEnabled(enableSeekbar);
        if(!enableSeekbar) seekbar.setAlpha(0.4f);
        else seekbar.setAlpha(1f);
        seekbar.setOnSeekBarListener(new SeekBarView.OnSeekBarListener() {

            @Override
            public void onStop(SeekBarView seekBarView, int position, String value) {
                VoltageInt.setVoltage(freq, value, getActivity());
                getHandler().postDelayed(() -> reload(), 200);
            }

            @Override
            public void onMove(SeekBarView seekBarView, int position, String value) {
            }
        });
    }

    private void reload() {
        List<String> freqs = VoltageInt.getFreqs();
        List<String> voltages = VoltageInt.getVoltages();
        List<String> voltagesStock = VoltageInt.getStockVoltages();

        if (freqs != null && voltages != null && voltagesStock != null) {
            for (int i = 0; i < mVoltages.size(); i++) {
                seekbarInit(mVoltages.get(i), freqs.get(i), voltages.get(i), voltagesStock.get(i));
            }
            List<String> progress = new ArrayList<>();
            for (float i = mVoltMinValue; i < (mVoltMaxValue + mVoltStep); i += mVoltStep) {
                String global = String.valueOf(i / VoltageInt.getOffset());
                progress.add(global);
            }
            seekbarProfInit(mSeekbarProf, freqs, voltages, voltagesStock, progress);
        }
    }
}
