package com.examw.netschool;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.examw.netschool.app.AppContext;
import com.examw.netschool.app.Constant;
import com.examw.netschool.dao.LessonDao;
import com.examw.netschool.dao.PlayRecordDao;
import com.examw.netschool.model.Lesson;
import com.examw.netschool.model.PlayRecord;
import com.examw.netschool.util.DownloadFactory;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 播放器Activity基类。
 * Created by jeasonyoung on 16/3/21.
 */
public abstract class PlayActivity extends BaseActivity {
    private static final String TAG = "playActivity";
    private static final long TIMER_DELAY = 500;//定时器等候时间(毫秒)
    private static final long TIMER_PERIOD = 1000;//定时器间隔时间(毫秒)

    /**
     * 消息类型-播放进度。
     */
    protected static final int MSG_TYPE_PLAYPROGRESS = 0;

    private String lessonId,recordId,lessonName,lessonUrl;
    private long pos;
    private PopupWindow topBar,footerBar;
    private SurfaceView playView;
    private Timer timer;
    private int maxVolumnValue;
    private GestureDetector gestureDetector;

    //
    private static final ExecutorService pools = Executors.newSingleThreadExecutor();

    /**
     * 消息处理。
     */
    protected final UpdateUIHandler handler = new UpdateUIHandler(this);

