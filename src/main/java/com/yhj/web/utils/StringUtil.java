package com.yhj.web.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * 字符串工具类
 * 
 * @author
 *
 */
public final class StringUtil extends StringUtils {

	/**
	 * 过滤掉集合里的空格
	 * @param list
	 * @return
	 */
	public static List<String> filterWhite(List<String> list) {
		List<String> resultList = new ArrayList<String>();
		for (String l : list) {
			if (isNotEmpty(l)) {
				resultList.add(l);
			}
		}
		return resultList;
	}

	/**
	 * 格式化模糊查询
	 * 
	 * @param str
	 * @return
	 */
	public static String formatLike(String str) {
		return isNotEmpty(str) ? "%" + str + "%" : null;
	}

	/**
	 * 判断是否是空
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		return (str == null || "".equals(str.trim()));
	}

	/**
	 * 判断是否不是空
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNotEmpty(String str) {
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
