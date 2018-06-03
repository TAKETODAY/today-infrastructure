package com.yhj.web.core;

import java.io.Serializable;

/**
 * 时间:2018,1,6 2018 1 16
 * @author Today
 */
public interface Constant extends Serializable {
	
	
	String TYPE_ARRAY_INT			= "[I";
	String TYPE_ARRAY_long			= "[J";
	String TYPE_ARRAY_LONG			= "[Ljava.lang.Long;";
	String TYPE_ARRAY_STRING		= "[Ljava.lang.String;";
	String TYPE_ARRAY_INTEGER		= "[Ljava.lang.Integer;";

	String TYPE_MAP					= "java.util.Map";
	String TYPE_SET					= "java.util.Set";
	String TYPE_LIST				= "java.util.List";
	
	String TYPE_LONG				= "java.lang.Long";
	String TYPE_NUMBER				= "java.lang.Number";
	String TYPE_STRING				= "java.lang.String";
	String TYPE_INTEGER				= "java.lang.Integer";
	String HTTP_SESSION				= "javax.servlet.http.HttpSession";
	String HTTP_SERVLET_REQUEST		= "javax.servlet.http.HttpServletRequest";
	String HTTP_SERVLET_RESPONSE   	= "javax.servlet.http.HttpServletResponse";

	String CONTENT_TYPE_JSON		= "application/json;charset=UTF-8";
	
	String							REDIRECT_URL_PREFIX		= "redirect:";
//	String							REQUEST_METHOD_PREFIX	= ":METHOD:";
	
	String							REQUEST_METHOD_PREFIX	= ":";

	
	
	/**
	 * The number of bytes in a kilobyte.
	 */
	long	ONE_KB						= 1024;

	/**
	 * The number of bytes in a megabyte.
	 */
	long	ONE_MB						= ONE_KB * ONE_KB;

	/**
	 * The number of bytes in a gigabyte.
	 */
	long	ONE_GB						= ONE_KB * ONE_MB;

	/**
	 * The number of bytes in a terabyte.
	 */
	long	ONE_TB						= ONE_KB * ONE_GB;

	// font
	String	DEFAULT_FONT				= "Verdana";

	String	USER_IMAGE_PATH				= "/user/head";

	// Session
	/** OtherFooterInfo */
	String	OTHER_FOOTER_INFO			= "OtherFooterInfo";
	/** operation_log */
	String	OPERATION_LOG				= "operation_log";
	/** icp备案号 */
	String	COPYRIGHT					= "Copyright";
	/** icp备案号 */
	String	ICP							= "ICP";
	/** 关键字 */
	String	KEYWORDS					= "keywords";
	/** 描述 */
	String	DESCRIPTION					= "description";
	/** 百度统计代码 */
	String	BAIDU_CODE					= "baiduCode";
	/** https://www.yanghaijian.top */
	String	HOST						= "host";
	/** CDN */
	String	CDN							= "CDN";
	/** 网站名称 */
	String	SITE_NAME					= "siteName";
	/** 服务器启动时间 */
	String	START_TIME					= "startTime";
	/** 验证码 */
	String	RAND_CODE					= "randCode";
	/** 登录用户 */
	String	USER_INFO					= "userInfo";
	/** 文章详情页面article Session */
	String	ARTICLE						= "article";
	/** 上传文件根路径 */
	String	UPLOAD_ROOT_PATH			= "UPLOAD_ROOT_PATH";

	String	SITE_ROOT_PATH				= "UPLOAD_ROOT_PATH";

	/** 分页 */
	String	PAGINATION					= "pagination";

	String	ARTICLE_LIST				= "ARTICLE_LIST";

	String	SITE_EXTENSION				= "action";

	String	FORBIDDEN					= "/Forbidden";

	String	NotFound					= "/NotFound";

	String	ServerIsBusy				= "/ServerIsBusy";

	/** 默认页面分页大小 */
	Integer	DEFAULT_PAGE_SIZE			= 6;
	/** 默认列表大小 */
	Integer	DEFAULT_LIST_SIZE			= 8;
	/** 默认搜索大小 */
	Integer	DEFAULT_SEARCH_SIZE			= 20;
	/** json搜索大小 */
	Integer	DEFAULT_JSON_SEARCH_SIZE	= 10;

	/** 文章最大字数 */
	Integer	MAX_ARTICLE_CONTENT			= 1024000;

}
