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

package cn.taketoday.web.util;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.HttpRequest;
import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.server.ServletServerHttpRequest;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MediaType;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextDecorator;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.session.WebSession;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Miscellaneous utilities for web applications.
 * <p>Used by various framework classes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 */
public abstract class WebUtils {
  public static final String ERROR_EXCEPTION_ATTRIBUTE = WebUtils.class.getName() + "-context-throwable";

  /**
   * Prefix of the charset clause in a content type String: ";charset=".
   */
  public static final String CONTENT_TYPE_CHARSET_PREFIX = ";charset=";

  /** Key for the mutex session attribute. */
  public static final String SESSION_MUTEX_ATTRIBUTE = WebUtils.class.getName() + ".MUTEX";

  /**
   * Return the best available mutex for the given session:
   * that is, an object to synchronize on for the given session.
   *
   * <p>The session mutex is guaranteed to be the same object during
   * the entire lifetime of the session, available under the key defined
   * by the {@code SESSION_MUTEX_ATTRIBUTE} constant. It serves as a
   * safe reference to synchronize on for locking on the current session.
   * <p>In many cases, the WebSession reference itself is a safe mutex
   * as well, since it will always be the same object reference for the
   * same active logical session. However, this is not guaranteed across
   * different servlet containers; the only 100% safe way is a session mutex.
   *
   * @param session the WebSession to find a mutex for
   * @return the mutex object (never {@code null})
   * @see #SESSION_MUTEX_ATTRIBUTE
   */
  public static Object getSessionMutex(WebSession session) {
    Assert.notNull(session, "Session must not be null");
    Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
    if (mutex == null) {
      mutex = session;
    }
    return mutex;
  }

  /**
   * Retrieve the first cookie with the given name. Note that multiple
   * cookies can have the same name but different paths or domains.
   *
   * @param request current servlet request
   * @param name cookie name
   * @return the first cookie with the given name, or {@code null} if none is found
   */
  @Nullable
  public static HttpCookie getCookie(RequestContext request, String name) {
    Assert.notNull(request, "Request must not be null");
    HttpCookie[] cookies = request.getCookies();
    for (HttpCookie cookie : cookies) {
      if (name.equals(cookie.getName())) {
        return cookie;
      }
    }
    return null;
  }

