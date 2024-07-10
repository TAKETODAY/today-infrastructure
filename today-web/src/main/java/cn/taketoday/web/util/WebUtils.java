/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.util;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import cn.taketoday.core.Conventions;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.session.WebSession;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextDecorator;
import cn.taketoday.web.multipart.Multipart;

/**
 * Miscellaneous utilities for web applications.
 * <p>Used by various framework classes.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class WebUtils {

  public static final String ERROR_MESSAGE_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          WebUtils.class, "error-message");

  public static final String ERROR_EXCEPTION_ATTRIBUTE = Conventions.getQualifiedAttributeName(
          WebUtils.class, "error-exception");

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
    Assert.notNull(session, "Session is required");
    Object mutex = session.getAttribute(SESSION_MUTEX_ATTRIBUTE);
    if (mutex == null) {
      mutex = session;
    }
    return mutex;
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
    MultiValueMap<String, String> result = MultiValueMap.forLinkedHashMap();
    if (StringUtils.isBlank(matrixVariables)) {
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

  // ---

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

  // checkNotModified
  // ---------------------------------------------

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
    MultiValueMap<String, String> decodedVars = MultiValueMap.forLinkedHashMap(vars.size());
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
   * @param lookupPath the request URI string to remove ";" content from
   * @return the updated URI string
   */
  public static String removeSemicolonContent(String lookupPath) {
    int semicolonIndex = lookupPath.indexOf(';');
    if (semicolonIndex == -1) {
      return lookupPath;
    }
    StringBuilder sb = new StringBuilder(lookupPath);
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
  public static boolean isValidOrigin(RequestContext request, Collection<String> allowedOrigins) {
    Assert.notNull(request, "Request is required");
    Assert.notNull(allowedOrigins, "Allowed origins is required");

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
  public static boolean isSameOrigin(RequestContext request) {
    HttpHeaders headers = request.getHeaders();
    String origin = headers.getOrigin();
    if (origin == null) {
      return true;
    }

    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();

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

  public static void cleanupMultipartRequest(@Nullable MultiValueMap<String, Multipart> multipartFiles) {
    if (CollectionUtils.isNotEmpty(multipartFiles)) {
      for (Map.Entry<String, List<Multipart>> entry : multipartFiles.entrySet()) {
        List<Multipart> value = entry.getValue();
        for (Multipart multipartFile : value) {
          try {
            multipartFile.cleanup();
          }
          catch (Exception e) {
            LoggerFactory.getLogger(WebUtils.class)
                    .error("error occurred when cleanup multipart", e);
          }
        }
      }
    }
  }

}
