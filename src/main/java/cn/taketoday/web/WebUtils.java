/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

import cn.taketoday.core.annotation.AnnotationAttributes;
import cn.taketoday.core.DefaultMultiValueMap;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.annotation.ResponseStatus;
import cn.taketoday.web.handler.DefaultResponseStatus;
import cn.taketoday.web.handler.HandlerMethod;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.http.HttpMethod;
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
   * Resolves the content type of the file.
   *
   * @param filename name of file or path
   * @return file content type
   * @since 2.3.7
   */
  @Nullable
  public static String resolveFileContentType(String filename) {
    MediaType mediaType = MediaType.fromFileName(filename);
    if (mediaType == null) {
      return null;
    }
    return mediaType.toString();
  }

  public static String getEtag(String name, long size, long lastModified) {
    return new StringBuilder()
            .append("W/\"")
            .append(name)
            .append(Constant.PATH_SEPARATOR)
            .append(size)
            .append(Constant.PATH_SEPARATOR)
            .append(lastModified)
            .append('\"')
            .toString();
  }

  // ---
  public static boolean isMultipart(RequestContext requestContext) {

    if (!"POST".equals(requestContext.getMethod())) {
      return false;
    }
    String contentType = requestContext.getContentType();
    return (contentType != null && contentType.toLowerCase().startsWith("multipart/"));
  }

  /**
   * Is ajax request
   */
  public static boolean isAjax(HttpHeaders request) {
    return HttpHeaders.XML_HTTP_REQUEST.equals(request.getFirst(HttpHeaders.X_REQUESTED_WITH));
  }

  public static boolean isHeadRequest(RequestContext requestContext) {
    return "HEAD".equalsIgnoreCase(requestContext.getMethod());
  }

  /**
   * Download file to client.
   *
   * @param context Current request context
   * @param download {@link Resource} to download
   * @param bufferSize Download buffer size
   * @since 2.1.x
   */
  public static void downloadFile(
          RequestContext context, Resource download, int bufferSize) throws IOException //
  {
    context.setContentLength(download.contentLength());
    context.setContentType(HttpHeaders.APPLICATION_FORCE_DOWNLOAD);
    HttpHeaders httpHeaders = context.responseHeaders();

    httpHeaders.set(HttpHeaders.CONTENT_TRANSFER_ENCODING, HttpHeaders.BINARY);
    httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                    new StringBuilder(HttpHeaders.ATTACHMENT_FILE_NAME)
                            .append(StringUtils.encodeURL(download.getName()))
                            .append(Constant.QUOTATION_MARKS)
                            .toString()
    );

    try (InputStream in = download.getInputStream()) {
      StreamUtils.copy(in, context.getOutputStream(), bufferSize);
    }
  }

  // ResponseStatus

  public static int getStatusValue(Throwable ex) {
    return getResponseStatus(ex).value().value();
  }

  public static ResponseStatus getResponseStatus(Throwable ex) {
    return getResponseStatus(ex.getClass());
  }

  public static ResponseStatus getResponseStatus(Class<? extends Throwable> exceptionClass) {
    if (ConversionException.class.isAssignableFrom(exceptionClass)) {
      return new DefaultResponseStatus(HttpStatus.BAD_REQUEST);
    }
    ResponseStatus status = AnnotationUtils.getAnnotation(ResponseStatus.class, exceptionClass);
    if (status != null) {
      return new DefaultResponseStatus(status);
    }
    return new DefaultResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public static ResponseStatus getResponseStatus(HandlerMethod handler) {
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

  public static ResponseStatus getResponseStatus(AnnotatedElement handler) {
    Assert.notNull(handler, "AnnotatedElement must not be null");
    ResponseStatus status = handler.getDeclaredAnnotation(ResponseStatus.class);
    if (status == null && handler instanceof Method) {
      Class<?> declaringClass = ((Method) handler).getDeclaringClass();
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
  public static boolean isCorsRequest(RequestContext request) {
    HttpHeaders httpHeaders = request.requestHeaders();
    return httpHeaders.getOrigin() != null;
  }

  /**
   * Returns {@code true} if the request is a valid CORS pre-flight one. To be
   * used in combination with {@link #isCorsRequest(RequestContext)} since regular
   * CORS checks are not invoked here for performance reasons.
   */
  public static boolean isPreFlightRequest(RequestContext request) {
    if (HttpMethod.OPTIONS.name().equals(request.getMethod())) {
      HttpHeaders requestHeaders = request.requestHeaders();
      return requestHeaders.containsKey(HttpHeaders.ORIGIN)
              && requestHeaders.containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    }
    return false;
  }

  // checkNotModified
  // ---------------------------------------------

  protected static boolean matches(String matchHeader, String etag) {
    if (matchHeader != null && StringUtils.isNotEmpty(etag)) {
      return "*".equals(etag) || matchHeader.equals(etag);
    }
    return false;
  }

  public static boolean checkNotModified(String etag, RequestContext context) {
    return checkNotModified(etag, -1, context);
  }

  public static boolean checkNotModified(long lastModifiedTimestamp, RequestContext context) {
    return checkNotModified(null, lastModifiedTimestamp, context);
  }

  public static boolean checkNotModified(String eTag,
                                         long lastModified,
                                         RequestContext context) {

    // Validate request headers for caching
    // ---------------------------------------------------

    // If-None-Match header should contain "*" or ETag. If so, then return 304
    HttpHeaders requestHeaders = context.requestHeaders();
    String ifNoneMatch = requestHeaders.getFirst(HttpHeaders.IF_NONE_MATCH);
    if (matches(ifNoneMatch, eTag)) {
      context.responseHeaders().setETag(eTag); // 304.
      context.setStatus(HttpStatus.NOT_MODIFIED);
      return true;
    }

    // If-Modified-Since header should be greater than LastModified
    // If so, then return 304
    // This header is ignored if any If-None-Match header is specified

    long ifModifiedSince = requestHeaders.getIfModifiedSince();// If-Modified-Since
    if (ifNoneMatch == null && (ifModifiedSince > 0 && lastModified != 0 && ifModifiedSince >= lastModified)) {
      // if (ifNoneMatch == null && ge(ifModifiedSince, lastModified)) {
      context.responseHeaders().setLastModified(lastModified); // 304
      context.setStatus(HttpStatus.NOT_MODIFIED);
      return true;
    }

    // Validate request headers for resume
    // ----------------------------------------------------

    // If-Match header should contain "*" or ETag. If not, then return 412
    String ifMatch = requestHeaders.getFirst(HttpHeaders.IF_MATCH);
    if (ifMatch != null && !matches(ifMatch, eTag)) {
//      context.status(412);
      context.setStatus(HttpStatus.PRECONDITION_FAILED);
      return true;
    }

    // If-Unmodified-Since header should be greater than LastModified.
    // If not, then return 412.
    long ifUnmodifiedSince = requestHeaders.getIfUnmodifiedSince();// "If-Unmodified-Since"
    if (ifUnmodifiedSince > 0 && lastModified > 0 && ifUnmodifiedSince <= lastModified) {
      context.setStatus(HttpStatus.PRECONDITION_FAILED);
      return true;
    }
    return false;
  }

  //

  public static void parseParameters(MultiValueMap<String, String> parameterMap, String s) {
    if (StringUtils.isNotEmpty(s)) {
      int nameStart = 0;
      int valueStart = -1;
      int i;
      int len = s.length();
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
            addParam(s, nameStart, valueStart, i, parameterMap);
            nameStart = i + 1;
            break;
          case '#':
            break loop;
          default:
            // continue
        }
      }
      addParam(s, nameStart, valueStart, i, parameterMap);
    }
  }

  /**
   * Parse Parameters
   *
   * @param s decoded {@link String}
   * @return Map of list parameters
   */
  public static MultiValueMap<String, String> parseParameters(String s) {
    DefaultMultiValueMap<String, String> params = new DefaultMultiValueMap<>();
    parseParameters(params, s);
    return params;
  }

  private static void addParam(
          String s, int nameStart, int valueStart, int valueEnd, MultiValueMap<String, String> params
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
   * @param matrixVariables the unparsed matrix variables string
   * @return a map with matrix variable names and values (never {@code null})
   * @since 3.0
   */
  public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
    DefaultMultiValueMap<String, String> result = new DefaultMultiValueMap<>();
    if (StringUtils.hasText(matrixVariables)) {
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
    }
    return result;
  }

  /**
   * @since 4.0
   */
  public static boolean isResponseBody(Method method) {
    AnnotationAttributes attributes = AnnotationUtils.getAttributes(ResponseBody.class, method);
    if (attributes != null) {
      return attributes.getBoolean(Constant.VALUE);
    }
    Class<?> declaringClass = method.getDeclaringClass();
    attributes = AnnotationUtils.getAttributes(ResponseBody.class, declaringClass);
    if (attributes != null) {
      return attributes.getBoolean(Constant.VALUE);
    }
    return false;
  }
}