    /**
     * 触摸事件处理。
     */
    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return gestureDetector != null && gestureDetector.onTouchEvent(event);
        }
    };

    /**
     * 获取播放器View。
     * @return 播放器View。
     */
    public SurfaceView getPlayView() {
        return playView;
    }

    /**
     * 重载Acitivity创建。
     * @param savedInstanceState
     * 保存实例状态。
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: 重载创建...");
        if(!this.preCreate(savedInstanceState)) return;
        //设置屏幕常亮(必须在setContentView之前)
        this.getWindow()
                .setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //获取最大音量值
        final AudioManager audioManager = this.getAudioManager();
        if(audioManager != null){
            this.maxVolumnValue = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        }
        //加载布局
        this.loadViewLayout();
        //加载数据传递
        this.loadIntentData();
        //手势处理
        this.gestureDetector = new GestureDetector(this,new GestureListener());
        this.gestureDetector.setIsLongpressEnabled(true);
    }

    /**
     * 创建之前调用。
     * @param savedInstanceState
     * 保存实例状态。
     */
    protected abstract boolean preCreate(Bundle savedInstanceState);

    /**
     * 加载布局。
     */
    protected void loadViewLayout(){
        //加载播放器View
        this.playView = this.loadVideoView();
        if(this.playView != null){
            //设置触摸事件监听
            this.playView.setOnTouchListener(this.onTouchListener);
            //设置启用长按响应
            this.playView.setLongClickable(true);
            //设置焦点启用
            this.playView.setFocusable(true);
            //设置点击启用
            this.playView.setClickable(true);
        }
        //初始化布局器
        final LayoutInflater inflater = LayoutInflater.from(this);
        //加载顶部控制栏
        this.topBar = this.loadTopControlBar(inflater);
        //加载底部控制栏
        this.footerBar = this.loadFooterControlBar(inflater);
        //加载Popup处理
        this.loadPopupWindows();
    }

    /**
     * 加载播放器View。
     * @return 播放器基类。
     */
    protected abstract SurfaceView loadVideoView();

    /**
     * 加载顶部控制栏。
     * @param inflater
     * 布局器。
     * @return 顶部控制栏。
     */
    protected abstract PopupWindow loadTopControlBar(final LayoutInflater inflater);

    /**
     * 加载底部控制栏。
     * @param inflater
     * 布局器。
     * @return 底部控制栏。
     */
    protected abstract PopupWindow loadFooterControlBar(final LayoutInflater inflater);

    /**
     * 加载Popup栏目
     */
    private void loadPopupWindows(){
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                if(playView != null && playView.isShown()) {
                    Log.d(TAG, "queueIdle: 显示控制栏目...");
                    //显示顶部控制栏
                    showTopBar();
                    //显示顶部控制栏目
                    showFooterBar();
                }
                return false;
            }
        });
    }

    /**
     * 显示顶部控制栏。
     */
    protected void showTopBar(){
        if(this.topBar != null && this.playView != null){
            Log.d(TAG, "showTopBar: 显示顶部控制栏...");
            this.topBar.showAtLocation(this.playView, Gravity.TOP, 0, 0);
            this.topBar.update();
        }
    }

    /**
     * 隐藏顶部控制栏。
     */
    protected void hideTopBar(){
        if(this.topBar != null && this.topBar.isShowing()){
            Log.d(TAG, "hideTopBar: ");
            this.topBar.dismiss();
        }
    }

    /**
     * 显示底部控制栏。
     */
    protected void showFooterBar(){
        if(this.footerBar != null && this.playView != null){
            Log.d(TAG, "showFooterBar: 显示底部控制栏...");
            this.footerBar.showAtLocation(this.playView, Gravity.BOTTOM, 0, 0);
            this.footerBar.update();
        }
    }

    /**
     * 隐藏底部控制栏.
     */
    protected void hideFooterBar(){
        if(this.footerBar != null && this.footerBar.isShowing()){
            Log.d(TAG, "hideFooterBar: ");
            this.footerBar.dismiss();
        }
    }

    /**
     * 加载意图数据。
     */
    private void loadIntentData(){
        //加载传递的数据
        final Intent intent = this.getIntent();
        if(intent != null){
            //课程ID
            this.lessonId = intent.getStringExtra(Constant.CONST_LESSON_ID);
            //课程标题
            this.lessonName = intent.getStringExtra(Constant.CONST_LESSON_NAME);
            //课程播放url
            this.lessonUrl = intent.getStringExtra(Constant.CONST_LESSON_PLAY_URL);
            //播放记录ID
            this.recordId = intent.getStringExtra(Constant.CONST_LESSON_RECORD_ID);
        }
    }

    /**
     * 启动播放。
     */
    @Override
    protected void onStart() {
        super.onStart();
        //异步处理数据
        new AsyncTask<Void,Void,Object>(){
            /**
             * 后台线程执行。
             * @param params
             * 参数列表。
             * @return 反馈结果。
             */
            @Override
            protected Object doInBackground(Void... params) {
                try{
                    Log.d(TAG, "doInBackground: 后台数据查询播放数据...");
                    //设置播放位置
                    pos = 0;
                    //初始化播放记录数据操作
                    final PlayRecordDao recordDao = new PlayRecordDao();
                    //如果播放记录存在
                    if(!StringUtils.isBlank(recordId)){
                        Log.d(TAG, "doInBackground: 根据播放记录加载数据=>" + recordId);
                        final PlayRecord record = recordDao.getPlayRecord(recordId);
                        if(record != null){
                            lessonId = record.getLessonId();
                            pos = record.getPlayTime() * 1000;
                        }
                    }
                    //如果课程ID不存在
                    if(StringUtils.isBlank(lessonId)){
                        Log.w(TAG, "doInBackground: 播放课程ID为空!");
                        return "课程ID为空!";
                    }
                    //加载课程资源数据
                    final LessonDao lessonDao = new LessonDao();
                    final Lesson lesson = lessonDao.getLesson(lessonId);
                    if(lesson == null){
                        if(StringUtils.isNotBlank(lessonUrl)){
                            return Uri.parse(lessonUrl);
                        }
                        Log.w(TAG, "doInBackground: 课程资源不存在["+lessonId+"]!");
                        return "课程资源不存在!";
                    }
                    //课程名称
                    lessonName = lesson.getName();
                    //课程URL地址
                    final String url = lesson.getPriorityUrl();
                    //检查是否已下载
                    final File path = DownloadFactory.getInstance()
                            .loadDownloadFilePath(new DownloadFactory.DownloadItemConfig(lessonId, lessonName, url));
                    if(path != null && path.exists()){
                        Log.d(TAG, "doInBackground: 本地播放地址=>" + path);
                        return Uri.parse(path.getAbsolutePath());
                    }
                    //网络播放
                    if(StringUtils.isBlank(url)){
                        Log.w(TAG, "doInBackground: 播放地址为空!" + url);
                        return "课程播放地址为空!";
                    }
                    //检查网络
                    final AppContext appContext = (AppContext)getApplicationContext();
                    if(appContext != null && !appContext.isNetworkConnected()){
                        Log.w(TAG, "doInBackground: 设备未连接网络!");
                        return "未连接网络!";
                    }
                    //网络播放
                    return Uri.parse(url);
                }catch (Exception e){
                    Log.e(TAG, "doInBackground: 加载播放数据异常=>" + e, e);
                    return e.getMessage();
                }
            }

            /**
             * 前台主线程处理。
             * @param o
             * 参数对象。
             */
            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if(o instanceof Uri){//播放
                    play(lessonName, pos, (Uri)o);
                }else{//消息弹出
                    new AlertDialog.Builder(PlayActivity.this)
                            .setTitle("提示")
                            .setMessage(o.toString())
                            .setCancelable(false).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.d(TAG, "onClick: 关闭播放器!");
                                    //关闭播放器
                                    finish();
                                }
                            }).show();
                }
            }
        }.execute();
    }

    /**
     * 播放。
     * @param name
     * 课程名称。
     * @param pos
     * 起始播放位置。
     * @param uri
     * 播放Uri.
     */
    protected abstract void play(final String name, final long pos, final Uri uri);

    /**
     * 播放。
     */
    protected void play(){
        try {
            Log.d(TAG, "play: 播放视频=>" + lessonName);
            if (this.getPlayTime() >= this.getPlayTotalTime()) {
                //重置播放位置
                this.setPlaySeek(0);
            }
            if (this.timer == null) {//定时器
                //初始化定时器。
                this.timer = new Timer();
                //定时任务
                this.timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if(handler != null) handler.sendEmptyMessage(MSG_TYPE_PLAYPROGRESS);
                    }
                }, TIMER_DELAY, TIMER_PERIOD);
            }
        }catch (Exception e){
            Log.e(TAG, "play: 播放异常:" + e, e);
        }
    }

    /**
     * 设置播放位置。
     * @param pos
     * 播放位置(毫秒)
     */
    protected abstract void setPlaySeek(long pos);

    /**
     * 获取播放总时长(毫秒).
     * @return 播放总时长(毫秒).
     */
    protected abstract long getPlayTotalTime();

    /**
     * 获取当前播放位置。
     * @return 播放位置。
     */
    protected abstract long getPlayTime();

    /**
     * 获取是否播放中。
     */
    protected abstract boolean IsPlay();

    /**
     * 暂停播放。
     */
    protected abstract void pausePlay();

    /**
     * 返回处理。
     */
    protected void back(){
        //取消播放进度更新
        this.endPlayUpdate();
        //隐藏顶部栏
        this.hideTopBar();
        //隐藏底部栏
        this.hideFooterBar();
        //暂停播放
        this.pausePlay();
        //关闭
        this.finish();
    }

    /**
     * 更新播放记录.
     * @param pos
     * 播放位置。
     */
    protected void updatePlayRecord(final long pos){
        if(pos < 1000 || StringUtils.isBlank(recordId)) return;
        pools.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "run: 更新播放[" + lessonName + "](" + recordId + ")记录(秒)＝>" + (pos / 1000));
                    //初始化播放记录数据操作
                    final PlayRecordDao playRecordDao = new PlayRecordDao();
                    //更新数据
                    playRecordDao.updatePlayTime(recordId, (int) (pos / 1000));
                } catch (Exception e) {
                    Log.e(TAG, "run: 更新播放记录异常=>" + e, e);
                }
            }
        });
    }

    /**
     * 结束播放进度更新。
     */
    protected void endPlayUpdate(){
        try{
            Log.d(TAG, "endPlayUpdate: 结束播放进度定时器...");
            if(this.timer != null){
                this.timer.cancel();
                this.timer = null;
            }
        }catch (Exception e){
            Log.e(TAG, "endPlayUpdate: 结束定时器异常=>" + e, e);
        }
    }

    /**
     * 按键事件处理。
     * @param keyCode
     * 按键代码。
     * @param event
     * 事件。
     * @return 处理结果。
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch(keyCode){
            case KeyEvent.KEYCODE_VOLUME_UP://音量增大
            case KeyEvent.KEYCODE_VOLUME_DOWN://音量减小
            {
                final int currentVolume = getVolumnCurrentValue();
                final int newVolume = (keyCode == KeyEvent.KEYCODE_VOLUME_UP) ? currentVolume + 1 : currentVolume - 1;
                if(newVolume >= 0){
                    //重置音量
                    setVolumnValue(newVolume);
                    //显示音量
                    if(this.topBar != null && this.topBar.isShowing()){
                        //更新音量值
                        updateVolumnValue(newVolume);
                    }
                }
            }
            case KeyEvent.KEYCODE_BACK:{//返回
                return super.onKeyDown(keyCode, event);
            }
            default: break;
        }
        return true;
    }

    /**
     * 获取音量管理器。
     * @return 音量管理器。
     */
    protected AudioManager getAudioManager(){
        final AppContext appContext = (AppContext)this.getApplicationContext();
        if(appContext != null){
           return appContext.getAudioManager();
        }
        return null;
    }

    /**
     * 获取最大音量值。
     * @return 最大音量。
     */
    protected int getVolumnMaxValue(){
        return this.maxVolumnValue;
    }

    /**
     * 获取当前音量值。
     * @return 当前音量值。
     */
    protected int getVolumnCurrentValue(){
        final AudioManager audioManager = this.getAudioManager();
        if(audioManager != null){
            return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return 0;
    }

    /**
     * 设置音量值。
     * @param value
     * 音量值。
     */
    protected void setVolumnValue(final int value){
        final AudioManager audioManager = this.getAudioManager();
        if(audioManager != null && value > 0){
            if(value > this.getVolumnMaxValue()) return;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, value, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        }
    }

    /**
     * 重载恢复处理。
     */
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
        this.play();
    }

    /**
     * 重载暂停处理。
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
        this.pausePlay();
    }

    /**
     * 重载停止处理.
     */
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop: ");
        //取消进度更新
        this.endPlayUpdate();
        //暂停播放
        this.pausePlay();
        //调用父函数
        super.onStop();
    }

    /**
     * 更新音量值。
     * @param volumnValue
     * 音量值。
     */
    protected abstract void updateVolumnValue(final int volumnValue);

    /**
     * 消息处理UI。
     * @param type
     * 类型。
     * @param activity
     * activity
     */
    protected abstract void updateHandler(final int type, final PlayActivity activity);

    /**
     * 更新进度消息处理器。
     */
    protected static class UpdateUIHandler extends Handler{
        private final WeakReference<PlayActivity> activityRef;

        /**
         * 构造函数。
         * @param activity
         * Activity.
         */
        public UpdateUIHandler(PlayActivity activity){
            this.activityRef = new WeakReference<>(activity);
        }

        /**
         * 消息处理。
         * @param msg
         * 消息数据。
         */
        @Override
        public void handleMessage(Message msg) {
            try{
                final PlayActivity activity;
                if((activity = this.activityRef.get()) != null){
                    activity.updateHandler(msg.what, activity);
                }
                super.handleMessage(msg);
            }catch (Exception e){
                Log.e(TAG, "handleMessage: 更新进度消息处理异常=>" + e, e);
            }
        }
    }


    /**
     * 手势监听处理类。
     */
    private class GestureListener implements GestureDetector.OnGestureListener{
        @Override
        public boolean onDown(MotionEvent e) {
            //轻触屏幕，控制栏出现或者消失
            Log.d(TAG, "onDown: 轻触屏幕，控制条出现或者消失...");
            //顶部控制栏
            if(topBar != null){
                if(topBar.isShowing())//显示=>隐藏
                    hideTopBar();
                else//隐藏=>显示
                    showTopBar();
            }
            //底部控制栏
            if(footerBar != null){
                if(footerBar.isShowing())//显示=>隐藏
                    hideFooterBar();
                else//隐藏=>显示
                    showFooterBar();
            }
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }
    }
}
