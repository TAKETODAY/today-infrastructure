package cn.taketoday.web.util;

import cn.taketoday.core.PathMatcher;
import cn.taketoday.http.server.PathContainer;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.pattern.PathPattern;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class to assist with preparation and access to the lookup path for
 * request mapping purposes. This can be the parsed {@link RequestPath}
 * representation of the path when use of {@link PathPattern parsed patterns}
 * is enabled or a String path for use with a {@link PathMatcher} otherwise.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/18 14:37
 */
public class RequestPathUtils {

  /** Name of Servlet request attribute that holds the parsed {@link RequestPath}. */
  public static final String PATH_ATTRIBUTE = RequestPathUtils.class.getName() + ".PATH";

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
   * through {@link #getParsedRequestPath(RequestContext)}.
   */
  public static RequestPath parseAndCache(RequestContext request) {
    RequestPath requestPath = parse(request);
    request.setAttribute(PATH_ATTRIBUTE, requestPath);
    return requestPath;
  }

  /**
   * Return a {@link #parseAndCache  previously} parsed and cached {@code RequestPath}.
   *
   * @throws IllegalArgumentException if not found
   */
  public static RequestPath getParsedRequestPath(RequestContext request) {
    RequestPath path = (RequestPath) request.getAttribute(PATH_ATTRIBUTE);
    if (path == null) {
      throw new IllegalArgumentException(
              "Expected parsed RequestPath in request attribute \"" + PATH_ATTRIBUTE + "\".");
    }
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
    return request.getAttribute(PATH_ATTRIBUTE) != null;
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
   * String lookupPath or the {@link #parseAndCache(RequestContext)
   * pre-parsed} {@code RequestPath}.
   *
   * @param request the current request
   * @return a String lookupPath or a {@code RequestPath}
   * @throws IllegalArgumentException if neither is available
   */
  public static Object getCachedPath(RequestContext request) {
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
   * Variant of {@link #getCachedPath(RequestContext)} that returns the path
   * for request mapping as a String.
   * <p>If the cached path is a {@link #parseAndCache(RequestContext)
   * pre-parsed} {@code RequestPath} then the returned String path value is
   * encoded and with path parameters removed.
   * <p>If the cached path is a {@link UrlPathHelper#resolveAndCacheLookupPath
   * pre-resolved} String lookupPath, then the returned String path value
   * depends on how {@link UrlPathHelper} that resolved is configured.
   *
   * @param request the current request
   * @return the full request mapping path as a String
   */
  public static String getCachedPathValue(RequestContext request) {
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
  public static boolean hasCachedPath(RequestContext request) {
    return request.getAttribute(PATH_ATTRIBUTE) != null
            || request.getAttribute(UrlPathHelper.PATH_ATTRIBUTE) != null;
  }

  public static RequestPath parse(RequestContext request) {
    String requestUri = request.getRequestPath();
    return RequestPath.parse(requestUri, request.getContextPath());
  }

}
