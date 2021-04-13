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
package cn.taketoday.web.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.StringTokenizer;

import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.io.Resource;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.DefaultMultiValueMap;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.MultiValueMap;
import cn.taketoday.context.utils.StringUtils;
import cn.taketoday.web.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestMethod;
import cn.taketoday.web.WebApplicationContext;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.DefaultResponseStatus;
import cn.taketoday.web.handler.HandlerExceptionHandler;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpStatus;

/**
 * @author TODAY <br>
 * 2019-03-15 19:53
 * @since 2.3.7
 */
public abstract class WebUtils {

  private static WebApplicationContext lastStartupContext;

  /**
   * Get last startup {@link WebApplicationContext}
   *
   * @return WebApplicationContext
   */
  public static WebApplicationContext getLastStartupWebContext() {
    return lastStartupContext;
  }

  public static void setLastStartupWebContext(WebApplicationContext applicationContext) {
    WebUtils.lastStartupContext = applicationContext;
  }

  /**
   * Write to {@link OutputStream}
   *
   * @param source
   *         {@link InputStream}
   * @param out
   *         {@link OutputStream}
   * @param bufferSize
   *         buffer size
   *
   * @throws IOException
   *         if any IO exception occurred
   */
  public static void writeToOutputStream(final InputStream source,
                                         final OutputStream out, final int bufferSize) throws IOException //
  {
    final byte[] buff = new byte[bufferSize];
    int len;
    while ((len = source.read(buff)) != -1) {
      out.write(buff, 0, len);
    }
  }

  /**
   * Resolves the content type of the file.
   *
   * @param filename
   *         name of file or path
   *
   * @return file content type
   *
   * @since 2.3.7
   */
  public static String resolveFileContentType(String filename) {
    return URLConnection.getFileNameMap().getContentTypeFor(filename);
  }

  public static String getEtag(String name, long size, long lastModifid) {
    return new StringBuilder()
            .append(name)
            .append(Constant.PATH_SEPARATOR)
            .append(size)
            .append(Constant.PATH_SEPARATOR)
            .append(lastModifid).toString();
  }

  // ---
  public static boolean isMultipart(final RequestContext requestContext) {

    if (!"POST".equals(requestContext.getMethod())) {
      return false;
    }
    final String contentType = requestContext.getContentType();
    return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
  }

  /**
   * Is ajax request
   */
  public static boolean isAjax(HttpHeaders request) {
    return Constant.XML_HTTP_REQUEST.equals(request.getFirst(Constant.X_REQUESTED_WITH));
  }

  public static boolean isHeadRequest(RequestContext requestContext) {
    return "HEAD".equalsIgnoreCase(requestContext.getMethod());
  }

  public static void handleException(final Object handler,
                                     final Throwable exception,
                                     final RequestContext context,
                                     final HandlerExceptionHandler resolver) throws Throwable //
  {
    resolver.handleException(context, ExceptionUtils.unwrapThrowable(exception), handler);
  }

  /**
   * Download file to client.
   *
   * @param context
   *         Current request context
   * @param download
   *         {@link Resource} to download
   * @param bufferSize
   *         Download buffer size
   *
   * @since 2.1.x
   */
  public static void downloadFile(final RequestContext context,
                                  final Resource download, final int bufferSize) throws IOException //
  {
    context.setContentLength(download.contentLength());
    context.setContentType(Constant.APPLICATION_FORCE_DOWNLOAD);
    final HttpHeaders httpHeaders = context.responseHeaders();

    httpHeaders.set(Constant.CONTENT_TRANSFER_ENCODING, Constant.BINARY);
    httpHeaders.set(Constant.CONTENT_DISPOSITION,
                           new StringBuilder(Constant.ATTACHMENT_FILE_NAME)
                                   .append(StringUtils.encodeUrl(download.getName()))
                                   .append(Constant.QUOTATION_MARKS)
                                   .toString()
    );

    try (final InputStream in = download.getInputStream()) {
      writeToOutputStream(in, context.getOutputStream(), bufferSize);
    }
  }

