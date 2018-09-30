package com.example.v_fanlulin.childrenreaderrobotdemo.utils;

import android.app.Activity;
import android.hardware.Camera.Size;
import android.view.Display;

import java.util.List;


public class CameraUtil {
    
    private static final String TAG = "CameraUtil";
    
    /**
     * 
     */
    private CameraUtil() {
    }
    
    /**
     * 
     * @param currentActivity
     * @param sizes
     * @param targetRatio
     * @return
     */
    public static Size getOptimalPreviewSize(Activity currentActivity,
                                             List<Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.001;
        if (sizes == null)
            return null;
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        Display display = currentActivity.getWindowManager()
                .getDefaultDisplay();
        int targetHeight = Math.min(display.getHeight(), display.getWidth());
        if (targetHeight <= 0) {
            // We don't know the size of SurfaceView, use screen height
            targetHeight = display.getHeight();
        }
        
        if (targetHeight > 720) {
            targetHeight = 720;
        }
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            LogUtil.logE(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
}
