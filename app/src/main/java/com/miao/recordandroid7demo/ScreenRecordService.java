package com.miao.recordandroid7demo;

import android.app.Service;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class ScreenRecordService extends Service {

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    private boolean running;
    private int width = 720;
    private int height = 1080;
    private int dpi;

    private String videoPath = "";
    private final String tag = "ScreenRecordService";

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread serviceThread = new HandlerThread("service_thread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        serviceThread.start();
        running = false;
    }

    public class ScreenRecordBinder extends Binder {
        public ScreenRecordService getScreenRecordService(){
            return ScreenRecordService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ScreenRecordBinder();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void setMediaProject(MediaProjection project) {
        mediaProjection = project;
    }

    public boolean isRunning() {
        return running;
    }

    public void setConfig(int width, int height, int dpi) {
        this.width = width;
        this.height = height;
        this.dpi = dpi;
    }

    public void startRecord() {
        if (mediaProjection == null || running) {
            return;
        }
        initRecorder();
        createVirtualDisplay();
        try {
            mediaRecorder.start();
            running = true;
        }catch (IllegalStateException e){
            e.printStackTrace();
            Toast.makeText(this,"",Toast.LENGTH_SHORT).show();
            Log.e(tag, "startRecord failed" + e.getMessage());
            running = false;
        }
    }

    public boolean stopRecord() {
        if (!running) {
            return false;
        }
        running = false;
        try{
            mediaRecorder.stop();
            mediaRecorder.reset();
            virtualDisplay.release();
        }catch (Exception e){
            e.printStackTrace();
            Log.e(tag, "stopRecord failed" + e.getMessage());
            return false;
        }
        Log.d(tag, "stopRecord success | videoPath: " + videoPath);
        return true;
    }

    private void createVirtualDisplay() {
        try{
            virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", width, height, dpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
        }catch (Exception e){
            Log.e(tag, "createVirtualDisplay failed" + e.getMessage());
        }
    }

    private void initRecorder() {
        mediaRecorder = new MediaRecorder();
        //设置声音来源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置视频来源
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //设置视频格式
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //设置视频储存地址
        videoPath = getSaveDirectory() + System.currentTimeMillis() + ".mp4";
        Log.d(tag, "videoPath: " + videoPath);
        mediaRecorder.setOutputFile(videoPath);
        //设置视频大小
        mediaRecorder.setVideoSize(width, height);
        //设置视频编码
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置声音编码
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        //视频码率
        mediaRecorder.setVideoEncodingBitRate(2 * 1920 * 1080);
        mediaRecorder.setVideoFrameRate(18);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,"prepare出错，录屏失败！",Toast.LENGTH_SHORT).show();
        }
    }

    public String getSaveDirectory() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            String rootDir = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/" + "ScreenRecord" + "/";
            File file = new File(rootDir);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    return null;
                }
            }
            return rootDir;
        } else {
            return null;
        }
    }

}
