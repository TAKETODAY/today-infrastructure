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

import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;

/**
 * Helper class for URL path matching. Provides support for URL paths in
 * {@code RequestDispatcher} includes and support for consistent URL decoding.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @see #getLookupPathForRequest
 * @see jakarta.servlet.RequestDispatcher
 * @since 4.0
 */
public class UrlPathHelper {

  /**
   * Name of Servlet request attribute that holds a
   * {@link #getLookupPathForRequest resolved} lookupPath.
   */
  public static final String PATH_ATTRIBUTE = UrlPathHelper.class.getName() + ".PATH";

  private static final Logger logger = LoggerFactory.getLogger(UrlPathHelper.class);

  private boolean alwaysUseFullPath = false;

  private boolean urlDecode = true;

  private boolean removeSemicolonContent = true;

  private String defaultEncoding = Constant.DEFAULT_ENCODING;

  private boolean readOnly = false;

  /**
   * Whether URL lookups should always use the full path within the current
   * web application context, i.e. within
   * {@link jakarta.servlet.ServletContext#getContextPath()}.
   * <p>If set to {@literal false} the path within the current servlet mapping
   * is used instead if applicable (i.e. in the case of a prefix based Servlet
   * mapping such as "/myServlet/*").
   * <p>By default this is set to "false".
   */
  public void setAlwaysUseFullPath(boolean alwaysUseFullPath) {
    checkReadOnly();
    this.alwaysUseFullPath = alwaysUseFullPath;
  }

  /**
   * Whether the context path and request URI should be decoded -- both of
   * which are returned <i>undecoded</i> by the Servlet API, in contrast to
   * the servlet path.
   * <p>Either the request encoding or the default Servlet spec encoding
   * (ISO-8859-1) is used when set to "true".
   * <p>By default this is set to {@literal true}.
   * <p><strong>Note:</strong> Be aware the servlet path will not match when
   * compared to encoded paths. Therefore use of {@code urlDecode=false} is
   * not compatible with a prefix-based Servlet mapping and likewise implies
   * also setting {@code alwaysUseFullPath=true}.
   *
   * @see #getContextPath
   * @see #getRequestUri
   * @see Constant#DEFAULT_ENCODING
   * @see URLDecoder#decode(String, String)
   */
  public void setUrlDecode(boolean urlDecode) {
    checkReadOnly();
    this.urlDecode = urlDecode;
  }

  /**
   * Whether to decode the request URI when determining the lookup path.
   *
   * @since 4.0
   */
  public boolean isUrlDecode() {
    return this.urlDecode;
  }

  /**
   * Set if ";" (semicolon) content should be stripped from the request URI.
   * <p>Default is "true".
   */
  public void setRemoveSemicolonContent(boolean removeSemicolonContent) {
    checkReadOnly();
    this.removeSemicolonContent = removeSemicolonContent;
  }

  /**
   * Whether configured to remove ";" (semicolon) content from the request URI.
   */
  public boolean shouldRemoveSemicolonContent() {
    return this.removeSemicolonContent;
  }

  /**
   * Set the default character encoding to use for URL decoding.
   * Default is ISO-8859-1, according to the Servlet spec.
   * <p>If the request specifies a character encoding itself, the request
   * encoding will override this setting. This also allows for generically
   * overriding the character encoding in a filter that invokes the
   * {@code ServletRequest.setCharacterEncoding} method.
   *
   * @param defaultEncoding the character encoding to use
   * @see Constant#DEFAULT_ENCODING
   */
  public void setDefaultEncoding(String defaultEncoding) {
    checkReadOnly();
    this.defaultEncoding = defaultEncoding;
  }

  /**
   * Return the default character encoding to use for URL decoding.
   */
  protected String getDefaultEncoding() {
    return this.defaultEncoding;
  }

  /**
   * Switch to read-only mode where further configuration changes are not allowed.
   */
  private void setReadOnly() {
    this.readOnly = true;
  }

  private void checkReadOnly() {
    Assert.isTrue(!this.readOnly, "This instance cannot be modified");
  }

  /**
   * {@link #getLookupPathForRequest Resolve} the lookupPath and cache it in a
   * request attribute with the key {@link #PATH_ATTRIBUTE} for subsequent
   * access via {@link #getResolvedLookupPath(RequestContext)}.
   *
   * @param request the current request
   * @return the resolved path
   */
  public String resolveAndCacheLookupPath(RequestContext request) {
    String lookupPath = getLookupPathForRequest(request);
    request.setAttribute(PATH_ATTRIBUTE, lookupPath);
    return lookupPath;
  }

