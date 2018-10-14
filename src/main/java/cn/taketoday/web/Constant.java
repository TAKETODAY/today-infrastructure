/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.web;

import java.io.Serializable;

/**
 * 
 * @author Today <br>
 *         2018,1,6 2018 1 16
 */
public interface Constant extends Serializable {

	String	CONVERT_METHOD						= "doConvert";

	String	DEFAULT_ENCODING					= "UTF-8";

	String	DEFAULT								= "default";
	String	WEB_INF								= "/WEB-INF";

	String	DISPATCHER_SERVLET_MAPPING			= "/";
	String	VIEW_DISPATCHER						= "ViewDispatcher";
	String	DISPATCHER_SERVLET					= "DispatcherServlet";

	// Resolver
	String	VIEW_RESOLVER						= "viewResolver";
	String	ACTION_HANDLER						= "actionHandler";
	String	EXCEPTION_RESOLVER					= "exceptionResolver";
	String	MULTIPART_RESOLVER					= "multipartResolver";
	String	PARAMETER_RESOLVER					= "parameterResolver";
	String	ACTION_CONFIG						= "actionConfig";
	// the dtd
	String	DTD_NAME							= "web-configuration";

	String	PATH_VARIABLE_REGEXP				= "\\{(\\w+)\\}";

	// config
	String	ATTR_ID								= "id";
	String	ATTR_CLASS							= "class";
	String	ATTR_ASSET							= "res";
	String	ATTR_TYPE							= "type";
	String	ATTR_NAME							= "name";
	String	ATTR_VALUE							= "value";
	String	ATTR_MAPPING						= "mapping";
	String	ATTR_PREFIX							= "prefix";
	String	ATTR_SUFFIX							= "suffix";
	String	ATTR_BASE_PACKAGE					= "base-package";

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

	String	ELEMENT_COMMON						= "common";
	String	ELEMENT_STATIC_RESOURCES			= "static-resources";
	String	ROOT_ELEMENT						= "Web-Configuration";

	int		TYPE_DISPATCHER						= 0x00;
	int		TYPE_REDIRECT						= 0x01;

	byte	ANNOTATION_NULL						= 0x00;
	byte	ANNOTATION_COOKIE					= 0x01;
	byte	ANNOTATION_SESSION					= 0x02;
	byte	ANNOTATION_HEADER					= 0x03;
	byte	ANNOTATION_PATH_VARIABLE			= 0x04;
	byte	ANNOTATION_SERVLET_CONTEXT			= 0x05;
	
	// byte ANNOTATION_REQUEST_PARAM = 0x05;//不需要设置
	byte	ANNOTATION_MULTIPART				= 0x06;
	byte	ANNOTATION_REQUESTBODY				= 0x07;
	
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
	
	byte	TYPE_OPTIONAL						= 0x11;
	byte	TYPE_MODEL							= 0x12;
	
	/**
	 * END
	 * 
	 **************************************************/
	

	String	CONTENT_TYPE_JSON					= "application/json;charset=UTF-8";

	String	APPLICATION_X_WWW_FORM_URLENCODED	= "application/x-www-form-urlencoded";

	String	REDIRECT_URL_PREFIX					= "redirect:";
	String	HTTP								= "http";
	String	REQUEST_METHOD_PREFIX				= ":";

	// font
	String	DEFAULT_FONT						= "Verdana";

