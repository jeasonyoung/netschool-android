package com.examw.netschool;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.Vitamio;
import io.vov.vitamio.widget.VideoView;

/**
 * 主视频播放器(vitamio)。
 * Created by jeasonyoung on 16/3/21.
 */
public class PrimaryPlayActivity extends BasePlayActivity {
    private static final String TAG = "primaryPlay";
    private View loading;
    private long playTotal = 0,firstPlayPosition = 0;

    @Override
    protected boolean preCreate(Bundle savedInstanceState) {
        //检查vitamio播放器依赖
        if(!LibsChecker.checkVitamioLibs(this)){
            Log.d(TAG, "preCreate: vitamio解压解码器..");
            return false;
        }
        return true;
    }

    @Override
    protected void loadViewLayout() {
        Log.d(TAG, "loadViewLayout: 加载布局文件...");
        //加载布局文件
        this.setContentView(R.layout.activity_primary_play);
        //加载父函数
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
        return (VideoView)this.findViewById(R.id.play_video_view);
    }

    @Override
    protected boolean IsPlay() {
        Log.d(TAG, "IsPlay: ...");
        final VideoView video = (VideoView)this.getPlayView();
        return (video != null) && video.isPlaying();
    }

    @Override
    protected long getPlayTotalTime() {
        Log.d(TAG, "getPlayTotalTime: ..." + this.playTotal);
        return this.playTotal;
    }

    @Override
    protected long getPlayTime() {
        final VideoView video = (VideoView)this.getPlayView();
        if(video != null){
            return video.getCurrentPosition();
        }
        return 0;
    }

    @Override
    protected synchronized void setPlaySeek(long pos) {
        Log.d(TAG, "setPlaySeek: " + pos);
        final VideoView video = (VideoView)this.getPlayView();
        if(video != null){
            video.seekTo(pos);
        }
    }

    @Override
    protected void play(String name, long pos, Uri uri) {
        try{
            Log.d(TAG, "play: [name=>" + name + ",pos=>" + pos + ",uri=>" + uri + "]");
            //设置标题
            this.setTitle(name);
            //设置播放位置
            this.firstPlayPosition = pos;
            //设置播放器
            final VideoView video = (VideoView)this.getPlayView();
            if(video != null && uri != null){
                //设置播放缓冲事件监听
                video.setOnBufferingUpdateListener(this.onBufferingUpdateListener);
                //设置播放前事件监听
                video.setOnPreparedListener(this.onPreparedListener);
                //设置播放完成事件监听
                video.setOnCompletionListener(this.onCompletionListener);
                //设置播放地址
                video.setVideoURI(uri);
                //设置请求开始
                video.requestFocus();
            }
        }catch (Exception e){
            Log.e(TAG, "play: 异常=>" + e, e);
        }
    }

    /**
     * 视频缓冲事件处理。
     */
    private MediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mp, int percent) {
            //更新缓冲进度。
            updateBufferedProgress(percent);
        }
    };

    /**
     * 视频播放前事件处理。
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
                //设置播放速度
                mp.setPlaybackSpeed(PLAY_SPEED);
                //设置播放位置
                mp.seekTo(firstPlayPosition);
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
    protected void play() {
        try {
            Log.d(TAG, "play: 开始播放...");
            //调用父函数
            super.play();
            //调用播放器播放
            final VideoView video = (VideoView) this.getPlayView();
            if (video != null) {
                video.start();
            }
        }catch (Exception e){
            Log.e(TAG, "play: 播放异常=>" + e, e);
        }
    }

    @Override
    protected void pausePlay() {
        Log.d(TAG, "pausePlay: 暂停播放...");
        final VideoView video = (VideoView)this.getPlayView();
        if(video != null){
            //暂停播放
            video.pause();
            //更新播放记录
            this.updatePlayRecord(this.getPlayTime());
        }
    }
}