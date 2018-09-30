package com.example.v_fanlulin.childrenreaderrobotdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.v_fanlulin.childrenreaderrobotdemo.utils.CameraUtil;

import java.io.IOException;
import java.util.List;


/**
 * 
 */
public class CameraSurfacePreview extends SurfaceView implements
        SurfaceHolder.Callback, PreviewCallback, ShutterCallback,
        Camera.AutoFocusCallback {
    
    private final static String TAG = "CameraSurfacePreview";
    
    private SurfaceHolder mHolder;
    private Camera mCamera;

    /** 是否打开前置相机,true为前置,false为后置 */
    private boolean mIsFrontCamera;
    /** 当前屏幕旋转角度 */
    public static int mOrientation = 0;
    public static int mSavePicOrientation = 0;

    private CamrepreiewCallback camrepreiewCallback;

    private Parameters mParameters;
    private static final int IMAGE_FORMAT = ImageFormat.NV21;

    private Context mContext;

    public int surfaceViewH = 0;

    public boolean isAutoFocus = false;

    public int mCurPicFormat = ImageFormat.JPEG;

    public CameraSurfacePreview(Context context) {
        super(context);
        mIsFrontCamera = false;
        mContext = context;

        this.mHolder = getHolder();
        this.mHolder.addCallback(this);

        this.mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public CameraSurfacePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsFrontCamera = false;
        mContext = context;

        this.mHolder = getHolder();
        this.mHolder.addCallback(this);

        this.mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    Paint paint = new Paint();
    {
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.5f);// 设置线宽
        paint.setAlpha(100);
    };

    public Parameters getParameters(){
        if(mParameters==null){
            mParameters = mCamera.getParameters();
        }
        return mParameters;
    }

    /**
     * 
     */
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            // Open the Camera in preview mode
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            mCamera.setPreviewDisplay(holder);
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            mCamera.setPreviewCallback(this);

            setCameraParameters();
            mCamera.startPreview();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        Log.d("Dennis", "surfaceChanged() is called");

        try {
            surfaceViewH = getHeight();

            mCamera.startPreview();
            mCamera.cancelAutoFocus();
            mCamera.autoFocus(this);
            updateCameraOrientation();
        } catch (Exception e) {
           e.printStackTrace();
        }
    }

    /**
     * 
     * @return
     */
    public int getSurfaceViewHeight() {
        return surfaceViewH;
    }

    /**
	 * 
	 */
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            try {
                mCamera.cancelAutoFocus(); // Reset the focus.
            } catch (Exception e) {
            }
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

    }

    public void takePicture( PictureCallback imageCallback) {
        if (mCamera != null) {
            mCamera.takePicture(this, null, imageCallback);
        }
    }

    /**
     * 根据当前朝向修改保存图片的旋转角度
     */
    private void updateCameraOrientation() {
        if (mCamera != null) {
            // rotation参数为 0、90、180、270。水平方向为0。
            int rotation = 90 + mOrientation == 360 ? 0 : 90 + mOrientation;
            // 前置摄像头需要对垂直方向做变换，否则照片是颠倒的
            if (mIsFrontCamera) {
                if (rotation == 90) {
                    rotation = 270;
                } else if (rotation == 270) {
                    rotation = 90;
                }
            }

            mSavePicOrientation = rotation;
            mParameters.setRotation(rotation);

            // 预览图片旋转90°
            mCamera.setDisplayOrientation(90);

            Size optimalSize = calcPreviewSize(mParameters);
            if (optimalSize != null) {
                mParameters.setPreviewSize(optimalSize.width,   optimalSize.height);
            }

            mCamera.setParameters(mParameters);
        }
    }

    /**
     * 
     * @param p
     * @return
     */
    private Size calcPreviewSize(Parameters p) {
        List<Size> list = p.getSupportedPreviewSizes();
        Size size = CameraUtil.getOptimalPreviewSize((Activity) mContext, list,
                Constant.ASPECT_RATIO);
        return size;
    }

    /**
     * 设置照相机参数
     */
    private void setCameraParameters() {
        mParameters = mCamera.getParameters();
        // 设置图片格式
        mParameters.setPreviewFormat(IMAGE_FORMAT); // setting preview
        mCurPicFormat = mParameters.getPictureFormat();

        mParameters.setPictureFormat(ImageFormat.JPEG);

        mParameters.setJpegQuality(100);
        mParameters.setJpegThumbnailQuality(100);

        // 自动聚焦模式
        mParameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        // 开启屏幕朝向监听
        startOrientationChangeListener();
        updateCameraOrientation();


    }

/**
 * 
 */
private void startOrientationChangeListener() {
    OrientationEventListener mOrEventListener = new OrientationEventListener(
            getContext()) {
        @Override
        public void onOrientationChanged(int rotation) {

            if (((rotation >= 0) && (rotation <= 45)) || (rotation > 315)) {
                rotation = 0;
            } else if ((rotation > 45) && (rotation <= 135)) {
                rotation = 90;
            } else if ((rotation > 135) && (rotation <= 225)) {
                rotation = 180;
            } else if ((rotation > 225) && (rotation <= 315)) {
                rotation = 270;
            } else {
                rotation = 0;
            }
            if (rotation == mOrientation)
                return;
            mOrientation = rotation;
        }
    };
    mOrEventListener.enable();
}

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

        Size size = mCamera.getParameters().getPreviewSize();
        camrepreiewCallback.getcameraFrame(data, size.width, size.height);

    }

    public void setPreviewFrame(CamrepreiewCallback camrepreiewCallback) {

        this.camrepreiewCallback = camrepreiewCallback;
    }

    @Override
    public void onShutter() {

    }

    @Override
    public void onAutoFocus(boolean success, Camera camera) {
        if (success) {
            isAutoFocus = true;
            mCamera.cancelAutoFocus();
        } else {
            isAutoFocus = false;
        }
    }

    /**
	 * 
	 */
    public void stopPreview() {
        if (mCamera != null) {
            try {
                mCamera.cancelAutoFocus(); // Reset the focus.
            } catch (Exception e) {
            }
            mCamera.setPreviewCallback(null);

            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
	 * 
	 */
    public void startPreview() {
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mHolder);
                mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                mCamera.setPreviewCallback(this);
                mCamera.startPreview();
                setCameraParameters();
            } catch (IOException e) {

                e.printStackTrace();
            }
        }
    }

}