  /**
   * Parse the given string with matrix variables. An example string would look
   * like this {@code "q1=a;q1=b;q2=a,b,c"}. The resulting map would contain
   * keys {@code "q1"} and {@code "q2"} with values {@code ["a","b"]} and
   * {@code ["a","b","c"]} respectively.
   *
   * @param matrixVariables the unparsed matrix variables string
   * @return a map with matrix variable names and values (never {@code null})
   */
  public static MultiValueMap<String, String> parseMatrixVariables(String matrixVariables) {
    MultiValueMap<String, String> result = MultiValueMap.fromLinkedHashMap();
    if (!StringUtils.hasText(matrixVariables)) {
      return result;
    }
    StringTokenizer pairs = new StringTokenizer(matrixVariables, ";");
    while (pairs.hasMoreTokens()) {
      String pair = pairs.nextToken();
      int index = pair.indexOf('=');
      if (index != -1) {
        String name = pair.substring(0, index);
        if (name.equalsIgnoreCase("jsessionid")) {
          continue;
        }
        String rawValue = pair.substring(index + 1);
        for (String value : StringUtils.commaDelimitedListToStringArray(rawValue)) {
          result.add(name, value);
        }
      }
      else {
        result.add(pair, "");
      }
    }
    return result;
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

    if (!"POST".equals(requestContext.getMethodValue())) {
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

  /**
   * Return an appropriate request object of the specified type, if available,
   * unwrapping the given request as far as necessary.
   *
   * @param request the RequestContext to introspect
   * @param requiredType the desired type of request object
   * @return the matching request object, or {@code null} if none
   * of that type is available
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public static <T> T getNativeContext(RequestContext request, @Nullable Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(request)) {
        return (T) request;
      }
      else if (request instanceof RequestContextDecorator wrapper) {
        return getNativeContext(wrapper.getDelegate(), requiredType);
      }
    }
    return null;
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
    if (HttpMethod.OPTIONS.name().equals(request.getMethodValue())) {
      HttpHeaders requestHeaders = request.requestHeaders();
      return requestHeaders.containsKey(HttpHeaders.ORIGIN)
              && requestHeaders.containsKey(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
    }
    return false;
  }

  // checkNotModified
  // ---------------------------------------------

  protected static boolean matches(@Nullable String matchHeader, @Nullable String etag) {
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

  public static boolean checkNotModified(
          @Nullable String eTag, long lastModified, RequestContext context) {

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

  /**
   * Sanitize the given path. Uses the following rules:
   * <ul>
   * <li>replace all "//" by "/"</li>
   * </ul>
   */
  public static String getSanitizedPath(final String path) {
    int index = path.indexOf("//");
    if (index >= 0) {
      StringBuilder sanitized = new StringBuilder(path);
      while (index != -1) {
        sanitized.deleteCharAt(index);
        index = sanitized.indexOf("//", index);
      }
      return sanitized.toString();
    }
    return path;
  }

  /**
   * Decode the given URI path variables
   *
   * @param vars the URI variables extracted from the URL path
   * @return the same Map or a new Map instance
   */
  public static Map<String, String> decodePathVariables(Map<String, String> vars) {
    Map<String, String> decodedVars = CollectionUtils.newLinkedHashMap(vars.size());
    for (Map.Entry<String, String> entry : vars.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      decodedVars.put(key, UriUtils.decode(value, StandardCharsets.UTF_8));
    }
    return decodedVars;
  }

  /**
   * Decode the given matrix variables
   *
   * @param vars the URI variables extracted from the URL path
   * @return the same Map or a new Map instance
   */
  public static MultiValueMap<String, String> decodeMatrixVariables(MultiValueMap<String, String> vars) {
    MultiValueMap<String, String> decodedVars = MultiValueMap.fromLinkedHashMap(vars.size());
    for (Map.Entry<String, List<String>> entry : vars.entrySet()) {
      String key = entry.getKey();
      List<String> values = entry.getValue();
      for (String value : values) {
        decodedVars.add(key, UriUtils.decode(value, StandardCharsets.UTF_8));
      }
    }
    return decodedVars;
  }

  /**
   * Remove ";" (semicolon) content from the given request URI
   *
   * @param requestUri the request URI string to remove ";" content from
   * @return the updated URI string
   */
  public static String removeSemicolonContent(String requestUri) {
    int semicolonIndex = requestUri.indexOf(';');
    if (semicolonIndex == -1) {
      return requestUri;
    }
    StringBuilder sb = new StringBuilder(requestUri);
    while (semicolonIndex != -1) {
      int slashIndex = sb.indexOf("/", semicolonIndex + 1);
      if (slashIndex == -1) {
        return sb.substring(0, semicolonIndex);
      }
      sb.delete(semicolonIndex, slashIndex);
      semicolonIndex = sb.indexOf(";", semicolonIndex);
    }
    return sb.toString();
  }

  /**
   * Check the given request origin against a list of allowed origins.
   * A list containing "*" means that all origins are allowed.
   * An empty list means only same origin is allowed.
   *
   * <p><strong>Note:</strong> this method ignores
   * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
   * client-originated address. Consider using the {@code ForwardedHeaderFilter}
   * to extract and use, or to discard such headers.
   *
   * @return {@code true} if the request origin is valid, {@code false} otherwise
   * @see <a href="https://tools.ietf.org/html/rfc6454">RFC 6454: The Web Origin Concept</a>
   */
  public static boolean isValidOrigin(HttpRequest request, Collection<String> allowedOrigins) {
    Assert.notNull(request, "Request must not be null");
    Assert.notNull(allowedOrigins, "Allowed origins must not be null");

    String origin = request.getHeaders().getOrigin();
    if (origin == null || allowedOrigins.contains("*")) {
      return true;
    }
    else if (CollectionUtils.isEmpty(allowedOrigins)) {
      return isSameOrigin(request);
    }
    else {
      return allowedOrigins.contains(origin);
    }
  }

  /**
   * Check if the request is a same-origin one, based on {@code Origin}, {@code Host},
   * {@code Forwarded}, {@code X-Forwarded-Proto}, {@code X-Forwarded-Host} and
   * {@code X-Forwarded-Port} headers.
   *
   * <p><strong>Note:</strong> this method ignores
   * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
   * client-originated address. Consider using the {@code ForwardedHeaderFilter}
   * to extract and use, or to discard such headers.
   *
   * @return {@code true} if the request is a same-origin one, {@code false} in case
   * of cross-origin request
   */
  public static boolean isSameOrigin(HttpRequest request) {
    HttpHeaders headers = request.getHeaders();
    String origin = headers.getOrigin();
    if (origin == null) {
      return true;
    }

    String scheme;
    String host;
    int port;

    if (request instanceof ServletRequestContext servletContext) {
      HttpServletRequest servletRequest = ServletUtils.getServletRequest(servletContext);
      scheme = servletRequest.getScheme();
      host = servletRequest.getServerName();
      port = servletRequest.getServerPort();
    }
    else if (request instanceof ServletServerHttpRequest servletServerHttpRequest) {
      // Build more efficiently if we can: we only need scheme, host, port for origin comparison
      HttpServletRequest servletRequest = servletServerHttpRequest.getServletRequest();
      scheme = servletRequest.getScheme();
      host = servletRequest.getServerName();
      port = servletRequest.getServerPort();
    }
    else {
      URI uri = request.getURI();
      scheme = uri.getScheme();
      host = uri.getHost();
      port = uri.getPort();
    }

    UriComponents originUrl = UriComponentsBuilder.fromOriginHeader(origin).build();
    return Objects.equals(scheme, originUrl.getScheme())
            && Objects.equals(host, originUrl.getHost())
            && getPort(scheme, port) == getPort(originUrl.getScheme(), originUrl.getPort());
  }

  private static int getPort(@Nullable String scheme, int port) {
    if (port == -1) {
      if ("http".equals(scheme) || "ws".equals(scheme)) {
        port = 80;
      }
      else if ("https".equals(scheme) || "wss".equals(scheme)) {
        port = 443;
      }
    }
    return port;
  }

}