	///////////////////////////////////////////////////////////////
	//
	// headers
	//
	///////////////////////////////////////////////////////////////
	/**
	 * {@code "Accept"}
	 */
	String	ACCEPT								= "Accept";
	/**
	 * {@code "Accept-Charset"}
	 */
	String	ACCEPT_CHARSET						= "Accept-Charset";
	/**
	 * {@code "Accept-Encoding"}
	 */
	String	ACCEPT_ENCODING						= "Accept-Encoding";
	/**
	 * {@code "Accept-Language"}
	 */
	String	ACCEPT_LANGUAGE						= "Accept-Language";
	/**
	 * {@code "Accept-Ranges"}
	 */
	String	ACCEPT_RANGES						= "Accept-Ranges";
	/**
	 * {@code "Accept-Patch"}
	 */
	String	ACCEPT_PATCH						= "Accept-Patch";
	/**
	 * {@code "Access-Control-Allow-Credentials"}
	 */
	String	ACCESS_CONTROL_ALLOW_CREDENTIALS	= "Access-Control-Allow-Credentials";
	/**
	 * {@code "Access-Control-Allow-Headers"}
	 */
	String	ACCESS_CONTROL_ALLOW_HEADERS		= "Access-Control-Allow-Headers";
	/**
	 * {@code "Access-Control-Allow-Methods"}
	 */
	String	ACCESS_CONTROL_ALLOW_METHODS		= "Access-Control-Allow-Methods";
	/**
	 * {@code "Access-Control-Allow-Origin"}
	 */
	String	ACCESS_CONTROL_ALLOW_ORIGIN			= "Access-Control-Allow-Origin";
	/**
	 * {@code "Access-Control-Expose-Headers"}
	 */
	String	ACCESS_CONTROL_EXPOSE_HEADERS		= "Access-Control-Expose-Headers";
	/**
	 * {@code "Access-Control-Max-Age"}
	 */
	String	ACCESS_CONTROL_MAX_AGE				= "Access-Control-Max-Age";
	/**
	 * {@code "Access-Control-Request-Headers"}
	 */
	String	ACCESS_CONTROL_REQUEST_HEADERS		= "Access-Control-Request-Headers";
	/**
	 * {@code "Access-Control-Request-Method"}
	 */
	String	ACCESS_CONTROL_REQUEST_METHOD		= "Access-Control-Request-Method";
	/**
	 * {@code "Age"}
	 */
	String	AGE									= "Age";
	/**
	 * {@code "Allow"}
	 */
	String	ALLOW								= "Allow";
	/**
	 * {@code "Authorization"}
	 */
	String	AUTHORIZATION						= "Authorization";
	/**
	 * {@code "Cache-Control"}
	 */
	String	CACHE_CONTROL						= "Cache-Control";
	/**
	 * {@code "Connection"}
	 */
	String	CONNECTION							= "Connection";
	/**
	 * {@code "Content-Base"}
	 */
	String	CONTENT_BASE						= "Content-Base";
	/**
	 * {@code "Content-Encoding"}
	 */
	String	CONTENT_ENCODING					= "Content-Encoding";
	/**
	 * {@code "Content-Language"}
	 */
	String	CONTENT_LANGUAGE					= "Content-Language";
	/**
	 * {@code "Content-Length"}
	 */
	String	CONTENT_LENGTH						= "Content-Length";
	/**
	 * {@code "Content-Location"}
	 */
	String	CONTENT_LOCATION					= "Content-Location";
	/**
	 * {@code "Content-Transfer-Encoding"}
	 */
	String	CONTENT_TRANSFER_ENCODING			= "Content-Transfer-Encoding";
	/**
	 * {@code "Content-MD5"}
	 */
	String	CONTENT_MD5							= "Content-MD5";
	/**
	 * {@code "Content-Range"}
	 */
	String	CONTENT_RANGE						= "Content-Range";
	/**
	 * {@code "Content-Type"}
	 */
	String	CONTENT_TYPE						= "Content-Type";
	/**
	 * {@code "Cookie"}
	 */
	String	COOKIE								= "Cookie";
	/**
	 * {@code "Date"}
	 */
	String	DATE								= "Date";
	/**
	 * {@code "ETag"}
	 */
	String	ETAG								= "ETag";
	/**
	 * {@code "Expect"}
	 */
	String	EXPECT								= "Expect";
	/**
	 * {@code "Expires"}
	 */
	String	EXPIRES								= "Expires";
	/**
	 * {@code "From"}
	 */
	String	FROM								= "From";
	/**
	 * {@code "Host"}
	 */
	String	HOST								= "Host";
	/**
	 * {@code "If-Match"}
	 */
	String	IF_MATCH							= "If-Match";
	/**
	 * {@code "If-Modified-Since"}
	 */
	String	IF_MODIFIED_SINCE					= "If-Modified-Since";
	/**
	 * {@code "If-None-Match"}
	 */
	String	IF_NONE_MATCH						= "If-None-Match";
	/**
	 * {@code "If-Range"}
	 */
	String	IF_RANGE							= "If-Range";
	/**
	 * {@code "If-Unmodified-Since"}
	 */
	String	IF_UNMODIFIED_SINCE					= "If-Unmodified-Since";
	/**
	 * {@code "Last-Modified"}
	 */
	String	LAST_MODIFIED						= "Last-Modified";
	/**
	 * {@code "Location"}
	 */
	String	LOCATION							= "Location";
	/**
	 * {@code "Max-Forwards"}
	 */
	String	MAX_FORWARDS						= "Max-Forwards";
	/**
	 * {@code "Origin"}
	 */
	String	ORIGIN								= "Origin";
	/**
	 * {@code "Pragma"}
	 */
	String	PRAGMA								= "Pragma";
	/**
	 * {@code "Proxy-Authenticate"}
	 */
	String	PROXY_AUTHENTICATE					= "Proxy-Authenticate";
	/**
	 * {@code "Proxy-Authorization"}
	 */
	String	PROXY_AUTHORIZATION					= "Proxy-Authorization";
	/**
	 * {@code "Range"}
	 */
	String	RANGE								= "Range";
	/**
	 * {@code "Referer"}
	 */
	String	REFERER								= "Referer";
	/**
	 * {@code "Retry-After"}
	 */
	String	RETRY_AFTER							= "Retry-After";
	/**
	 * {@code "Sec-WebSocket-Key1"}
	 */
	String	SEC_WEBSOCKET_KEY1					= "Sec-WebSocket-Key1";
	/**
	 * {@code "Sec-WebSocket-Key2"}
	 */
	String	SEC_WEBSOCKET_KEY2					= "Sec-WebSocket-Key2";
	/**
	 * {@code "Sec-WebSocket-Location"}
	 */
	String	SEC_WEBSOCKET_LOCATION				= "Sec-WebSocket-Location";
	/**
	 * {@code "Sec-WebSocket-Origin"}
	 */
	String	SEC_WEBSOCKET_ORIGIN				= "Sec-WebSocket-Origin";
	/**
	 * {@code "Sec-WebSocket-Protocol"}
	 */
	String	SEC_WEBSOCKET_PROTOCOL				= "Sec-WebSocket-Protocol";
	/**
	 * {@code "Sec-WebSocket-Version"}
	 */
	String	SEC_WEBSOCKET_VERSION				= "Sec-WebSocket-Version";
	/**
	 * {@code "Sec-WebSocket-Key"}
	 */
	String	SEC_WEBSOCKET_KEY					= "Sec-WebSocket-Key";
	/**
	 * {@code "Sec-WebSocket-Accept"}
	 */
	String	SEC_WEBSOCKET_ACCEPT				= "Sec-WebSocket-Accept";
	/**
	 * {@code "Server"}
	 */
	String	SERVER								= "Server";
	/**
	 * {@code "Set-Cookie"}
	 */
	String	SET_COOKIE							= "Set-Cookie";
	/**
	 * {@code "Set-Cookie2"}
	 */
	String	SET_COOKIE2							= "Set-Cookie2";
	/**
	 * {@code "TE"}
	 */
	String	TE									= "TE";
	/**
	 * {@code "Trailer"}
	 */
	String	TRAILER								= "Trailer";
	/**
	 * {@code "Transfer-Encoding"}
	 */
	String	TRANSFER_ENCODING					= "Transfer-Encoding";
	/**
	 * {@code "Upgrade"}
	 */
	String	UPGRADE								= "Upgrade";
	/**
	 * {@code "User-Agent"}
	 */
	String	USER_AGENT							= "User-Agent";
	/**
	 * {@code "Vary"}
	 */
	String	VARY								= "Vary";
	/**
	 * {@code "Via"}
	 */
	String	VIA									= "Via";
	/**
	 * {@code "Warning"}
	 */
	String	WARNING								= "Warning";
	/**
	 * {@code "WebSocket-Location"}
	 */
	String	WEBSOCKET_LOCATION					= "WebSocket-Location";
	/**
	 * {@code "WebSocket-Origin"}
	 */
	String	WEBSOCKET_ORIGIN					= "WebSocket-Origin";
	/**
	 * {@code "WebSocket-Protocol"}
	 */
	String	WEBSOCKET_PROTOCOL					= "WebSocket-Protocol";
	/**
	 * {@code "WWW-Authenticate"}
	 */
	String	WWW_AUTHENTICATE					= "WWW-Authenticate";

