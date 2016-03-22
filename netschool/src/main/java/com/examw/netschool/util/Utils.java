package com.examw.netschool.util;

import org.apache.commons.lang3.StringUtils;

/**
 * 工具类
 * 
 * @author jeasonyoung
 * @since 2015年9月29日
 */
public final class Utils {
	/**
	 * 将时间值(秒)转换为(mm:ss)
	 * @param time
	 * 秒。
	 * @return 时间值。
	 */
	public static String getTime(long time) {
		return StringUtils.leftPad(String.valueOf(time / 60), 2, '0')
				+ ":"
                + StringUtils.leftPad(String.valueOf(time % 60), 2, '0');
	}
}