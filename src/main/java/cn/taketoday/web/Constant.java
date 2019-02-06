/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web;

/**
 * @author Today <br>
 *         2018,1,6 2018 1 16
 */
public interface Constant extends cn.taketoday.context.Constant {

	/**
	 * 
	 */
	String[] DEFAULT_URL_PATTERNS = { //
			"*.gif", "*.jpg", "*.jpeg", "*.png", "*.js", "*.css", //
			"*.ico", "*.zip", "*.txt", "*.mp3", "*.woff2", "*.woff" //
	};

	String 	ENABLE_WEB_MVC_XML 					= "enable.webmvc.xml";
	String 	ENABLE_WEB_STARTED_LOG				= "enable.started.log";
	//@off
	/**
	 * mode
	 */
	String 	DEV 								= "dev";
	String 	TEST 								= "test";
	String 	PROD 								= "prod";
	String 	WEB_DEBUG 							= "webdebug";
	
	/**********************************************************
	 * Framework Attribute Keys
	 */
	String 	KEY_REQUEST_BODY					= "REQUESTBODY";
	String	KEY_MODEL_AND_VIEW					= "MODELANDVIEW";
	String	KEY_REDIRECT_MODEL					= "REDIRECTMODEL";
	String	KEY_WEB_APPLICATION_CONTEXT 		= "WebApplicationContext";

	/**
	 * @since 2.3.3
	 */
	String 	KEY_REQUEST 						= "Request";
	String 	KEY_SESSION 						= "Session";
	String 	KEY_JSP_TAGLIBS 					= "JspTaglibs";
	String 	KEY_APPLICATION 					= "Application";
	String 	KEY_REQUEST_PARAMETERS 				= "RequestParameters";
	String 	KEY_REQUEST_URI 					= "REQUESTURI";
	String	KEY_REPLACED						= "REPLACED";
	/**
	 * Framework Attribute Keys End
	 **********************************************************/

	String	CONVERT_METHOD						= "doConvert";

	String	WEB_INF								= "/WEB-INF";

	String	DISPATCHER_SERVLET_MAPPING			= "/*";
	String	DEFAULT_MAPPINGS[]					= { DISPATCHER_SERVLET_MAPPING };
	String	VIEW_DISPATCHER						= "viewDispatcher";
	String	DISPATCHER_SERVLET					= "dispatcherServlet";
	String	HANDLER_MAPPING_REGISTRY			= "handlerMappingRegistry";
	String	HANDLER_INTERCEPTOR_REGISTRY		= "handlerInterceptorRegistry";
	
	/**
	 * @since 2.3.3
	 */
	String 	SERVLET_SECURITY_ELEMENT 			= "servletSecurity";	
	String 	MULTIPART_CONFIG_ELEMENT 			= "multipartConfig";
	
	// Resolver
	String	VIEW_RESOLVER						= "viewResolver";
	String	EXCEPTION_RESOLVER					= "exceptionResolver";
	String	MULTIPART_RESOLVER					= "multipartResolver";
	String	PARAMETER_RESOLVER					= "parameterResolver";
	String	VIEW_CONFIG							= "viewConfig";
	String	ACTION_CONFIG						= "actionConfig";
	// the dtd
	String	DTD_NAME							= "web-configuration";
	String	WEB_MVC_CONFIG_LOCATION				= "WebMvcConfigLocation";

	String 	COLLECTION_PARAM_REGEXP				= "(\\[|\\]|\\.)";
	String 	MAP_PARAM_REGEXP					= "(\\['|\\']|\\.)";
	String 	REPLACE_SPLIT_METHOD_URL			= "\\";
	String 	REPLACE_REGEXP						= "\\\\";
	String 	NUMBER_REGEXP						= "\\d+";
	String 	STRING_REGEXP						= "\\w+";
	String 	ONE_PATH							= "\\*";
	String 	ANY_PATH							= "\\*\\*";
	String 	ONE_PATH_REGEXP						= "[\\\\s\\\\S]+";
	String 	ANY_PATH_REGEXP				 		= "[\\\\s\\\\S|/]+";//[\\s\\S]*
	String	PATH_VARIABLE_REGEXP				= "\\{(\\w+)\\}";

	// config
	String	ATTR_ID								= "id";
	String	ATTR_CLASS							= "class";
	String	ATTR_RESOURCE						= "resource";
	String	ATTR_TYPE							= "type";
	String	ATTR_NAME							= "name";
	String	ATTR_VALUE							= VALUE;
	String	ATTR_METHOD							= "method";
	String	ATTR_MAPPING						= "mapping";
	String	ATTR_PREFIX							= "prefix";
	String	ATTR_SUFFIX							= "suffix";
	String	ATTR_BASE_PACKAGE					= "base-package";

