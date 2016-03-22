package com.examw.netschool.app;
/**
 * APP 常量。
 * 
 * @author jeasonyoung
 * @since 2015年9月2日
 */
public final class Constant {
	/**
	 * 终端类型(2-苹果,3-安卓,4-其他)。
	 */
	public static final int TERMINAL = 3;
	/**
	 * 班级ID/班级名称。
	 */
	public static final String CONST_CLASS_ID = "class_id", CONST_CLASS_NAME = "class_name";
	/**
	 * 课程资源ID/课程资源名称。
	 */
	public static final String CONST_LESSON_ID = "lesson_id", CONST_LESSON_NAME = "lesson_name";
	/**
	 * 播放记录ID。
	 */
	public static final String CONST_LESSON_RECORD_ID = "lesson_record_id";
	
	public static final String CONST_LESSON_PLAY_URL = "lesson_play_url";

	/**
	 * 导航文件配置。
	 */
	public static final String PREFERENCES_CONFIG_GUIDEFILE = "guidefile";
	/**
	 * 导航文件配置-是否是第一次。
	 */
	public static final String PREFERENCES_CONFIG_GUIDEFILE_ISFIRST = "isfirst_";

    /**
     * 用户存储配置。
     */
    public static final String PREFERENCES_CONFIG_USER = "user";
	
	/**
	 * 用户密码存储配置。
	 */
	public static final String PREFERENCES_CONFIG_USERPWD = "userpwd";
	/**
	 * 用户密码存储配置-用户ID。
	 */
	public static final String PREFERENCES_CONFIG_USERPWD_USERID = "id_";
	/**
	 * 用户密码存储配置-用户机构。
	 */
	public static final String PREFERENCES_CONFIG_USERPWD_AGENCYID = "agency_";
	/**
	 * 共享用户名。
	 */
	public static final String PREFERENCES_CONFIG_SHARE_USER = "share_username";
}