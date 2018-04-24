package com.yhj.web.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串工具类
 * 
 * @author
 *
 */
public final class StringUtil extends StringUtils {


	/**
	 * 判断是否是空
	 * 
	 * @param str
	 * @return
	 */
	public final static boolean isEmpty(String str) {
		return (str == null || "".equals(str.trim()));
	}

	/**
	 * 判断是否不是空
	 * 
	 * @param str
	 * @return
	 */
	public final static boolean isNotEmpty(String str) {
		return (str != null) && !"".equals(str.trim());
	}


	public static int toInt(final String str, final int defaultValue) {
		if (str == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(str);
		} catch (final NumberFormatException nfe) {
			return defaultValue;
		}
	}
	
//	public static void main(String[] args) {
//		System.out.println(isEmpty(null));
//		System.out.println(formatLike(""));
//	}
	
}