	/**
	 * The resoure's content type
	 * @since 2.3.3
	 */
	String	ATTR_CONTENT_TYPE					= "content-type";

	String  VALUE_FORWARD 						= "forward";
	String  VALUE_REDIRECT 						= "redirect";

	String	ELEMENT_EXCEPTION_RESOLVER			= "exception-resolver";
	String	ELEMENT_PARAMETER_RESOLVER			= "parameter-resolver";

	String	ELEMENT_VIEW_RESOLVER				= "view-resolver";
	String	ELEMENT_VIEW_PREFIX					= "view-prefix";
	String	ELEMENT_VIEW_LOCALE					= "view-locale";
	String	ELEMENT_VIEW_SUFFIX					= "view-suffix";
	String	ELEMENT_VIEW_ENCODING				= "view-encoding";

	String	ELEMENT_MULTIPART					= "multipart";
	String	ELEMENT_UPLOAD_ENCODING				= "upload-encoding";
	String	ELEMENT_UPLOAD_LOCATION				= "upload-location";
	String	ELEMENT_UPLOAD_MAX_FILE_SIZE		= "upload-maxFileSize";
	String	ELEMENT_UPLOAD_MAX_REQUEST_SIZE		= "upload-maxRequestSize";
	String	ELEMENT_UPLOAD_FILE_SIZE_THRESHOLD	= "upload-fileSizeThreshold";

	String	ELEMENT_ACTION						= "action";
	String	ELEMENT_DISPATCHER_SERVLET			= "dispatcher-servlet";
	String	ELEMENT_CONTROLLER					= "controller";
	String	ELEMENT_STATIC_RESOURCES			= "static-resources";
	String	ROOT_ELEMENT						= "Web-Configuration";

	byte	ANNOTATION_NULL						= 0x00;
	byte	ANNOTATION_COOKIE					= 0x01;
	byte	ANNOTATION_SESSION					= 0x02;
	byte	ANNOTATION_HEADER					= 0x03;
	byte	ANNOTATION_PATH_VARIABLE			= 0x04;
	byte	ANNOTATION_SERVLET_CONTEXT			= 0x05;

	// byte ANNOTATION_REQUEST_PARAM = 0x05;//不需要设置
	byte	ANNOTATION_MULTIPART				= 0x06;
	byte	ANNOTATION_REQUEST_BODY				= 0x07;

	/*************************************************
	 * Parameter Types
	 */
	byte	TYPE_OTHER							= 0x00;
	byte 	TYPE_ARRAY							= 0x01;

	byte	TYPE_BYTE							= 0x02;
	byte	TYPE_INT							= 0x03;
	byte	TYPE_SHORT							= 0x04;
	byte	TYPE_LONG							= 0x05;
	byte	TYPE_DOUBLE							= 0x06;
	byte	TYPE_FLOAT							= 0x07;
	byte	TYPE_STRING							= 0x08;
	byte	TYPE_BOOLEAN						= 0x09;

	byte	TYPE_HTTP_SESSION					= 0x0A;
	byte	TYPE_SERVLET_CONTEXT				= 0x0B;
	byte	TYPE_HTTP_SERVLET_REQUEST			= 0x0C;
	byte	TYPE_HTTP_SERVLET_RESPONSE			= 0x0D;

	byte	TYPE_MAP							= 0x0E;
	byte	TYPE_SET							= 0x0F;
	byte	TYPE_LIST							= 0x10;

	byte	TYPE_MODEL							= 0x12;
	// multi
//	byte	TYPE_FILE_ITEM						= 0x13;
//	byte	TYPE_ARRAY_FILE_ITEM				= TYPE_ARRAY + TYPE_FILE_ITEM;
	
	byte	TYPE_MULTIPART_FILE					= 0x15;
	byte	TYPE_ARRAY_MULTIPART_FILE			= TYPE_ARRAY + TYPE_MULTIPART_FILE;

//	byte	TYPE_SET_FILE_ITEM					= TYPE_SET + TYPE_FILE_ITEM;
//	byte	TYPE_LIST_FILE_ITEM					= TYPE_LIST + TYPE_FILE_ITEM;

	byte	TYPE_SET_MULTIPART_FILE				= TYPE_SET + TYPE_MULTIPART_FILE;
	byte	TYPE_LIST_MULTIPART_FILE			= TYPE_LIST + TYPE_MULTIPART_FILE;

