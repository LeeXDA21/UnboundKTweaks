package com.thunder.thundertweaks.custom_views.layout;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.LinearLayoutCompat;

public class ForcedScrollableLinearLayout extends LinearLayoutCompat {
    public ForcedScrollableLinearLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public final boolean canScrollVertically(int i) {
        return true;
    }
}
