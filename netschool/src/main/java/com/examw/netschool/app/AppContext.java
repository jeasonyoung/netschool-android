
package com.examw.netschool.app;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 全局应用程序类：用于保存和调用全局应用配置及访问网络数据
 * 
 * @version 1.0
 */
public class AppContext extends Application {
	private static final String TAG = "appContext";
	//全局上下文	
	private static Context mContext;
	//当前用户ID
	private static String currentyAgencyId,currentUserId,currentUsername;
	//连接管理
	private ConnectivityManager connectivityManager;
	//音频管理
	private AudioManager audioManager;
	//包信息
	private PackageInfo packageInfo;

	/**
	 * 多线程池。
	 */
	public static final ExecutorService pools_fixed = Executors.newFixedThreadPool(10);

	/*
	 * 重载应用创建。
	 * @see android.app.Application#onCreate()
	 */
	@Override
	public void onCreate() {
		Log.d(TAG, "重载应用创建...");
		super.onCreate();
		mContext = this;
	}

	//获取连接管理
	private ConnectivityManager getConnectivityManager(){
		if(this.connectivityManager == null){
			this.connectivityManager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
			Log.d(TAG, "从系统服务中加载连接管理器...");
		}
		return this.connectivityManager;
	}

	/**
	 * 获取音频管理。
	 * @return 音频管理。
	 */
	public AudioManager getAudioManager(){
		if(this.audioManager == null){
			this.audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
			Log.d(TAG, "从系统服务中加载音频管理器...");
		}
		return this.audioManager;
	}

	//获取包信息
	private PackageInfo getPackageInfo(){
		if(this.packageInfo == null){
			try {
				this.packageInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
				Log.d(TAG, "获取包信息...");
			} catch (NameNotFoundException e) {
				Log.e(TAG, "获取包信息异常:" + e.getMessage(),	 e);
			}
		}
		return this.packageInfo;
	}

	/**
	 * 获取应用全局上下文。
	 * @return 全局上下文对象。 
	 */
	public static Context getContext() {
		if (mContext == null) {
			Log.d(TAG, "获取应用全局上下文失败!");
			throw new RuntimeException("APPLICATION_CONTEXT_IS_NULL");
		}
		return mContext;
	}

	/**
	 *  获取当前机构ID。
	 * @return 机构ID。
	 */
	public static String getCurrentyAgencyId(){
		return currentyAgencyId;
	}

    /**
	 * 获取当前用户ID。
	 * @return 当前用户ID。
	 */
	public static String getCurrentUserId() {
		return currentUserId;
	}

    /**
	 * 获取当前用户名。
	 * @return 用户名。
	 */
	public static String getCurrentUsername(){
		return currentUsername;
	}

    /**
	 * 设置当前用户信息。
	 * @param agencyId
	 * 机构ID。
	 * @param userId
	 * 用户ID。
	 * @param userName
	 * 用户名。
	 */
	public synchronized static void setCurrentUserInfo(String agencyId, String userId, String userName) {
		Log.d(TAG, "设置当前用户信息:" + StringUtils.join(new String[]{agencyId, userId, userName}, "#"));
		//设置当前机构ID
		currentyAgencyId = agencyId;
		//设置当前用户ID
		currentUserId = userId;
		//设置当前用户名
		currentUsername = userName;
	}

	/**
	 * 获取当前网络类型。
	 * @return 0：没有网络 1：WIFI网络 2：WAP网络 3：NET网络
	 */
	public NetType getNetworkType() {
		Log.d(TAG, "获取当前网络类型...");
		NetType type = NetType.NONE;
		if(this.getConnectivityManager() == null) return type;
		final NetworkInfo networkInfo = this.getConnectivityManager().getActiveNetworkInfo();
		if (networkInfo == null) return type; 
		
		final int nType = networkInfo.getType();
		if (nType == ConnectivityManager.TYPE_MOBILE) {
			final String extraInfo = networkInfo.getExtraInfo();
			if (StringUtils.isNotBlank(extraInfo) && StringUtils.equalsIgnoreCase(extraInfo, "cmnet")) {
				type = NetType.CNNET;
			}else {
				type = NetType.CNWAP;
			}
		} else if (nType == ConnectivityManager.TYPE_WIFI) {
			type = NetType.WIFI;
		}
		return type;
	}

	/**
	 * 检测网络是否可用。
	 * @return 是否可用
	 */
	public boolean isNetworkConnected() {
		Log.d(TAG, "检测网络是否可用...");
		final NetType type = this.getNetworkType();
		if(type != NetType.NONE){
			final NetworkInfo ni = this.getConnectivityManager().getActiveNetworkInfo();
			return (ni != null && ni.isConnectedOrConnecting());
		}
		return false;
	}

	/**
	 * 获取当前应用版本代码。
	 * @return 版本代码。
	 */
	public int getVersionCode() {
		Log.d(TAG, "获取当前应用版本代码...");
		try {
			final PackageInfo info = this.getPackageInfo();
			if(info != null) return info.versionCode;
		} catch (Exception e) {
			Log.e(TAG, "发生异常:" + e.getMessage(), e);
		}
		return 0;
	}

	/**
	 * 网络类型。
	 *
	 * @author jeasonyoung
	 * @since 2015年9月2日
	 */
	public enum NetType {
		/**
		 * 无网络。
		 */
		NONE,
		/**
		 * WIFI.
		 */
		WIFI,
		/**
		 * CNWAP.
		 */
		CNWAP,
		/**
		 * CNNET.
		 */
		CNNET
	}
}