	/**
	 * {@link cn.taketoday.web.ui.ModelAndView}
	 *
	 * @since 2.3.3
	 */
	byte 	TYPE_MODEL_AND_VIEW					= 0x16;
	/**
	 *
	 * {@link cn.taketoday.web.ui.RedirectModel}
	 *
	 * @since 2.3.3
	 */
	byte	TYPE_REDIRECT_MODEL					= 0x17;

	/**
	 * END  Parameter Types 
	 ***************************************************/

	/***************************************************
	 * return types
	 */
	byte 	RETURN_VOID							= 0x00;
	byte 	RETURN_JSON							= 0x01;
	byte 	RETURN_VIEW							= 0x02;
	
	byte 	RETURN_FILE							= 0x03;
	byte 	RETURN_IMAGE						= 0x04;
	/** @since 2.3.3 */
	byte 	RETURN_STRING						= 0x05;
	/** @since 2.3.3 */
	byte 	RETURN_MODEL_AND_VIEW				= 0x06;
	/** @since 2.3.3 */
	byte 	RETURN_OBJECT						= 0x07;
	
	
	byte 	TYPE_FORWARD						= 0x01;
	byte 	TYPE_REDIRECT						= 0x02;
	
	/**
	 * End return types
	 **************************************************/

	String	CONTENT_TYPE_IMAGE					= "image/jpeg";

	String	REDIRECT_URL_PREFIX					= "redirect:";
	String	QUOTATION_MARKS						= "\"";
	String	IMAGE_PNG							= "png";
	String	HTTP								= "http";
	String	HTTPS								= "https";
	// default font
	/*****************************************************
	 * default values
	 */
	String	DEFAULT_FONT						= "Verdana";
	/** @since 2.3.3 */
	String 	DEFAULT_CONTENT_TYPE				= "text/html;charset=UTF-8";
	String	CONTENT_TYPE_JSON					= "application/json;charset=UTF-8";
	
	/**
	 * default values end
	 *******************************************************/
	
