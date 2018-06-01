package com.yhj.web.core;

import java.io.Serializable;

/**
 * 时间:2018,1,6 2018 1 16
 * @author Today
 */
public interface Constant extends Serializable {
	
	
	String						TYPE_ARRAY_INT				= "[I";
	String						TYPE_ARRAY_long				= "[J";
	String						TYPE_ARRAY_LONG				= "[Ljava.lang.Long;";
	String						TYPE_ARRAY_STRING			= "[Ljava.lang.String;";
	String						TYPE_ARRAY_INTEGER			= "[Ljava.lang.Integer;";

	
	String						TYPE_NUMBER					= "java.lang.Number";
	String						TYPE_LONG					= "java.lang.Long";
	String						TYPE_STRING					= "java.lang.String";
	String						TYPE_INTEGER				= "java.lang.Integer";
	String						HTTP_SESSION				= "javax.servlet.http.HttpSession";
	String						HTTP_SERVLET_REQUEST		= "javax.servlet.http.HttpServletRequest";
	String						HTTP_SERVLET_RESPONSE		= "javax.servlet.http.HttpServletResponse";

	String 						CONTENT_TYPE_JSON			= "application/json;charset=UTF-8";
	
	
	public static final String							REDIRECT_URL_PREFIX		= "redirect:";
//	public static final String							REQUEST_METHOD_PREFIX	= ":METHOD:";
	
	public static final String							REQUEST_METHOD_PREFIX	= ":";
	
	
	/**
	 * The number of bytes in a kilobyte.
	 */
	public static final long	ONE_KB						= 1024;

	/**
	 * The number of bytes in a megabyte.
	 */
	public static final long	ONE_MB						= ONE_KB * ONE_KB;

	/**
	 * The number of bytes in a gigabyte.
	 */
	public static final long	ONE_GB						= ONE_KB * ONE_MB;

	/**
	 * The number of bytes in a terabyte.
	 */
	public static final long	ONE_TB						= ONE_KB * ONE_GB;

	// font
	public static final String	DEFAULT_FONT				= "Verdana";

	public static final String	USER_IMAGE_PATH				= "/user/head";

	// Session
	/** OtherFooterInfo */
	public static final String	OTHER_FOOTER_INFO			= "OtherFooterInfo";
	/** operation_log */
	public static final String	OPERATION_LOG				= "operation_log";
	/** icp备案号 */
	public static final String	COPYRIGHT					= "Copyright";
	/** icp备案号 */
	public static final String	ICP							= "ICP";
	/** 关键字 */
	public static final String	KEYWORDS					= "keywords";
	/** 描述 */
	public static final String	DESCRIPTION					= "description";
	/** 百度统计代码 */
	public static final String	BAIDU_CODE					= "baiduCode";
	/** https://www.yanghaijian.top */
	public static final String	HOST						= "host";
	/** CDN */
	public static final String	CDN							= "CDN";
	/** 网站名称 */
	public static final String	SITE_NAME					= "siteName";
	/** 服务器启动时间 */
	public static final String	START_TIME					= "startTime";
	/** 验证码 */
	public static final String	RAND_CODE					= "randCode";
	/** 登录用户 */
	public static final String	USER_INFO					= "userInfo";
	/** 文章详情页面article Session */
	public static final String	ARTICLE						= "article";
	/** 上传文件根路径 */
	public static final String	UPLOAD_ROOT_PATH			= "UPLOAD_ROOT_PATH";

	public static final String	SITE_ROOT_PATH				= "UPLOAD_ROOT_PATH";

	/** 分页 */
	public static final String	PAGINATION					= "pagination";

	public static final String	ARTICLE_LIST				= "ARTICLE_LIST";

	public static final String	SITE_EXTENSION				= "action";

	public static final String	FORBIDDEN					= "/Forbidden";

	public static final String	NotFound					= "/NotFound";

	public static final String	ServerIsBusy				= "/ServerIsBusy";

	/** 默认页面分页大小 */
	public static final Integer	DEFAULT_PAGE_SIZE			= 6;
	/** 默认列表大小 */
	public static final Integer	DEFAULT_LIST_SIZE			= 8;
	/** 默认搜索大小 */
	public static final Integer	DEFAULT_SEARCH_SIZE			= 20;
	/** json搜索大小 */
	public static final Integer	DEFAULT_JSON_SEARCH_SIZE	= 10;

	/** 文章最大字数 */
	public static final Integer	MAX_ARTICLE_CONTENT			= 1024000;

}
