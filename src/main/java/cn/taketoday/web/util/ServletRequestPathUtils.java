package cn.taketoday.web.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.MappingMatch;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/18 14:37
 */
public class ServletRequestPathUtils {

  /** Name of Servlet request attribute that holds the parsed {@link RequestPath}. */
  public static final String PATH_ATTRIBUTE = ServletRequestPathUtils.class.getName() + ".PATH";

  /**
   * Parse the {@link HttpServletRequest#getRequestURI() requestURI} to a
   * {@link RequestPath} and save it in the request attribute
   * {@link #PATH_ATTRIBUTE} for subsequent use with
   * {@link cn.taketoday.web.util.pattern.PathPattern parsed patterns}.
   * <p>The returned {@code RequestPath} will have both the contextPath and any
   * servletPath prefix omitted from the {@link RequestPath#pathWithinApplication()
   * pathWithinApplication} it exposes.
   * <p>This method is typically called by the {@code DispatcherServlet} to determine
   * if any {@code HandlerMapping} indicates that it uses parsed patterns.
   * After that the pre-parsed and cached {@code RequestPath} can be accessed
   * through {@link #getParsedRequestPath(ServletRequest)}.
   */
  public static RequestPath parseAndCache(HttpServletRequest request) {
    RequestPath requestPath = ServletRequestPath.parse(request);
    request.setAttribute(PATH_ATTRIBUTE, requestPath);
    return requestPath;
  }

  /**
   * Return a {@link #parseAndCache  previously} parsed and cached {@code RequestPath}.
   *
   * @throws IllegalArgumentException if not found
   */
  public static RequestPath getParsedRequestPath(ServletRequest request) {
    RequestPath path = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
    Assert.notNull(path, "Expected parsed RequestPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
    return path;
  }

  /**
   * Set the cached, parsed {@code RequestPath} to the given value.
   *
   * @param requestPath the value to set to, or if {@code null} the cache
   * value is cleared.
   * @param request the current request
   */
  public static void setParsedRequestPath(@Nullable RequestPath requestPath, ServletRequest request) {
    if (requestPath != null) {
      request.setAttribute(PATH_ATTRIBUTE, requestPath);
    }
    else {
      request.removeAttribute(PATH_ATTRIBUTE);
    }
  }

  /**
   * Check for a {@link #parseAndCache  previously} parsed and cached {@code RequestPath}.
   */
  public static boolean hasParsedRequestPath(ServletRequest request) {
    return (request.getAttribute(PATH_ATTRIBUTE) != null);
  }

  /**
   * Remove the request attribute {@link #PATH_ATTRIBUTE} that holds a
   * {@link #parseAndCache  previously} parsed and cached {@code RequestPath}.
   */
  public static void clearParsedRequestPath(ServletRequest request) {
    request.removeAttribute(PATH_ATTRIBUTE);
  }

  // Methods to select either parsed RequestPath or resolved String lookupPath

  /**
   * Return the {@link UrlPathHelper#resolveAndCacheLookupPath pre-resolved}
   * String lookupPath or the {@link #parseAndCache(HttpServletRequest)
   * pre-parsed} {@code RequestPath}.
   * <p>In Spring MVC, when at least one {@code HandlerMapping} has parsed
   * {@code PathPatterns} enabled, the {@code DispatcherServlet} eagerly parses
   * and caches the {@code RequestPath} and the same can be also done earlier with
   * {@link cn.taketoday.web.filter.ServletRequestPathFilter
   * ServletRequestPathFilter}. In other cases where {@code HandlerMapping}s
   * use String pattern matching with {@code PathMatcher}, the String
   * lookupPath is resolved separately by each {@code HandlerMapping}.
   *
   * @param request the current request
   * @return a String lookupPath or a {@code RequestPath}
   * @throws IllegalArgumentException if neither is available
   */
  public static Object getCachedPath(ServletRequest request) {

    // The RequestPath is pre-parsed if any HandlerMapping uses PathPatterns.
    // The lookupPath is re-resolved or cleared per HandlerMapping.
    // So check for lookupPath first.

    String lookupPath = (String) request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE);
    if (lookupPath != null) {
      return lookupPath;
    }
    RequestPath requestPath = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
    if (requestPath != null) {
      return requestPath.pathWithinApplication();
    }
    throw new IllegalArgumentException(
            "Neither a pre-parsed RequestPath nor a pre-resolved String lookupPath is available.");
  }

