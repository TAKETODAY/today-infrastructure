/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.util.ClassUtils;
import cn.taketoday.web.interceptor.HandlerInterceptor;

/**
 * @author TODAY  2018 1 ? - 2018 3 8
 * @version 2.3.7
 */
public interface WebConstant extends cn.taketoday.core.Constant {

  String ENV_SERVLET = "javax.servlet.Servlet";

  boolean RUN_IN_SERVLET = ClassUtils.isPresent(ENV_SERVLET); // @since 3.0.3

  HandlerInterceptor[] EMPTY_HANDLER_INTERCEPTOR = {};

  String NOT_FOUND = "Not Found";
  String BAD_REQUEST = "Bad Request";
  String UNAUTHORIZED = "Unauthorized";
  String ACCESS_FORBIDDEN = "Access Forbidden";
  String METHOD_NOT_ALLOWED = "Method Not Allowed";
  String INTERNAL_SERVER_ERROR = "Internal Server Error";

  // Resolver


  String IMAGE_PNG = "png";
  String HTTP = "http";
  String HTTPS = "https";

  /*****************************************************
   * default values
   */
  // default font
  String DEFAULT_FONT = "Verdana";

  /**
   * default values end
   *******************************************************/

  // Headers
  //-------------------------------------------------------

  // Values
  //----------------------------------------------------
  String NONE = "none";
  String GZIP = "gzip";
  String BYTES = "bytes";
  String CLOSE = "close";
  String PUBLIC = "public";
  String BASE64 = "base64";
  String BINARY = "binary";
  String CHUNKED = "chunked";
  String CHARSET = "charset";
  String MAX_AGE = "max-age";
  String DEFLATE = "deflate";
  String PRIVATE = "private";
  String BOUNDARY = "boundary";
  String IDENTITY = "identity";
  String NO_CACHE = "no-cache";
  String NO_STORE = "no-store";
  String S_MAXAGE = "s-maxage";
  String TRAILERS = "trailers";
  String COMPRESS = "compress";
  String MAX_STALE = "max-stale";
  String MIN_FRESH = "min-fresh";
  String WEBSOCKET = "WebSocket";
  String KEEP_ALIVE = "keep-alive";
  String GZIP_DEFLATE = "gzip,deflate";
  String CONTINUE = "100-continue";
  String NO_TRANSFORM = "no-transform";
  String ONLY_IF_CACHED = "only-if-cached";
  String XML_HTTP_REQUEST = "XMLHttpRequest";
  String MUST_REVALIDATE = "must-revalidate";
  String PROXY_REVALIDATE = "proxy-revalidate";
  String QUOTED_PRINTABLE = "quoted-printable";
  String MULTIPART_FORM_DATA = "multipart/form-data";
  String INLINE_FILE_NAME = "inline;filename=\"";
  String ATTACHMENT_FILE_NAME = "attachment;filename=\"";
  String APPLICATION_OCTET_STREAM = "application/octet-stream";
  String APPLICATION_FORCE_DOWNLOAD = "application/force-download;";
  String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
  // @on

}
