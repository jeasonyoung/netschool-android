package com.examw.netschool;

import android.media.AudioManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.examw.netschool.util.Utils;

/**
 * 播放器基础抽象类。
 * Created by jeasonyoung on 16/3/21.
 */
public abstract class BasePlayActivity extends PlayActivity {
    private static final String TAG = "basePlay";
    private TextView tvTitle, tvVolSize, tvPlayTime, tvTotalTime;
    private SeekBar volBar, playBar;

    /**
     * 播放步进幅度。
     */
    public static final int PLAY_STEP_SPEED = 5000;

    /**
     * 播放速度。
     */
    public static final float PLAY_SPEED = 1.0f;


    @Override
    protected PopupWindow loadTopControlBar(LayoutInflater inflater) {
        Log.d(TAG, "loadTopControlBar:");
        final View topView = inflater.inflate(R.layout.play_volumn_control, null);
        if (topView != null) {
            //设置背景透明
            topView.getBackground().setAlpha(0);
            //返回按钮
            final View btnReturn = topView.findViewById(R.id.btn_return);
            if (btnReturn != null) {
                btnReturn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: 返回...");
                        back();
                    }
                });
            }
            //获取标题
            this.tvTitle = (TextView) topView.findViewById(R.id.txt_title);
            //音量拖拽条
            this.volBar = (SeekBar) topView.findViewById(R.id.seek_bar_vol);
            //音量百分比
            this.tvVolSize = (TextView) topView.findViewById(R.id.tv_vol_size);
            //设置音量数据
            if (this.volBar != null) {
                final int maxVol = this.getVolumnMaxValue(), currentVol = this.getVolumnCurrentValue();
                //设置音量拖拽条最大值
                this.volBar.setMax(maxVol);
                //设置音量控制栏
                this.setVolumnControlBar(currentVol);
                //设置音量控制器
                this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
                //设置拖拽事件
                this.volBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        setVolumnControlBar(seekBar.getProgress());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                });
            }
            //初始化顶部浮动控制栏
            final PopupWindow topBar = new PopupWindow(topView,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            topBar.setAnimationStyle(R.style.AnimationFade);
            return topBar;
        }
        return null;
    }

    /**
     * 设置标题。
     *
     * @param title 标题。
     */
    protected void setTitle(String title) {
        Log.d(TAG, "setTitle: " + title);
        if (this.tvTitle != null) {
            this.tvTitle.setText(title);
        }
    }

    /**
     * 设置音量条。
     *
     * @param value 音量值。
     */
    private void setVolumnControlBar(int value) {
        if (value < 0) value = 0;
        //设置音量进度条
        if (this.volBar != null) {
            this.volBar.setProgress(value);
        }
        //设置音量百分比
        if (this.tvVolSize != null) {
            final int max = this.getVolumnMaxValue();
            this.tvVolSize.setText(String.format("%d%%", value * 100 / max));
        }
    }

    @Override
    protected PopupWindow loadFooterControlBar(LayoutInflater inflater) {
        Log.d(TAG, "loadFooterControlBar: ...");
        final View footerView = inflater.inflate(R.layout.play_video_control, null);
        if (footerView != null) {
            //回退
            final View btnPrev = footerView.findViewById(R.id.btn_previous);
            if (btnPrev != null) {
                btnPrev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: 回退...");
                        if (getPlayView() == null) return;
                        final long pos = getPlayTime() - PLAY_STEP_SPEED;
                        if (pos >= 0) {
                            //设置播放位置
                            setPlaySeek(pos);
                            //通知进度更新
                            if (handler != null) {
                                handler.sendEmptyMessage(MSG_TYPE_PLAYPROGRESS);
                            }
                        }
                    }
                });
            }
            //播放
            final ImageButton btnPlay = (ImageButton) footerView.findViewById(R.id.btn_play);
            if (btnPlay != null) {
                btnPlay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: 播放/暂停...");
                        if (getPlayView() == null) return;
                        int bgResId, imgResId;
                        if (IsPlay()) {//播放=>暂停
                            //资源
                            bgResId = R.drawable.play_video_video_control_play_bg;
                            imgResId = R.drawable.play_video_video_control_play_icon;
                            //暂停
                            pausePlay();
                            Log.d(TAG, "onClick: 暂停播放...");
                        } else {//暂停=>播放
                            //资源
                            bgResId = R.drawable.play_video_video_control_pause_bg;
                            imgResId = R.drawable.play_video_video_control_pause_icon;
                            //播放
                            play();
                        }
                        //重置图片按钮资源
                        final ImageButton btn = (ImageButton) v;
                        if (btn != null) {
                            //设置背景
                            btn.setBackgroundResource(bgResId);
                            //设置图片
                            btn.setImageResource(imgResId);
                        }
                    }
                });
            }
            //前进
            final View btnNext = footerView.findViewById(R.id.btn_next);
            if (btnNext != null) {
                btnNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "onClick: 前进...");
                        if (getPlayView() == null) return;
                        long pos = getPlayTime() + PLAY_STEP_SPEED;
                        if (pos <= getPlayTotalTime()) {
                            //设置播放位置
                            setPlaySeek(pos);
                            //通知进度更新
                            if (handler != null) {
                                handler.sendEmptyMessage(MSG_TYPE_PLAYPROGRESS);
                            }
                        }
                    }
                });
            }
            //播放时间
            this.tvPlayTime = (TextView) footerView.findViewById(R.id.tv_play_time);
            //播放进度条
            this.playBar = (SeekBar) footerView.findViewById(R.id.seek_bar_progress);
            if (this.playBar != null) {
                //设置进度条监听
                this.playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    long pos = 0;

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        try {
                            if (getPlayView() == null) return;
                            this.pos = progress * getPlayTotalTime() / seekBar.getMax();
                        } catch (Exception e) {
                            Log.e(TAG, "onProgressChanged: 滑动播放进度条异常=>" + e, e);
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        //设置播放位置
                        setPlaySeek(pos);
                        //设置播放时间显示
                        setPlayTime(pos);
                    }
                });
            }
            //总时长
            this.tvTotalTime = (TextView) footerView.findViewById(R.id.tv_total_time);

            //底部控制栏
            final PopupWindow footerBar = new PopupWindow(footerView,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            footerBar.setAnimationStyle(R.style.AnimationFade);
            return footerBar;
        }
        return null;
    }

    /**
     * 设置播放时间。
     *
     * @param time 当前播放时间。
     */
    protected void setPlayTime(long time) {
        if (time >= 0 && this.tvPlayTime != null) {
            this.tvPlayTime.setText(Utils.getTime(time / 1000));
        }
    }

    /**
     * 设置播放总时长。
     *
     * @param total 总时长。
     */
    protected void setPlayTotalTime(long total) {
        if (total >= 0 && this.tvTotalTime != null) {
            this.tvTotalTime.setText(Utils.getTime(total / 1000));
        }
    }

    /**
     * 设置播放进度。
     * @param pos
     * 播放位置。
     */
    protected void setPlayProgress(long pos) {
        if (pos > 0) {
            //设置播放时间
            this.setPlayTime(pos);
            //设置播放进度条
            final long total = this.getPlayTotalTime();
            if (this.playBar != null && total > 0) {
                this.playBar.setProgress((int) ((this.playBar.getMax() * pos) / total));
            }
        }
    }

    /**
     * 更新缓冲进度。
     * @param percent
     * 进度数。
     */
    protected void updateBufferedProgress(final int percent){
        if(this.playBar != null){
            this.playBar.setSecondaryProgress(percent);
        }
    }

    /**
     * 更新音量值。
     *
     * @param volumnValue 音量值。
     */
    @Override
    protected void updateVolumnValue(int volumnValue) {
        Log.d(TAG, "updateVolumnValue: " + volumnValue);
        this.setVolumnControlBar(volumnValue);
    }

    /**
     * 更新事件处理。
     *
     * @param type     类型。
     * @param activity activity弱应用.
     */
    @Override
    protected void updateHandler(int type, PlayActivity activity) {
        if (activity instanceof BasePlayActivity) {
            if (type == MSG_TYPE_PLAYPROGRESS) {
                final long pos = activity.getPlayTime();
                if (pos > 0) {
                    //设置播放进度
                    ((BasePlayActivity) activity).setPlayProgress(pos);
                }
            }
        }
    }
}
