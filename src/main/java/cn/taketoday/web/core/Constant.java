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
package cn.taketoday.web.core;

import java.io.Serializable;

/**
 * 时间:2018,1,6 2018 1 16
 * 
 * @author Today
 */
public interface Constant extends Serializable {

	String	DEFAULT								= "default";

	String	ACTION_DISPATCHER_MAPPING			= "/";
	String	VIEW_DISPATCHER						= "ViewDispatcher";
	String	ACTION_DISPATCHER					= "ActionDispatcher";

	String	ACTION_HANDLER						= "actionHandler";
	String	VIEW_RESOLVER						= "viewResolver";
	String	EXCEPTION_RESOLVER					= "exceptionResolver";
	String	MULTIPART_RESOLVER					= "multipartResolver";
	String	PARAMETER_RESOLVER					= "parameterResolver";

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
	// byte ANNOTATION_REQUEST_PARAM = 0x05;//不需要设置
	byte	ANNOTATION_MULTIPART				= 0x06;
	byte	ANNOTATION_REQUESTBODY				= 0x07;

	String	TYPE_ARRAY_INT						= "[I";
	String	TYPE_ARRAY_long						= "[J";
	String	TYPE_ARRAY_LONG						= "[Ljava.lang.Long;";
	String	TYPE_ARRAY_STRING					= "[Ljava.lang.String;";
	String	TYPE_ARRAY_INTEGER					= "[Ljava.lang.Integer;";

	String	TYPE_MAP							= "java.util.Map";
	String	TYPE_SET							= "java.util.Set";
	String	TYPE_LIST							= "java.util.List";

	String	TYPE_LONG							= "java.lang.Long";
	String	TYPE_NUMBER							= "java.lang.Number";
	String	TYPE_STRING							= "java.lang.String";
	String	TYPE_INTEGER						= "java.lang.Integer";
	String	TYPE_OPTIONAL						= "java.util.Optional";

	String	HTTP_SESSION						= "javax.servlet.http.HttpSession";
	String	HTTP_SERVLET_CONTEXT				= "javax.servlet.ServletContext";
	String	HTTP_SERVLET_REQUEST				= "javax.servlet.http.HttpServletRequest";
	String	HTTP_SERVLET_RESPONSE				= "javax.servlet.http.HttpServletResponse";

	String	CONTENT_TYPE_JSON					= "application/json;charset=UTF-8";

	String	REDIRECT_URL_PREFIX					= "redirect:";

	String	REQUEST_METHOD_PREFIX				= ":";

	// font
	String	DEFAULT_FONT						= "Verdana";

	String	SITE_EXTENSION						= "action";

	String	FORBIDDEN							= "/Forbidden";

	String	NotFound							= "/NotFound";

	String	ServerIsBusy						= "/ServerIsBusy";

}
