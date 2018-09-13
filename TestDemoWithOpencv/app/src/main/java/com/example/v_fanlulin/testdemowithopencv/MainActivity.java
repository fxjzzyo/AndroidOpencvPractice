package com.example.v_fanlulin.testdemowithopencv;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native int[] imgToGray(int[] buf,int w,int h);

    private ImageView mImageView;
    private String mPublicPhotoPath;
    private static final int REQ_GALLERY = 33;
    private static final int REQUEST_CODE_PICK_IMAGE = 22;
    private int mWidth;
    private int mHeight;
    private Uri uri;
    private String path;
    private Bitmap bitmapBack;//用于备份的bitmap

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        // Example of a call to a native method
//        TextView tv = (TextView) findViewById(R.id.sample_text);
//        tv.setText(stringFromJNI());
    }
    private void initView() {
        mImageView =  (ImageView) findViewById(R.id.iv);

    }


    /**
     * 点击拍照按钮
     * @param view
     */
    public void takePhoto(View view) {
        //获取拍照的权限
        showTakePicture();

    }

    /**
     * 点击选择图片按钮
     * @param view
     */
    public void selectPhoto(View view) {
        //获取相册中的照片
        getImageFromAlbum();

    }


    //获取拍照的权限
    private void showTakePicture() {
//        判断手机版本,如果低于6.0 则不用申请权限,直接拍照
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {//7.0及以上
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            }else{
                startTake();
            }
        }else{
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
                    if (i == 0){
                        startTake();
                    }
                } else {
                    Toast.makeText(this, "" + "权限" + permissions[i] + "申请失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void startTake() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //判断是否有相机应用
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            //创建临时图片文件
            File photoFile = null;
            try {
                photoFile = createPublicImageFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //设置Action为拍照
            if (photoFile != null) {
                takePictureIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                //这里加入flag
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Uri photoURI;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//7.0及以上
                    photoURI = FileProvider.getUriForFile(this, "com.example.v_fanlulin.testdemowithopencv", photoFile);
                }else{
                    photoURI = Uri.fromFile(photoFile);
                }
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQ_GALLERY);

            }
        }
        //将照片添加到相册中
        galleryAddPic(mPublicPhotoPath, this);
    }

    /**
     * 获取相册中的图片
     */
    public void getImageFromAlbum() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);
    }
    /**
     * 创建临时图片文件
     * @return
     * @throws IOException
     */
    private File createPublicImageFile() throws IOException {
        File path = null;
        if (hasSdcard()) {
            path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM);
        }
        Date date = new Date();
        String timeStamp = getTime(date, "yyyyMMdd_HHmmss", Locale.CHINA);
        String imageFileName = "Camera/" + "IMG_" + timeStamp + ".jpg";
        File image = new File(path, imageFileName);
        mPublicPhotoPath = image.getAbsolutePath();
        return image;
    }
    /**
     * 判断sdcard是否被挂载
     * @return
     */
    public static boolean hasSdcard() {
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 获取时间的方法
     * @param date
     * @param mode
     * @param locale
     * @return
     */
    private String getTime(Date date, String mode, Locale locale) {
        SimpleDateFormat format = new SimpleDateFormat(mode, locale);
        return format.format(date);
    }
    /**
     * 将照片添加到相册中
     */
    public static void galleryAddPic(String mPublicPhotoPath, Context context) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mPublicPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * 拍照之后获取结果的方法
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //获取mImageView的宽高
        mWidth = mImageView.getWidth();
        mHeight = mImageView.getHeight();
        switch (requestCode) {
            //拍照
            case REQ_GALLERY:
                if (resultCode != Activity.RESULT_OK) return;
                uri = Uri.parse(mPublicPhotoPath);
                path = uri.getPath();
                break;
            //获取相册的图片
            case REQUEST_CODE_PICK_IMAGE:
                if (data == null) return;
                uri = data.getData();
                int sdkVersion = Integer.valueOf(Build.VERSION.SDK);
                if (sdkVersion >= 19) {  // 或者 android.os.Build.VERSION_CODES.KITKAT这个常量的值是19
                    path = this.uri.getPath();//5.0直接返回的是图片路径 Uri.getPath is ：  /document/image:46 ，5.0以下是一个和数据库有关的索引值
                    // path_above19:/storage/emulated/0/girl.jpg 这里才是获取的图片的真实路径
                    path =getPath_above19(this, this.uri);
                } else {
                    path = getFilePath_below19(this, this.uri);
                }
                break;
        }
        bitmapBack = getSmallBitmap(path, mWidth, mHeight);
        mImageView.setImageBitmap(bitmapBack);


    }

    /**
     * 获取小于api19时获取相册中图片真正的uri
     * @param context
     * @param uri
     * @return
     */
    public static String getFilePath_below19(Context context,Uri uri) {
        //这里开始的第二部分，获取图片的路径：低版本的是没问题的，但是sdk>19会获取不到
        String[] proj = {MediaStore.Images.Media.DATA};
        //好像是android多媒体数据库的封装接口，具体的看Android文档
        Cursor cursor = context.getContentResolver().query(uri, proj, null, null, null);
        //获得用户选择的图片的索引值
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        //将光标移至开头 ，这个很重要，不小心很容易引起越界
        cursor.moveToFirst();
        //最后根据索引值获取图片路径   结果类似：/mnt/sdcard/DCIM/Camera/IMG_20180724_032212.jpg
        String path = cursor.getString(column_index);
        return path;
    }

    /**
     * 获取大于api19时获取相册中图片真正的uri
     * @param context
     * @param uri
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public  static String getPath_above19(final Context context, final Uri uri) {
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
    /**
     * 图片的压缩
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height
                    / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }
    /**
     * 根据路径获得突破并压缩返回bitmap用于显示
     * @param filePath
     * @return
     */
    public static Bitmap getSmallBitmap(String filePath, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        //只返回图片的大小信息
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    /**
     * 恢复为原图
     * @param view
     */
    public void converToOrig(View view) {
        //直接从备份的bitmapBack拿图片
        if(bitmapBack==null){
            bitmapBack = BitmapFactory.decodeResource(getResources(), R.mipmap.genie);

        }
        mImageView.setImageBitmap(bitmapBack);

    }

    /**
     * 转换为灰度图
     * @param view
     */
    public void converToGray(View view) {
        //从imageview上获取bitmap图片
        mImageView.setDrawingCacheEnabled(true);
        Bitmap bitmap = mImageView.getDrawingCache();

        int w=bitmap.getWidth(),h=bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        //把彩色像素转为灰度像素
        int[] resultInt=imgToGray(pix, w, h);
        Bitmap resultImg=Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        resultImg.setPixels(resultInt, 0, w, 0, 0,w, h);
        mImageView.setDrawingCacheEnabled(false);
        mImageView.setImageBitmap(resultImg);
    }
    /**
     * 转换为反相图
//     * @param view
     */
//    public void convert(View view) {
//        //从imageview上获取bitmap图片
//        mImageView.setDrawingCacheEnabled(true);
//        Bitmap bitmap = mImageView.getDrawingCache();
//
//        int w=bitmap.getWidth(),h=bitmap.getHeight();
//        int[] pix = new int[w * h];
//        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
//        //把彩色像素转为灰度像素
//        int[] resultInt=getImgConvert(pix, w, h);
//        Bitmap resultImg=Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
//        resultImg.setPixels(resultInt, 0, w, 0, 0,w, h);
//        mImageView.setDrawingCacheEnabled(false);
//        mImageView.setImageBitmap(resultImg);
//    }
    private int[] bitmapToArray(int resID)
    {
        Bitmap bitmap=((BitmapDrawable) getResources().getDrawable(resID)).getBitmap();
        int w=bitmap.getWidth(),h=bitmap.getHeight();
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);
        return pix;
    }

}
