package com.yhj.web.core;

/**
 * @author YHJ
 * @version 1.0
 * @time 2017 08 29 22:20
 * @version 2.0
 * @time 2018 1 ? - 2018 3 8
 */
public final class Version {

	public static final String	HISTORY_VERSION			= "1.0.0";

	/**
	 * 当前版本
	 */
	public static final String	VERSION					= "1.2.0";

	/**
	 * 当前版本
	 * 
	 * @return
	 */
	public String getVersion() {
		return VERSION;
	}

	public static String getHistoryVersion() {
		return HISTORY_VERSION;
	}
}
