package com.leo.camerroll.camera;

import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * Created by leo on 17/1/22.
 */

public class CameraRollManager extends GuardedAsyncTask{
    private  static Context mContext;
    private final Uri mUri;
    private static Handler handler=new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(mContext,"保存成功！",Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            mContext.startActivity(intent);
        }
    };
    public CameraRollManager(Context context, Uri uri) {
        super(context);
        mContext = context;
        mUri = uri;
    }

    @Override
    protected void doInBackgroundGuarded(Object[] params) {
        File source = new File(mUri.getPath());
        FileChannel input = null, output = null;
        try {
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            exportDir.mkdirs();
            if (!exportDir.isDirectory()) {
                return;
            }
            File dest = new File(exportDir, source.getName());
            int n = 0;
            String fullSourceName = source.getName();
            String sourceName, sourceExt;
            if (fullSourceName.indexOf('.') >= 0) {
                sourceName = fullSourceName.substring(0, fullSourceName.lastIndexOf('.'));
                sourceExt = fullSourceName.substring(fullSourceName.lastIndexOf('.'));
            } else {
                sourceName = fullSourceName;
                sourceExt = "";
            }
            while (!dest.createNewFile()) {
                dest = new File(exportDir, sourceName + "_" + (n++) + sourceExt);
            }
            input = new FileInputStream(source).getChannel();
            output = new FileOutputStream(dest).getChannel();
            output.transferFrom(input, 0, input.size());
            input.close();
            output.close();

            MediaScannerConnection.scanFile(
                    mContext,
                    new String[]{dest.getAbsolutePath()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            handler.sendEmptyMessage(0);
                        }
                    });
        } catch (IOException e) {
        } finally {
            if (input != null && input.isOpen()) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
            if (output != null && output.isOpen()) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