	String	X_REQUESTED_WITH					= "X-Requested-With";
	/////////////////////////////////////////////////
	// Values

	String	BASE64								= "base64";
	/**
	 * {@code "binary"}
	 */
	String	BINARY								= "binary";
	/**
	 * {@code "boundary"}
	 */
	String	BOUNDARY							= "boundary";
	/**
	 * {@code "bytes"}
	 */
	String	BYTES								= "bytes";
	/**
	 * {@code "charset"}
	 */
	String	CHARSET								= "charset";
	/**
	 * {@code "chunked"}
	 */
	String	CHUNKED								= "chunked";
	/**
	 * {@code "close"}
	 */
	String	CLOSE								= "close";
	/**
	 * {@code "compress"}
	 */
	String	COMPRESS							= "compress";
	/**
	 * {@code "100-continue"}
	 */
	String	CONTINUE							= "100-continue";
	/**
	 * {@code "deflate"}
	 */
	String	DEFLATE								= "deflate";
	/**
	 * {@code "gzip"}
	 */
	String	GZIP								= "gzip";
	/**
	 * {@code "gzip,deflate"}
	 */
	String	GZIP_DEFLATE						= "gzip,deflate";
	/**
	 * {@code "identity"}
	 */
	String	IDENTITY							= "identity";
	/**
	 * {@code "keep-alive"}
	 */
	String	KEEP_ALIVE							= "keep-alive";
	/**
	 * {@code "max-age"}
	 */
	String	MAX_AGE								= "max-age";
	/**
	 * {@code "max-stale"}
	 */
	String	MAX_STALE							= "max-stale";
	/**
	 * {@code "min-fresh"}
	 */
	String	MIN_FRESH							= "min-fresh";
	/**
	 * {@code "multipart/form-data"}
	 */
	String	MULTIPART_FORM_DATA					= "multipart/form-data";
	/**
	 * {@code "must-revalidate"}
	 */
	String	MUST_REVALIDATE						= "must-revalidate";
	/**
	 * {@code "no-cache"}
	 */
	String	NO_CACHE							= "no-cache";
	/**
	 * {@code "no-store"}
	 */
	String	NO_STORE							= "no-store";
	/**
	 * {@code "no-transform"}
	 */
	String	NO_TRANSFORM						= "no-transform";
	/**
	 * {@code "none"}
	 */
	String	NONE								= "none";
	/**
	 * {@code "only-if-cached"}
	 */
	String	ONLY_IF_CACHED						= "only-if-cached";
	/**
	 * {@code "private"}
	 */
	String	PRIVATE								= "private";
	/**
	 * {@code "proxy-revalidate"}
	 */
	String	PROXY_REVALIDATE					= "proxy-revalidate";
	/**
	 * {@code "public"}
	 */
	String	PUBLIC								= "public";
	/**
	 * {@code "quoted-printable"}
	 */
	String	QUOTED_PRINTABLE					= "quoted-printable";
	/**
	 * {@code "s-maxage"}
	 */
	String	S_MAXAGE							= "s-maxage";
	/**
	 * {@code "trailers"}
	 */
	String	TRAILERS							= "trailers";
	/**
	 * {@code "WebSocket"}
	 */
	String	WEBSOCKET							= "WebSocket";

	String	XML_HTTP_REQUEST					= "XMLHttpRequest";
}