  // ResponseStatus

  public static int getStatusValue(final Throwable ex) {
    return getResponseStatus(ex).value().value();
  }

  public static ResponseStatus getResponseStatus(final Throwable ex) {
    return getResponseStatus(ex.getClass());
  }

  public static ResponseStatus getResponseStatus(Class<? extends Throwable> exceptionClass) {
    if (ConversionException.class.isAssignableFrom(exceptionClass)) {
      return new DefaultResponseStatus(HttpStatus.BAD_REQUEST);
    }
    final ResponseStatus status = ClassUtils.getAnnotation(ResponseStatus.class, exceptionClass);
    if (status != null) {
      return new DefaultResponseStatus(status);
    }
    return new DefaultResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public static ResponseStatus getResponseStatus(final HandlerMethod handler) {
    Assert.notNull(handler, "handler method must not be null");
    ResponseStatus status = handler.getMethodAnnotation(ResponseStatus.class);
    if (status == null) {
      status = handler.getDeclaringClassAnnotation(ResponseStatus.class);
    }
    return wrapStatus(status);
  }

  private static DefaultResponseStatus wrapStatus(ResponseStatus status) {
    return status != null ? new DefaultResponseStatus(status) : null;
  }

  public static ResponseStatus getResponseStatus(final AnnotatedElement handler) {
    Assert.notNull(handler, "AnnotatedElement must not be null");
    ResponseStatus status = handler.getDeclaredAnnotation(ResponseStatus.class);
    if (status == null && handler instanceof Method) {
      final Class<?> declaringClass = ((Method) handler).getDeclaringClass();
      status = declaringClass.getDeclaredAnnotation(ResponseStatus.class);
    }
    return wrapStatus(status);
  }

  // Utility class for CORS request handling based on the
  // CORS W3C recommendation: https://www.w3.org/TR/cors
  // -----------------------------------------------------

  /**
   * Returns {@code true} if the request is a valid CORS one by checking
   * {@code Origin} header presence and ensuring that origins are different.
   */
  public static boolean isCorsRequest(final RequestContext request) {
    final HttpHeaders httpHeaders = request.requestHeaders();
    return httpHeaders.getOrigin() != null;
  }

  /**
   * Returns {@code true} if the request is a valid CORS pre-flight one. To be
   * used in combination with {@link #isCorsRequest(RequestContext)} since regular
   * CORS checks are not invoked here for performance reasons.
   */
  public static boolean isPreFlightRequest(final RequestContext request) {
    return RequestMethod.OPTIONS.name().equals(request.getMethod())
            && request.requestHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null;
  }

  // checkNotModified
  // ---------------------------------------------

  protected static boolean matches(final String matchHeader, final String etag) {
    if (matchHeader != null && StringUtils.isNotEmpty(etag)) {
      return "*".equals(etag) || matchHeader.equals(etag);
    }
    return false;
  }

  public static boolean checkNotModified(String etag, final RequestContext context) {
    return checkNotModified(etag, -1, context);
  }

  public static boolean checkNotModified(long lastModifiedTimestamp, final RequestContext context) {
    return checkNotModified(null, lastModifiedTimestamp, context);
  }

  public static boolean checkNotModified(final String eTag,
                                         final long lastModified,
                                         final RequestContext context) {

    // Validate request headers for caching
    // ---------------------------------------------------

    // If-None-Match header should contain "*" or ETag. If so, then return 304
    final HttpHeaders requestHeaders = context.requestHeaders();
    final String ifNoneMatch = requestHeaders.getFirst(Constant.IF_NONE_MATCH);
    if (matches(ifNoneMatch, eTag)) {
      context.responseHeaders().setETag(eTag); // 304.
      context.setStatus(HttpStatus.NOT_MODIFIED);
      return true;
    }

    // If-Modified-Since header should be greater than LastModified
    // If so, then return 304
    // This header is ignored if any If-None-Match header is specified

    final long ifModifiedSince = requestHeaders.getIfModifiedSince();// If-Modified-Since
    if (ifNoneMatch == null && (ifModifiedSince > 0 && lastModified != 0 && ifModifiedSince >= lastModified)) {
      // if (ifNoneMatch == null && ge(ifModifiedSince, lastModified)) {
      context.responseHeaders().setLastModified(lastModified); // 304
      context.setStatus(HttpStatus.NOT_MODIFIED);
      return true;
    }

    // Validate request headers for resume
    // ----------------------------------------------------

    // If-Match header should contain "*" or ETag. If not, then return 412
    final String ifMatch = requestHeaders.getFirst(Constant.IF_MATCH);
    if (ifMatch != null && !matches(ifMatch, eTag)) {
//      context.status(412);
      context.setStatus(HttpStatus.PRECONDITION_FAILED);
      return true;
    }

    // If-Unmodified-Since header should be greater than LastModified.
    // If not, then return 412.
    final long ifUnmodifiedSince = requestHeaders.getIfUnmodifiedSince();// "If-Unmodified-Since"
    if (ifUnmodifiedSince > 0 && lastModified > 0 && ifUnmodifiedSince <= lastModified) {
      context.setStatus(HttpStatus.PRECONDITION_FAILED);
      return true;
    }
    return false;
  }

  //

  /**
   * Parse Parameters
   *
   * @param s
   *         decoded {@link String}
   *
   * @return Map of list parameters
   */
  public static MultiValueMap<String, String> parseParameters(final String s) {
    if (StringUtils.isEmpty(s)) {
      return new DefaultMultiValueMap<>();
    }

    final DefaultMultiValueMap<String, String> params = new DefaultMultiValueMap<>();
    int nameStart = 0;
    int valueStart = -1;
    int i;
    final int len = s.length();
    loop:
    for (i = 0; i < len; i++) {
      switch (s.charAt(i)) {
        case '=':
          if (nameStart == i) {
            nameStart = i + 1;
          }
          else if (valueStart < nameStart) {
            valueStart = i + 1;
          }
          break;
        case '&':
        case ';':
          addParam(s, nameStart, valueStart, i, params);
          nameStart = i + 1;
          break;
        case '#':
          break loop;
        default:
          // continue
      }
    }
    addParam(s, nameStart, valueStart, i, params);
    return params;
  }

  private static void addParam(
          String s, int nameStart, int valueStart, int valueEnd, DefaultMultiValueMap<String, String> params
  ) {
    if (nameStart < valueEnd) {
      if (valueStart <= nameStart) {
        valueStart = valueEnd + 1;
      }
      String name = s.substring(nameStart, valueStart - 1);
      String value = s.substring(valueStart, valueEnd);
      params.add(name, value);
    }
  }


  /**
   * Parse the given string with matrix variables. An example string would look
   * like this {@code "q1=a;q1=b;q2=a,b,c"}. The resulting map would contain
   * keys {@code "q1"} and {@code "q2"} with values {@code ["a","b"]} and
   * {@code ["a","b","c"]} respectively.
   *
   * @param matrixVariables
   *         the unparsed matrix variables string
   *
   * @return a map with matrix variable names and values (never {@code null})
   *
   * @since 3.0
   */
  public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
    MultiValueMap<String, String> result = new DefaultMultiValueMap<>();
    if (!StringUtils.hasText(matrixVariables)) {
      return result;
    }
    StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
    while (pairs.hasMoreTokens()) {
      String pair = pairs.nextToken();
      int index = pair.indexOf('=');
      if (index != -1) {
        String name = pair.substring(0, index);
        String rawValue = pair.substring(index + 1);
        for (String value : StringUtils.tokenizeToStringArray(rawValue, ",")) {
          result.add(name, value);
        }
      }
      else {
        result.add(pair, Constant.BLANK);
      }
    }
    return result;
  }
}
