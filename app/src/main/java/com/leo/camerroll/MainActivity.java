package com.leo.camerroll;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.leo.camerroll.camera.CameraRollManager;
import com.leo.camerroll.camera.LoaddingView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String IMAGE_URL = "http://b.hiphotos.baidu.com/image/h%3D200/sign=990764739ccad1c8cfbbfb274f3f67c4/5bafa40f4bfbfbed022d422371f0f736afc31f71.jpg";
    private ImageView mImageView;
    private LoaddingView mLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.id_image);
        mLoadingView = (LoaddingView) findViewById(R.id.id_loading);
        mImageView.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                if(mImageView.getDrawable() instanceof BitmapDrawable){
                    Toast.makeText(getApplicationContext(),"长按保存图片至相册",Toast.LENGTH_SHORT).show();
                    File fileDir=new File(getApplication().getExternalCacheDir(),"images");
                    File file=new File(fileDir.getAbsolutePath()+"/"+IMAGE_URL.hashCode()+".png");
                    if(file!=null&&file.length()>0){
                        CameraRollManager rollManager=new CameraRollManager(MainActivity.this, Uri.parse(file.getAbsolutePath()));
                        rollManager.execute();
                    }
                }
                return false;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestAlertWindowPermission();
            }
        }
        loadImage();
    }
    private static final int REQUEST_CODE = 1;
    private  void requestAlertWindowPermission() {
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE);
    }

    private void loadImage() {
        DownImageTask task = new DownImageTask(mImageView);
        task.execute(IMAGE_URL);
    }

    private class DownImageTask extends AsyncTask<String, Long, Bitmap> {
        private ImageView imageView;
        private long contentLength;
        public DownImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Bitmap bitmap = null;
            BufferedInputStream bis = null;
            ByteArrayOutputStream bos = null;
            try {
//                File fileDir=new File(getApplication().getExternalCacheDir(),"images");
//                if(fileDir==null||!fileDir.isDirectory()){
//                    fileDir.mkdir();
//                }
//                File file=new File(fileDir.getAbsolutePath()+"/"+params[0].hashCode()+".png");
//                if(file!=null&&file.length()>0){
//                    return bitmap=BitmapFactory.decodeFile(file.getAbsolutePath());
//                }
                bos=new ByteArrayOutputStream();
                byte[] buffer = new byte[512];
                long total=0;
                int len ;
                URL url = new URL(params[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                this.contentLength = conn.getContentLength();
                bis = new BufferedInputStream(conn.getInputStream());
                while ((len = bis.read(buffer)) != -1) {
                    SystemClock.sleep(200);
                    total+=len;
                    publishProgress(total);
                    bos.write(buffer, 0, len);
                    bos.flush();
                }
                bitmap= BitmapFactory.decodeByteArray(bos.toByteArray(),0,bos.toByteArray().length);
                saveBitmapToDisk(bos,params[0]);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (bis != null) {
                        bis.close();
                    }
                    if (bos != null) {
                        bos.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return bitmap;
        }

        private void saveBitmapToDisk(final ByteArrayOutputStream baos, final String url) {
            new Thread(){
                @Override
                public void run() {BufferedOutputStream bos=null;
                    try{
                        if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())){
                            Log.e("TAG","内存卡不存在");
                            return;
                        }
                        Log.e("TAG","开始保存图片至内存卡～～");
                        byte[] bytes = baos.toByteArray();
                        File fileDir=new File(getApplication().getExternalCacheDir(),"images");
                        if(fileDir==null||!fileDir.isDirectory()){
                            fileDir.mkdir();
                        }
                        File file=new File(fileDir.getAbsolutePath()+"/"+url.hashCode()+".png");
                        file.createNewFile();
                        bos=new BufferedOutputStream(new FileOutputStream(file));
                        bos.write(bytes);
                        bos.flush();
                        Log.e("TAG","图片已经保存至内存卡～～");
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        if(bos!=null){
                            try {
                                bos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }.start();
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            mLoadingView.setProgress(((values[0].longValue() * 1.0f / contentLength)));
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
                mLoadingView.loadComplete();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
        }
    }
}