  /**
   * Return a previously {@link #getLookupPathForRequest resolved} lookupPath.
   *
   * @param request the current request
   * @return the previously resolved lookupPath
   * @throws IllegalArgumentException if the not found
   */
  public static String getResolvedLookupPath(RequestContext request) {
    Object attribute = request.getAttribute(PATH_ATTRIBUTE);
    if (attribute instanceof String lookupPath) {
      return lookupPath;
    }
    throw new IllegalArgumentException("Expected lookupPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
  }

  /**
   * Return the mapping lookup path for the given request, within the current
   * servlet mapping if applicable, else within the web application.
   * <p>Detects include request URL if called within a RequestDispatcher include.
   *
   * @param request current HTTP request
   * @return the lookup path
   * @see #getPathWithinApplication
   */
  public String getLookupPathForRequest(RequestContext request) {
    // Always use full path within current servlet context?
    return getPathWithinApplication(request);
  }

  /**
   * Return the path within the web application for the given request.
   * <p>Detects include request URL if called within a RequestDispatcher include.
   *
   * @param request current HTTP request
   * @return the path within the web application
   * @see #getLookupPathForRequest
   */
  public String getPathWithinApplication(RequestContext request) {
    String contextPath = getContextPath(request);
    String requestUri = getRequestUri(request);
    String path = getRemainingPath(requestUri, contextPath, true);
    if (path != null) {
      // Normal case: URI contains context path.
      return (StringUtils.hasText(path) ? path : "/");
    }
    else {
      return requestUri;
    }
  }

  /**
   * Match the given "mapping" to the start of the "requestUri" and if there
   * is a match return the extra part. This method is needed because the
   * context path and the servlet path returned by the RequestContext are
   * stripped of semicolon content unlike the requestUri.
   */
  @Nullable
  private String getRemainingPath(String requestUri, String mapping, boolean ignoreCase) {
    int index1 = 0;
    int index2 = 0;
    for (; (index1 < requestUri.length()) && (index2 < mapping.length()); index1++, index2++) {
      char c1 = requestUri.charAt(index1);
      char c2 = mapping.charAt(index2);
      if (c1 == ';') {
        index1 = requestUri.indexOf('/', index1);
        if (index1 == -1) {
          return null;
        }
        c1 = requestUri.charAt(index1);
      }
      if (c1 == c2 || (ignoreCase && (Character.toLowerCase(c1) == Character.toLowerCase(c2)))) {
        continue;
      }
      return null;
    }
    if (index2 != mapping.length()) {
      return null;
    }
    else if (index1 == requestUri.length()) {
      return "";
    }
    else if (requestUri.charAt(index1) == ';') {
      index1 = requestUri.indexOf('/', index1);
    }
    return (index1 != -1 ? requestUri.substring(index1) : "");
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
   * Return the request URI for the given request, detecting an include request
   * URL if called within a RequestDispatcher include.
   * <p>As the value returned by {@code request.getRequestURI()} is <i>not</i>
   * decoded by the servlet container, this method will decode it.
   * <p>The URI that the web container resolves <i>should</i> be correct, but some
   * containers like JBoss/Jetty incorrectly include ";" strings like ";jsessionid"
   * in the URI. This method cuts off such incorrect appendices.
   *
   * @param request current HTTP request
   * @return the request URI
   */
  public String getRequestUri(RequestContext request) {
    String uri = request.getRequestPath();
    return decodeAndCleanUriString(request, uri);
  }

  /**
   * Return the context path for the given request, detecting an include request
   * URL if called within a RequestDispatcher include.
   * <p>As the value returned by {@code request.getContextPath()} is <i>not</i>
   * decoded by the servlet container, this method will decode it.
   *
   * @param request current HTTP request
   * @return the context path
   */
  public String getContextPath(RequestContext request) {
    String contextPath = request.getContextPath();
    if (StringUtils.matchesCharacter(contextPath, '/')) {
      // Invalid case, but happens for includes on Jetty: silently adapt it.
      contextPath = "";
    }
    return decodeRequestString(request, contextPath);
  }

  /**
   * Return the request URI for the given request. If this is a forwarded request,
   * correctly resolves to the request URI of the original request.
   */
  public String getOriginatingRequestUri(RequestContext request) {
    String uri = request.getRequestPath();
    return decodeAndCleanUriString(request, uri);
  }

  /**
   * Decode the supplied URI string and strips any extraneous portion after a ';'.
   */
  private String decodeAndCleanUriString(RequestContext request, String uri) {
    uri = removeSemicolonContent(uri);
    uri = decodeRequestString(request, uri);
    uri = getSanitizedPath(uri);
    return uri;
  }

  /**
   * Decode the given source string with a URLDecoder. The encoding will be taken
   * from the request, falling back to the default "ISO-8859-1".
   * <p>The default implementation uses {@code URLDecoder.decode(input, enc)}.
   *
   * @param request current HTTP request
   * @param source the String to decode
   * @return the decoded String
   * @see Constant#DEFAULT_ENCODING
   * @see URLDecoder#decode(String, String)
   * @see URLDecoder#decode(String)
   */
  @SuppressWarnings("deprecation")
  public String decodeRequestString(RequestContext request, String source) {
    if (this.urlDecode) {
      return decodeInternal(request, source);
    }
    return source;
  }

  @SuppressWarnings("deprecation")
  private String decodeInternal(RequestContext request, String source) {
    String enc = getDefaultEncoding();
    try {
      return UriUtils.decode(source, enc);
    }
    catch (UnsupportedCharsetException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Could not decode request string [" + source + "] with encoding '" + enc +
                "': falling back to platform default encoding; exception message: " + ex.getMessage());
      }
      return URLDecoder.decode(source);
    }
  }

