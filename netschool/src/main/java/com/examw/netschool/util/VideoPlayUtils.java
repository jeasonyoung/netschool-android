package com.examw.netschool.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.examw.netschool.app.AppContext;

import org.apache.commons.lang3.StringUtils;

/**
 * 播放器工具。
 * Created by jeasonyoung on 16/3/31.
 */
public final class VideoPlayUtils {
    private static final String TAG = "VideoPlayUtils";
    private static final String VIDEO_PLAY_TYPE = "video_play_type";
    private static final String DEFAULT_VIDEO_PLAY = "PrimaryPlayActivity";

    private static String video_play_activity_class;

    /**
     * 获取视频播放器Activity类型名称。
     * @return 类型名称。
     */
    private static String getVideoPlayActivityClass(){
        if(video_play_activity_class == null){
            Log.d(TAG, "getVideoPlayActivityClass: 获取播放器Activity类型名称...");
            synchronized (VideoPlayUtils.class){
                final Context context = AppContext.getContext();
                if(context != null){
                    try {
                        Log.d(TAG, "getVideoPlayActivityClass: 获取播放器Activity类型设置...");
                        ApplicationInfo appInfo = context.getPackageManager()
                                .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                        if(appInfo != null){
                            String classType = appInfo.metaData.getString(VIDEO_PLAY_TYPE);
                            if(StringUtils.isBlank(classType))
                                classType = DEFAULT_VIDEO_PLAY;
                            Log.d(TAG, "getVideoPlayActivityClass: classType=>" + classType);
                            video_play_activity_class = classType;
                        }
                    }catch (Exception e){
                        Log.e(TAG, "getInstance: 创建播放器实例异常:" + e, e);
                    }
                }

            }
        }
        return video_play_activity_class;
    }

    /**
     * 创建播放器Activity意图。
     * @return 播放器意图.
     */
    public static Intent createVideoPlayIntent(final Activity activity){
        Log.d(TAG, "createVideoPlayIntent: ...");
        final Intent intent = new Intent();
        intent.setClassName(activity, "com.examw.netschool." + getVideoPlayActivityClass());
        return intent;
    }

}