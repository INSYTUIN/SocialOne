package com.example.socialonetwo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;

public class UIUtils {

    @SuppressLint("ClickableViewAccessibility")
    public static void setClickAnimation(Context context, View view) {
        if (view == null || context == null) return;
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_down));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.scale_up));
            }
            return false;
        });
    }

    public static int dpToPx(Context context, int dp) {
        if (context == null) return dp;
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