  /**
   * Remove ";" (semicolon) content from the given request URI if the
   * {@linkplain #setRemoveSemicolonContent removeSemicolonContent}
   * property is set to "true". Note that "jsessionid" is always removed.
   *
   * @param requestUri the request URI string to remove ";" content from
   * @return the updated URI string
   */
  public String removeSemicolonContent(String requestUri) {
    return (this.removeSemicolonContent ?
            removeSemicolonContentInternal(requestUri) : removeJsessionid(requestUri));
  }

  private static String removeSemicolonContentInternal(String requestUri) {
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

  private String removeJsessionid(String requestUri) {
    String key = ";jsessionid=";
    int index = requestUri.toLowerCase().indexOf(key);
    if (index == -1) {
      return requestUri;
    }
    String start = requestUri.substring(0, index);
    for (int i = index + key.length(); i < requestUri.length(); i++) {
      char c = requestUri.charAt(i);
      if (c == ';' || c == '/') {
        return start + requestUri.substring(i);
      }
    }
    return start;
  }

  /**
   * Decode the given URI path variables via {@link #decodeRequestString} unless
   * {@link #setUrlDecode} is set to {@code true} in which case it is assumed
   * the URL path from which the variables were extracted is already decoded
   * through a call to {@link #getLookupPathForRequest(RequestContext)}.
   *
   * @param request current HTTP request
   * @param vars the URI variables extracted from the URL path
   * @return the same Map or a new Map instance
   */
  public Map<String, String> decodePathVariables(RequestContext request, Map<String, String> vars) {
    if (this.urlDecode) {
      return vars;
    }
    else {
      Map<String, String> decodedVars = CollectionUtils.newLinkedHashMap(vars.size());
      for (Map.Entry<String, String> entry : vars.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        decodedVars.put(key, decodeInternal(request, value));
      }
      return decodedVars;
    }
  }

  /**
   * Decode the given matrix variables via {@link #decodeRequestString} unless
   * {@link #setUrlDecode} is set to {@code true} in which case it is assumed
   * the URL path from which the variables were extracted is already decoded
   * through a call to {@link #getLookupPathForRequest(RequestContext)}.
   *
   * @param request current HTTP request context
   * @param vars the URI variables extracted from the URL path
   * @return the same Map or a new Map instance
   */
  public MultiValueMap<String, String> decodeMatrixVariables(
          RequestContext request, MultiValueMap<String, String> vars) {
    if (this.urlDecode) {
      return vars;
    }
    else {
      MultiValueMap<String, String> decodedVars = MultiValueMap.fromLinkedHashMap(vars.size());
      for (Map.Entry<String, List<String>> entry : vars.entrySet()) {
        String key = entry.getKey();
        List<String> values = entry.getValue();
        for (String value : values) {
          decodedVars.add(key, decodeInternal(request, value));
        }
      }
      return decodedVars;
    }
  }

  /**
   * Shared, read-only instance with defaults. The following apply:
   * <ul>
   * <li>{@code alwaysUseFullPath=false}
   * <li>{@code urlDecode=true}
   * <li>{@code removeSemicolon=true}
   * <li>{@code defaultEncoding=}{@link Constant#DEFAULT_ENCODING}
   * </ul>
   */
  public static final UrlPathHelper defaultInstance = new UrlPathHelper();

  static {
    defaultInstance.setReadOnly();
  }

  /**
   * Shared, read-only instance for the full, encoded path. The following apply:
   * <ul>
   * <li>{@code alwaysUseFullPath=true}
   * <li>{@code urlDecode=false}
   * <li>{@code removeSemicolon=false}
   * <li>{@code defaultEncoding=}{@link Constant#DEFAULT_ENCODING}
   * </ul>
   */
  public static final UrlPathHelper rawPathInstance = new UrlPathHelper() {

    @Override
    public String removeSemicolonContent(String requestUri) {
      return requestUri;
    }
  };

  static {
    rawPathInstance.setAlwaysUseFullPath(true);
    rawPathInstance.setUrlDecode(false);
    rawPathInstance.setRemoveSemicolonContent(false);
    rawPathInstance.setReadOnly();
  }

}