  /**
   * Variant of {@link #getCachedPath(ServletRequest)} that returns the path
   * for request mapping as a String.
   * <p>If the cached path is a {@link #parseAndCache(HttpServletRequest)
   * pre-parsed} {@code RequestPath} then the returned String path value is
   * encoded and with path parameters removed.
   * <p>If the cached path is a {@link UrlPathHelper#resolveAndCacheLookupPath
   * pre-resolved} String lookupPath, then the returned String path value
   * depends on how {@link UrlPathHelper} that resolved is configured.
   *
   * @param request the current request
   * @return the full request mapping path as a String
   */
  public static String getCachedPathValue(ServletRequest request) {
    Object path = getCachedPath(request);
    if (path instanceof PathContainer) {
      String value = ((PathContainer) path).value();
      path = UrlPathHelper.defaultInstance.removeSemicolonContent(value);
    }
    return (String) path;
  }

  /**
   * Check for a previously {@link UrlPathHelper#resolveAndCacheLookupPath
   * resolved} String lookupPath or a previously {@link #parseAndCache parsed}
   * {@code RequestPath}.
   *
   * @param request the current request
   * @return whether a pre-resolved or pre-parsed path is available
   */
  public static boolean hasCachedPath(ServletRequest request) {
    return request.getAttribute(PATH_ATTRIBUTE) != null
            || request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) != null;
  }

  /**
   * Simple wrapper around the default {@link RequestPath} implementation that
   * supports a servletPath as an additional prefix to be omitted from
   * {@link #pathWithinApplication()}.
   */
  private static final class ServletRequestPath implements RequestPath {

    private final RequestPath requestPath;
    private final PathContainer contextPath;

    private ServletRequestPath(String rawPath, @Nullable String contextPath, String servletPathPrefix) {
      Assert.notNull(servletPathPrefix, "`servletPathPrefix` is required");
      this.requestPath = RequestPath.parse(rawPath, contextPath + servletPathPrefix);
      this.contextPath = PathContainer.parsePath(StringUtils.hasText(contextPath) ? contextPath : "");
    }

    @Override
    public String value() {
      return this.requestPath.value();
    }

    @Override
    public List<Element> elements() {
      return this.requestPath.elements();
    }

    @Override
    public PathContainer contextPath() {
      return this.contextPath;
    }

    @Override
    public PathContainer pathWithinApplication() {
      return this.requestPath.pathWithinApplication();
    }

    @Override
    public RequestPath modifyContextPath(String contextPath) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      return (this.requestPath.equals(((ServletRequestPath) other).requestPath));
    }

    @Override
    public int hashCode() {
      return this.requestPath.hashCode();
    }

    @Override
    public String toString() {
      return this.requestPath.toString();
    }

    public static RequestPath parse(HttpServletRequest request) {
      String requestUri = (String) request.getAttribute(RequestDispatcher.INCLUDE_REQUEST_URI);
      if (requestUri == null) {
        requestUri = request.getRequestURI();
      }
      String servletPathPrefix = getServletPathPrefix(request);
      if (StringUtils.hasText(servletPathPrefix)) {
        return new ServletRequestPath(requestUri, request.getContextPath(), servletPathPrefix);
      }
      return RequestPath.parse(requestUri, request.getContextPath());
    }
  }

  @Nullable
  static String getServletPathPrefix(HttpServletRequest request) {
    HttpServletMapping mapping = (HttpServletMapping) request.getAttribute(RequestDispatcher.INCLUDE_MAPPING);
    if (mapping == null) {
      mapping = request.getHttpServletMapping();
    }
    MappingMatch match = mapping.getMappingMatch();
    if (!ObjectUtils.nullSafeEquals(match, MappingMatch.PATH)) {
      return null;
    }
    String servletPath = (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
    servletPath = (servletPath != null ? servletPath : request.getServletPath());
    return UriUtils.encodePath(servletPath, StandardCharsets.UTF_8);
  }

}
