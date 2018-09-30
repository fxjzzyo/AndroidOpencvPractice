package com.example.v_fanlulin.childrenreaderrobotdemo;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.v_fanlulin.childrenreaderrobotdemo.database.MySQLiteOpenHelper;
import com.example.v_fanlulin.childrenreaderrobotdemo.utils.LogUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements CamrepreiewCallback {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    private static final String TAG = "MainActivity";

    //用byte数组检测相似度
    public native double judgeImgByte(byte[] buf, int w, int h, int angle);

    //得出图片指纹信息 byte[]
    public native String getImgFingerprint(byte[] buf, int w, int h, int angle);

    //得出图片指纹信息 int[]
    public native String getImgFingerprintInt(int[] buf, int w, int h, int angle);


    private TextView tvContent;
    private FrameLayout flCameraPreview;
    private CameraSurfacePreview mCameraSurPreview = null;

    private MySQLiteOpenHelper mySQLiteOpenHelper;
    private SQLiteDatabase mSqLiteDatabase;
    public static final String DB_NAME = "children_reader_database";
    public static final String TABLE_NAME = "data_records";

    //测试图片
    private int[] pictures = new int[]{
            R.mipmap.a1, R.mipmap.a2, R.mipmap.a3, R.mipmap.a4, R.mipmap.a5,
            R.mipmap.a6, R.mipmap.a7, R.mipmap.a8, R.mipmap.a9, R.mipmap.a10
    };
    //测试图片对应内容
    private String[] contents = {
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10"
    };

    private Timer mTimer;

    private byte[] pictureBytes;//记录每一张截图的bytes
    private int w, h;
    private int mContent = 0;//模拟内容

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //初始化数据库
        initDatabase();

        //准备测试数据的指纹信息，存入数据库
//        prepareTestData();


    }

    /**
     * 准备测试数据的指纹信息，存入数据库
     */
    private void prepareTestData() {
        for (int i = 0; i < 3; i++) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), pictures[i],options);
            //获取图片指纹
            String imgFingerprint = getImgInfo(bitmap);
            if(bitmap != null && !bitmap.isRecycled()){
                // 回收并且置为null
                bitmap.recycle();
                bitmap = null;
            }
            System.gc();


            //将图片指纹添加到数据库
            addFingerprintToDB(imgFingerprint, contents[i]);

        }

    }

    /**
     * 创建数据库
     */
    private void initDatabase() {
        //创建数据库
        mySQLiteOpenHelper = new MySQLiteOpenHelper(this, DB_NAME);
        //获得可写的数据库
        mSqLiteDatabase = mySQLiteOpenHelper.getWritableDatabase();
    }

    /**
     * 获取一张图片的指纹信息
     * @param bitmap
     * @return
     */
    private String getImgInfo(Bitmap bitmap) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        int[] buf = new int[w * h];
        bitmap.getPixels(buf, 0, w, 0, 0, w, h);

        //获取图片信息
        long start = System.currentTimeMillis();
        String imgFingerprint = getImgFingerprintInt(buf, w, h, 0);
        long end = System.currentTimeMillis();
        Log.i("tag", "time :----------->" + (end - start) + "ms.");

        LogUtil.logV(TAG, "finger print is---->" + imgFingerprint);
        if(bitmap != null && !bitmap.isRecycled()){
            // 回收并且置为null
            bitmap.recycle();
            bitmap = null;
        }
        System.gc();

        return imgFingerprint;

    }

    private void addFingerprintToDB(String imgFingerprint, String content) {
        if (mSqLiteDatabase != null) {

            ContentValues values1 = new ContentValues();
            values1.put("picture_fingerprint", imgFingerprint);
            values1.put("content", content);

            mSqLiteDatabase.insert(TABLE_NAME, null, values1);

        }


    }

    private void initView() {
        tvContent = findViewById(R.id.tv_content);
        flCameraPreview = findViewById(R.id.fl_camera_preview);

    }


    /**
     * 点击拍照按钮
     *
     * @param view
     */
    public void takePhoto(View view) {
        //获取拍照的权限
        requestCameraPermission();
    }


    //获取拍照的权限
    private void requestCameraPermission() {
//        判断手机版本,如果低于6.0 则不用申请权限,直接拍照
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//6.0及以上
            Log.i("tag", "手机版本高于6.0，需要申请权限");
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.i("tag", "没有权限");
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    Log.i("tag", "上次点击了禁止，但没有勾选不再询问");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                } else {
                    Log.i("tag", "第一次启动，或者，上次点击了禁止，并勾选不再询问");
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            } else {
                startTake();
            }
        } else {
            Log.i("tag", "手机是6.0以下的，不需要权限");
            startTake();
        }

    }

    //权限申请的回调
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    if (i == 0) {
                        Log.i("tag", "申请权限成功");
                        startTake();
                    }
                } else {
                    Log.i("tag", "" + "权限" + permissions[i] + "申请失败");
                    Toast.makeText(this, "" + "权限" + permissions[i] + "申请失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    /**
     * 开始拍照
     */
    private void startTake() {
        mCameraSurPreview = new CameraSurfacePreview(this);

        flCameraPreview.addView(mCameraSurPreview);

        mCameraSurPreview.setPreviewFrame(MainActivity.this);
        mCameraSurPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_GPU);
    }

    @Override
    public void getcameraFrame(final byte[] data, final int width, final int height) {
        LogUtil.logV(TAG, "width--->" + width + " height------>" + height);
        //记录到全局
        pictureBytes = data;
        w = width;
        h = height;

        if (mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //开始匹配
                    String content = matchPicture(pictureBytes, width, height);
                    Log.i("tag", "content---->" + content);
                }
            }, 0, 3000);
        }


    }

    public String matchPicture(byte[] data, final int width, final int height) {
        //byte[]转bitmap
        YuvImage yuv = new YuvImage(data, mCameraSurPreview.getParameters().getPreviewFormat(), width, height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 100, out);
        final byte[] bytes = out.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 4;
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length,options);

        //bitmap转int[]
        int w=bitmap.getWidth(),h=bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        //获得截图的指纹
        String imgFingerprint = getImgFingerprintInt(pix, w, h, 0);

        //获取数据库中所有图片指纹
        Map<String,String> picFingerprints = getAllFingerprint();

        double maxSimilarity = 0;//记录相似度最大的值
        String matchKey = null; //记录相似度最大的指纹

        //与数据库中的数据比较
        for (String key : picFingerprints.keySet()) {
            //比较相似度
            double similarity = getSimilarity(imgFingerprint, key);

            if(similarity>maxSimilarity){
                maxSimilarity = similarity;
                matchKey = key;
            }

            Log.i("tag","target--->"+imgFingerprint+" data pic----->"+key+" similarity--->"+similarity);

        }

        if(maxSimilarity!=0){
            return picFingerprints.get(matchKey);
        }else {
            return "没有找到匹配内容";
        }

    }

    /**
     * 比较两张图片指纹的相似度
     * @param imgFingerprint
     * @param key
     * @return
     */
    private double getSimilarity(String imgFingerprint, String key) {
        double similarity = 0;
        int diff = 0;//记录不相同的位数
        for(int i =0;i<imgFingerprint.length();i++){
            if(imgFingerprint.charAt(i) == key.charAt(i)){
                similarity+=1.0/imgFingerprint.length();
                if(similarity>=0.1){
                    return similarity;
                }else {
                    return 0;
                }
            }else {
                diff+=1.0/imgFingerprint.length();
                if(diff>0.9){//如果相异度大于0.6就认为不相似
                    return 0;
                }
            }
        }
        return similarity;
    }

    private Map<String, String> getAllFingerprint() {

        String fingerPrint = null;
        String content= null;
        Map<String,String> one = new HashMap<>();

        if (mSqLiteDatabase != null) {
            Cursor cursor = mSqLiteDatabase.query(TABLE_NAME, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                fingerPrint = cursor.getString(cursor.getColumnIndex("picture_fingerprint"));
                content = cursor.getString(cursor.getColumnIndex("content"));
                one.put(fingerPrint, content);
            }
            cursor.close();
        }

        return one;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.start_take:
                //获取拍照的权限
                requestCameraPermission();
                break;

            case R.id.take_one:
                takeOnePhoto();
                break;
            default:
                    break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 数据采集，每按一下，采集一张样本图片
     */
    private void takeOnePhoto() {
        if (pictureBytes != null) {
            //提取图片指纹特征
            String imgFingerprint = getImgFingerprint(pictureBytes, w, h, 0);
            //将特征存入数据库
            addFingerprintToDB(imgFingerprint, ++mContent+"");
            Log.i("tag", "采集一张图片指纹到数据库");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSqLiteDatabase != null && mSqLiteDatabase.isOpen()) {
            mSqLiteDatabase.close();
        }

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
}
