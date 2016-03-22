package com.examw.netschool;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

/**
 * 本地播放器(安卓原生播放器)。
 * Created by jeasonyoung on 16/3/22.
 */
public class NativePlayActivity extends BasePlayActivity implements SurfaceHolder.Callback {
    private static final String TAG = "nativePlay";
    private View loading;
    private MediaPlayer mediaPlayer;
    private SurfaceHolder surfaceHolder;
    private long playTotal = 0,firstPlayPosition = 0;

    @Override
    protected boolean preCreate(Bundle savedInstanceState) {
        return true;
    }

    private void gotoPrimaryPlay(){
        Log.d(TAG, "gotoPrimaryPlay: 使用Vitamio播放器...");
        //
        final Intent secondPlay = new Intent(this, PrimaryPlayActivity.class);
        secondPlay.putExtras(this.getIntent());
        secondPlay.setData(this.getIntent().getData());
        this.startActivity(secondPlay);
        //关闭本播放器
        this.finish();
    }
    
    @Override
    protected void loadViewLayout() {
        Log.d(TAG, "loadViewLayout: ...");
        //加载布局文件
        this.setContentView(R.layout.activity_native_play);
        //调用父函数
        super.loadViewLayout();
    }

    @Override
    protected SurfaceView loadVideoView() {
        Log.d(TAG, "loadVideoView: 加载播放器...");
        //加载进度条
        this.loading = this.findViewById(R.id.video_loading_view);
        if(this.loading != null){
            this.loading.setVisibility(View.VISIBLE);
        }
        //加载播放器
        final SurfaceView surfaceView = (SurfaceView)this.findViewById(R.id.play_video_view);
        if(surfaceView != null){
            //初始化播放器
            this.mediaPlayer = new MediaPlayer();
            this.surfaceHolder = surfaceView.getHolder();
            this.surfaceHolder.addCallback(this);
        }
        return surfaceView;
    }

    @Override
    protected void play(String name, long pos, Uri uri) {
        try{
            Log.d(TAG, "play: [name=>" + name + ",pos=>" + pos + ",uri=>" + uri + "]");
            //设置首次播放位置
            this.firstPlayPosition = pos;
            //设置播放缓存事件处理
            this.mediaPlayer.setOnBufferingUpdateListener(this.onBufferingUpdateListener);
            //设置播放前事件处理
            this.mediaPlayer.setOnPreparedListener(this.onPreparedListener);
            //设置播放完成事件处理
            this.mediaPlayer.setOnCompletionListener(this.onCompletionListener);
            if(uri != null) {
                //设置播放地址
                this.mediaPlayer.setDataSource(this, uri);
                //启动
                this.mediaPlayer.prepareAsync();
            }
        }catch (Exception e){
            Log.e(TAG, "play: 发生异常=>" + e, e);
            gotoPrimaryPlay();
        }
    }

    /**
     * 播放缓冲事件处理。
     */
    private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            //更新缓冲进度。
            updateBufferedProgress(percent);
        }
    };

    /**
     * 播放前事件处理。
     */
    private MediaPlayer.OnPreparedListener onPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            try {
                Log.d(TAG, "onPrepared: 播放前处理...");
                //获取播放总时长
                playTotal = mp.getDuration();
                //设置播放总时长
                setPlayTotalTime(playTotal);
                //设置播放位置
                mp.seekTo((int)firstPlayPosition);
                //设置播放进度
                setPlayProgress(firstPlayPosition);

                //隐藏正在加载
                if (loading != null) loading.setVisibility(View.GONE);

                //开始播放
                if (!mp.isPlaying()) {
                    Log.d(TAG, "onPrepared: 开始播放...");
                    mp.start();
                }
            }catch (Exception e){
                Log.e(TAG, "onPrepared: 播放前异常=>" + e, e);
                gotoPrimaryPlay();
            }
        }
    };

    /**
     * 播放完成事件处理。
     */
    private MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log.d(TAG, "onCompletion: 播放完成...");
            //取消更新播放进度
            endPlayUpdate();
            //暂停播放
            pausePlay();
            //显示顶部控制栏
            showTopBar();
            //显示底部控制栏
            showFooterBar();
        }
    };

    @Override
    protected void setPlaySeek(long pos) {
        Log.d(TAG, "setPlaySeek: " + pos);
        if(this.mediaPlayer != null){
            this.mediaPlayer.seekTo((int)pos);
        }
    }

    @Override
    protected long getPlayTotalTime() {
        Log.d(TAG, "getPlayTotalTime: " + this.playTotal);
        return this.playTotal;
    }

    @Override
    protected long getPlayTime() {
        Log.d(TAG, "getPlayTime: ...");
        if(this.mediaPlayer != null){
            return this.mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    protected boolean IsPlay() {
        Log.d(TAG, "IsPlay: ...");
        return this.mediaPlayer != null && this.mediaPlayer.isPlaying();
    }

    @Override
    protected void play() {
        try {
            Log.d(TAG, "play: 开始播放...");
            //调用父函数
            super.play();
            //调用播放器播放
            if (this.mediaPlayer != null) {
                this.mediaPlayer.start();
            }
        }catch (Exception e){
            Log.e(TAG, "play: 播放异常=>" + e, e);
        }
    }

    @Override
    protected void pausePlay() {
        Log.d(TAG, "pausePlay: ...");
        if(this.mediaPlayer != null){
            //暂停播放
            this.mediaPlayer.pause();
            //更新播放记录
            this.updatePlayRecord(this.mediaPlayer.getCurrentPosition());
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ....");
        if(this.mediaPlayer != null){
            this.mediaPlayer.release();
            this.mediaPlayer = null;
        }
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: ....");
        if(this.mediaPlayer != null && this.surfaceHolder != null){
            this.mediaPlayer.setDisplay(this.surfaceHolder);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}