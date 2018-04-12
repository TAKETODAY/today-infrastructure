package com.yhj.web.core;

import java.io.Serializable;

/**
 * 时间:2018,1,6 2018 1 16
 * 
 * @author Today
 */
public interface Constant extends Serializable {

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

	// 文件类型
	public static final String	FILE_JS						= "js";

	public static final String	FILE_CSS					= "css";

	public static final String	FILE_ZIP					= "zip";

	public static final String	FILE_TEXT					= "txt";

	public static final String	FILE_MP3					= "mp3";

	public static final String	FILE_HTML					= "html";

	public static final String	FILE_IMAGE_JPG				= "jpg";

	public static final String	FILE_IMAGE_PNG				= "png";

	public static final String	FILE_DIRECTORY				= "directory";

	public static final String	ADMIN_ARTICLES				= "articles";

	public static final String	ADMIN_COMMENTS				= "comments";

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

	public static final String	CONTEXT_PATH				= "";

	public static final String	SITE_EXTENSION				= "action";

	public static final String	FORBIDDEN					= "/Forbidden." + SITE_EXTENSION;

	public static final String	NotFound					= "/NotFound." + SITE_EXTENSION;

	public static final String	ServerIsBusy				= "/ServerIsBusy." + SITE_EXTENSION;

	public static final String	LOAD_SUCCESS				= "加载成功";

	public static final String	LOAD_FALIED					= "加载失败";

	public static final String	SAVE_SUCCESS				= "保存成功";

	public static final String	SAVE_FALIED					= "保存失败";

	public static final String	OVER_ARTICLE_CONTENT		= "字数超出限制";
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

	/**
	 * 文章状态 评论状态没有站长推荐
	 * 
	 * @author Today
	 */
	/** 屏蔽 */
	public static final byte	ARTICLE_SHIELD				= 0x00;
	/** 还未彻底删除 */
	public static final byte	ARTICLE_DELETED				= 0x01;
	/** 草稿 */
	public static final byte	ARTICLE_DARFT				= 0x02;
	/** 未审核 */
	public static final byte	ARTICLE_NOT_AUDITED			= 0x03;
	/** 已审核 */
	public static final byte	ARTICLE_AUDITED				= 0x04;
	/** 站长推荐 */
	public static final byte	ARTICLE_RECOMMEND			= 0x05;

	/**
	 * 文章基本类型 不可更改
	 * 
	 * @author Today
	 */
	/** Announcement */
	public static final byte	TYPE_ANNOUNCEMENT			= 0x01;
	/** COMPUETR */
	public static final byte	TYPE_COMPUETR				= 0x02;
	/** COMPUETR_LANGUAGE */
	public static final byte	TYPE_COMPUETR_LANGUAGE		= 0x03;
	/** ELECTRONICS */
	public static final byte	TYPE_ELECTRONICS			= 0x04;
	/** 分享 */
	public static final byte	TYPE_SHARE					= 0x05;

	/** 未审核 */
	public static final byte	COMMENT_NOT_AUDITED			= 0x11;
	/** 已审核 */
	public static final byte	COMMENT_AUDITED				= 0x12;
	/** 还未彻底删除 */
	public static final byte	COMMENT_DELETED				= 0x13;
	/** 屏蔽 */
	public static final byte	COMMENT_SHIELD				= 0x09;

	/**
	 * 登录相关
	 * 
	 * @author Today
	 */
	/** 失败 */
	public static final byte	LOGIN_FAILED				= 0x00;
	/** 成功 */
	public static final byte	LOGIN_SUCCESS				= 0x01;
	/** 系统发生异常 */
	public static final byte	EXCEPTION_OCCUR				= 0x04;
	/** 验证码错误 */
	public static final byte	RANDCODE_INCORRECT			= 0x05;

	public static final byte	SYSTEM_NORMAL				= 0x01;

	public static final byte	SYSTEM_ERROR_OCCUR			= 0x00;

	/** 状态正常 */
	public static final byte	USER_NORMAL					= 0x00;
	/** 未激活 */
	public static final byte	USER_INACTIVE				= 0x01;
	/** 账号被锁 */
	public static final byte	USER_LOCKED					= 0x02;
	/** 普通用户 */
	public static final byte	IS_COMMON_USER				= 0x01;
	/** 管理员 */
	public static final byte	IS_MANAGER					= 0x7E;
	/** 超级管理员 */
	public static final byte	IS_SUPER_USER				= 0x7F;
	/** 有用户 */
	public static final byte	EXIST_USER					= 0x01;
	/** 没有用户 */
	public static final byte	NON_EXIST_USER				= 0x00;

	/** 激活失败 */
	public static final byte	ACTIVE_FALIED				= 0x00;
	/** 激活成功 */
	public static final byte	ACTIVE_SUCCESS				= 0x01;
	/** 系统化异常 激活失败 */
	public static final byte	ACTIVE_FORBIDDEN			= 0x02;

}
