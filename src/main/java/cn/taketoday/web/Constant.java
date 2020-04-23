/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

import cn.taketoday.web.handler.ResourceMatchResult;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * @author TODAY <br>
 * 
 * @version 2.3.7
 * @time 2018 1 ? - 2018 3 8 <br>
 *       <b>2.2.2.RELEASE -> 2018-08-23 14:53</b><br>
 *       <b>2.2.4.RELEASE -> 2018-09-09 18:37</b><br>
 *       <b>2.3.1.RELEASE -> 2018-10-18 20:26</b><br>
 *       ...<br>
 *       <b>2.3.3.RELEASE -> 2018-12-07 21:28</b><br>
 *       <b>2.3.4.RELEASE -> 2019-02-10 18:03</b><br>
 */
public interface Constant extends cn.taketoday.context.Constant {

    String WEB_VERSION = "2.3.7.RELEASE";

    String DEFAULT_TEMPLATE_PATH = "classpath:templates";
    String X_REQUIRED_AUTHORIZATION = "X-Required-Authorization";

    String ENV_SERVLET = "javax.servlet.Servlet";

    HandlerInterceptor[] EMPTY_HANDLER_INTERCEPTOR = {};

    String NOT_FOUND = "Not Found";
    String BAD_REQUEST = "Bad Request";
    String UNAUTHORIZED = "Unauthorized";
    String ACCESS_FORBIDDEN = "Access Forbidden";
    String METHOD_NOT_ALLOWED = "Method Not Allowed";
    String INTERNAL_SERVER_ERROR = "Internal Server Error";

    String ENABLE_WEB_MVC_XML = "enable.webmvc.xml";
    String DOWNLOAD_BUFF_SIZE = "download.buff.size";
    String ENABLE_WEB_STARTED_LOG = "enable.started.log";
    String FAST_JSON_SERIALIZE_FEATURES = "fastjson.serialize.features";

    String RESOURCE_MATCH_RESULT = ResourceMatchResult.class.getName();

    //@off
	/**********************************************************
	 * Framework Attribute Keys
	 */
	String	KEY_THROWABLE 		                = "THROWABLE";
	String  VALIDATION_ERRORS                   = "validation-errors";
	/**
	 * Framework Attribute Keys End
	 **********************************************************/

	String	DISPATCHER_SERVLET_MAPPING			= "/";
	String	DEFAULT_MAPPINGS[]					= { DISPATCHER_SERVLET_MAPPING };
	String	DISPATCHER_SERVLET					= "dispatcherServlet";
	// Resolver
	String	EXCEPTION_HANDLER					= "exceptionHandler";
	String	TEMPLATE_VIEW_RESOLVER				= "templateViewResolver";
	
	// the dtd
	String	DTD_NAME							= "web-configuration";
	String	WEB_MVC_CONFIG_LOCATION				= "WebMvcConfigLocation";

	String 	COLLECTION_PARAM_REGEXP				= "(\\[|\\]|\\.)";
	String 	MAP_PARAM_REGEXP					= "(\\['|\\']|\\.)";

	// config
	String	ATTR_ID								= "id";
	String	ATTR_CLASS							= "class";
	String	ATTR_RESOURCE						= "resource";
	String	ATTR_NAME							= "name";
	String	ATTR_VALUE							= VALUE;
	String	ATTR_ORDER							= "order";
	String	ATTR_METHOD							= "method";
	String	ATTR_MAPPING						= "mapping";
	/** resource location @since 2.3.7 */
	String	ATTR_LOCATION						= "location";
	String	ATTR_PREFIX							= "prefix";
	String	ATTR_SUFFIX							= "suffix";

	/**
	 * The resoure's content type
	 * @since 2.3.3
	 */
	String	ATTR_CONTENT_TYPE					= "content-type";
	/** The response status @since 2.3.7 */
	String	ATTR_STATUS							= "status";

	String  VALUE_FORWARD 						= "forward";
	String  VALUE_REDIRECT 						= "redirect";

	String	ELEMENT_ACTION						= "action";
	String	ELEMENT_CONTROLLER					= "controller";
	String	ROOT_ELEMENT						= "Web-Configuration";

	String	CONTENT_TYPE_IMAGE					= "image/jpeg";

	String	QUOTATION_MARKS						= "\"";
	String	IMAGE_PNG							= "png";
	String	HTTP								= "http";
	String	HTTPS								= "https";
	String	RESPONSE_BODY_PREFIX				= "body:";
	String	REDIRECT_URL_PREFIX					= "redirect:";
	int		REDIRECT_URL_PREFIX_LENGTH			= REDIRECT_URL_PREFIX.length();
	
	/*****************************************************
	 * default values
	 */
	// default font
	String	DEFAULT_FONT						= "Verdana";
	/** @since 2.3.3 */
	String 	DEFAULT_CONTENT_TYPE				= "text/html;charset=UTF-8";
	String	CONTENT_TYPE_JSON					= "application/json;charset=UTF-8";
	
	/**
	 * default values end
	 *******************************************************/
	
	// Headers
	//-------------------------------------------------------
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

	// Values
	//----------------------------------------------------
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
	String 	INLINE_FILE_NAME					= "inline;filename=\"";
	String 	ATTACHMENT_FILE_NAME				= "attachment;filename=\"";
	String 	APPLICATION_OCTET_STREAM			= "application/octet-stream";
	String 	APPLICATION_FORCE_DOWNLOAD  		= "application/force-download;";
	String	APPLICATION_X_WWW_FORM_URLENCODED	= "application/x-www-form-urlencoded";
	// @on

}
