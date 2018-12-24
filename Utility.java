package com.example.admin.keyproirityapp.conferenceCall;

/**
 * Created by Anil on 12/3/2018.
 */

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;

public class Utility {

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return dp * ((float) metrics.densityDpi /
                DisplayMetrics.DENSITY_DEFAULT);
    }
}
