//-----------------------------------------------------------------------------
//
// (C) Brandon Valosek, 2011 <bvalosek@gmail.com>
// (C) Willi Ye, 2015 <williye97@gmail.com>
//
//-----------------------------------------------------------------------------
// Modified by Willi Ye to work with big.LITTLE
package com.bvalosek.cpuspy

import android.content.Context
import com.bvalosek.cpuspy.CpuStateMonitor
import com.thunder.thundertweaks.utils.AppSettings
import android.util.SparseArray
import com.thunder.thundertweaks.utils.Utils
import java.lang.StringBuilder

/**
 * main application class
 */
class CpuSpyApp(private val mCore: Int, private val mContext: Context, private val mGpu: String?) {
    /**
     * @return the internal CpuStateMonitor object
     */
    /**
     * the long-living object used to monitor the system frequency states
     */
    var cpuStateMonitor: CpuStateMonitor? = null

    /**
     * Load the saved string of offsets from preferences and put it into the
     * state monitor
     */
    private fun loadOffsets() {
        if (mGpu != null || mCore == -1) {
            val prefs = AppSettings.getCpuSpyOffsets(-1, mContext)
            if (prefs.isEmpty()) return
            // split the string by peroids and then the info by commas and load
            val offsets = SparseArray<Long>()
            val sOffsets = prefs.split(",").toTypedArray()
            for (offset in sOffsets) {
                val parts = offset.split(" ").toTypedArray()
                offsets.put(
                    Utils.strToInt(parts[0]), Utils.strToLong(
                        parts[1]
                    )
                )
            }
            cpuStateMonitor!!.offsets = offsets
        } else {
            val prefs = AppSettings.getCpuSpyOffsets(mCore, mContext)
            if (prefs.isEmpty()) return
            // split the string by peroids and then the info by commas and load
            val offsets = SparseArray<Long>()
            val sOffsets = prefs.split(",").toTypedArray()
            for (offset in sOffsets) {
                val parts = offset.split(" ").toTypedArray()
                offsets.put(
                    Utils.strToInt(parts[0]), Utils.strToLong(
                        parts[1]
                    )
                )
            }
            cpuStateMonitor!!.offsets = offsets
        }
    }

    /**
     * Save the state-time offsets as a string e.g. "100 24, 200 251, 500 124
     * etc
     */
    fun saveOffsets() {
        // build the string by iterating over the freq->duration map
        val str = StringBuilder()
        val offsets = cpuStateMonitor!!.offsets
        for (i in 0 until offsets.size()) {
            str.append(offsets.keyAt(i)).append(" ").append(offsets.valueAt(i)).append(",")
        }
        if (mGpu != null || mCore == -1) {
            AppSettings.saveCpuSpyOffsets(str.toString(), -1, mContext)
        } else {
            AppSettings.saveCpuSpyOffsets(str.toString(), mCore, mContext)
        }
    }

    init {
        if (mGpu != null || mCore == -1) {
            cpuStateMonitor = CpuStateMonitor(-1, mGpu)
        } else {
            cpuStateMonitor = CpuStateMonitor(mCore, null)
        }
        loadOffsets()
    }
}