	///////////////////////////////////////////////////////////////
	// headers
	String	TE									= "TE";
	String	AGE									= "Age";
	String	VIA									= "Via";
	String	DATE								= "Date";
	String	ETAG								= "ETag";
	String	FROM								= "From";
	String	VARY								= "Vary";
	String	HOST								= "Host";
	String	ALLOW								= "Allow";
	String	RANGE								= "Range";
	String	COOKIE								= "Cookie";
	String	EXPECT								= "Expect";
	String	ACCEPT								= "Accept";
	String	PRAGMA								= "Pragma";
	String	ORIGIN								= "Origin";
	String	SERVER								= "Server";
	String	EXPIRES								= "Expires";
	String	REFERER								= "Referer";
	String	TRAILER								= "Trailer";
	String	UPGRADE								= "Upgrade";
	String	WARNING								= "Warning";
	String	IF_MATCH							= "If-Match";
	String	IF_RANGE							= "If-Range";
	String	LOCATION							= "Location";
	String	CONNECTION							= "Connection";
	String	SET_COOKIE							= "Set-Cookie";
	String	USER_AGENT							= "User-Agent";
	String	RETRY_AFTER							= "Retry-After";
	String	SET_COOKIE2							= "Set-Cookie2";
	String	CONTENT_MD5							= "Content-MD5";
	String	CONTENT_BASE						= "Content-Base";
	String	CONTENT_TYPE						= "Content-Type";
	String	MAX_FORWARDS						= "Max-Forwards";
	String	ACCEPT_PATCH						= "Accept-Patch";
	String	AUTHORIZATION						= "Authorization";
	String	CACHE_CONTROL						= "Cache-Control";
	String	CONTENT_RANGE						= "Content-Range";
	String	IF_NONE_MATCH						= "If-None-Match";
	String	LAST_MODIFIED						= "Last-Modified";
	String	ACCEPT_RANGES						= "Accept-Ranges";
	String	CONTENT_LENGTH						= "Content-Length";
	String	ACCEPT_CHARSET						= "Accept-Charset";
	String	ACCEPT_ENCODING						= "Accept-Encoding";
	String	ACCEPT_LANGUAGE						= "Accept-Language";
	String	WEBSOCKET_ORIGIN					= "WebSocket-Origin";
	String	X_REQUESTED_WITH					= "X-Requested-With";
	String	WWW_AUTHENTICATE					= "WWW-Authenticate";
	String	CONTENT_ENCODING					= "Content-Encoding";
	String	CONTENT_LANGUAGE					= "Content-Language";
	String	CONTENT_LOCATION					= "Content-Location";
	String	TRANSFER_ENCODING					= "Transfer-Encoding";
	String	SEC_WEBSOCKET_KEY					= "Sec-WebSocket-Key";
	String	IF_MODIFIED_SINCE					= "If-Modified-Since";
	String	WEBSOCKET_LOCATION					= "WebSocket-Location";
	String	WEBSOCKET_PROTOCOL					= "WebSocket-Protocol";
	String	PROXY_AUTHENTICATE					= "Proxy-Authenticate";
	String	SEC_WEBSOCKET_KEY1					= "Sec-WebSocket-Key1";
	String	SEC_WEBSOCKET_KEY2					= "Sec-WebSocket-Key2";
	String	PROXY_AUTHORIZATION					= "Proxy-Authorization";
	String	IF_UNMODIFIED_SINCE					= "If-Unmodified-Since";
	String 	CONTENT_DISPOSITION					= "Content-Disposition";
	String	SEC_WEBSOCKET_ORIGIN				= "Sec-WebSocket-Origin";
	String	SEC_WEBSOCKET_ACCEPT				= "Sec-WebSocket-Accept";
	String	SEC_WEBSOCKET_VERSION				= "Sec-WebSocket-Version";
	String	SEC_WEBSOCKET_PROTOCOL				= "Sec-WebSocket-Protocol";
	String	SEC_WEBSOCKET_LOCATION				= "Sec-WebSocket-Location";
	String	ACCESS_CONTROL_MAX_AGE				= "Access-Control-Max-Age";
	String	CONTENT_TRANSFER_ENCODING			= "Content-Transfer-Encoding";
	String	ACCESS_CONTROL_ALLOW_ORIGIN			= "Access-Control-Allow-Origin";
	String	ACCESS_CONTROL_ALLOW_HEADERS		= "Access-Control-Allow-Headers";
	String	ACCESS_CONTROL_ALLOW_METHODS		= "Access-Control-Allow-Methods";
	String	ACCESS_CONTROL_REQUEST_METHOD		= "Access-Control-Request-Method";
	String	ACCESS_CONTROL_EXPOSE_HEADERS		= "Access-Control-Expose-Headers";
	String	ACCESS_CONTROL_REQUEST_HEADERS		= "Access-Control-Request-Headers";
	String	ACCESS_CONTROL_ALLOW_CREDENTIALS	= "Access-Control-Allow-Credentials";

	/////////////////////////////////////////////////
	// Values
	String	NONE								= "none";
	String	GZIP								= "gzip";
	String	BYTES								= "bytes";
	String	CLOSE								= "close";
	String	PUBLIC								= "public";
	String	BASE64								= "base64";
	String	BINARY								= "binary";
	String	CHUNKED								= "chunked";
	String	CHARSET								= "charset";
	String	MAX_AGE								= "max-age";
	String	DEFLATE								= "deflate";
	String	PRIVATE								= "private";
	String	BOUNDARY							= "boundary";
	String	IDENTITY							= "identity";
	String	NO_CACHE							= "no-cache";
	String	NO_STORE							= "no-store";
	String	S_MAXAGE							= "s-maxage";
	String	TRAILERS							= "trailers";
	String	COMPRESS							= "compress";
	String	MAX_STALE							= "max-stale";
	String	MIN_FRESH							= "min-fresh";
	String	WEBSOCKET							= "WebSocket";
	String	KEEP_ALIVE							= "keep-alive";
	String	GZIP_DEFLATE						= "gzip,deflate";
	String	CONTINUE							= "100-continue";
	String	NO_TRANSFORM						= "no-transform";
	String	ONLY_IF_CACHED						= "only-if-cached";
	String	XML_HTTP_REQUEST					= "XMLHttpRequest";
	String	MUST_REVALIDATE						= "must-revalidate";
	String	PROXY_REVALIDATE					= "proxy-revalidate";
	String	QUOTED_PRINTABLE					= "quoted-printable";
	String	MULTIPART_FORM_DATA					= "multipart/form-data";
	String 	ATTACHMENT_FILE_NAME				= "attachment;filename=\"";
	String 	APPLICATION_FORCE_DOWNLOAD  		= "application/force-download;";
	String	APPLICATION_X_WWW_FORM_URLENCODED	= "application/x-www-form-urlencoded";
	// @on

}
