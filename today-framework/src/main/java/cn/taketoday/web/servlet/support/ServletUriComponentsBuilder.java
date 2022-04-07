/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.servlet.support;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.servlet.ServletUtils;
import cn.taketoday.web.servlet.UrlPathHelper;
import cn.taketoday.web.servlet.filter.ForwardedHeaderFilter;
import cn.taketoday.web.util.UriComponentsBuilder;
import cn.taketoday.web.util.UriUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * UriComponentsBuilder with additional static factory methods to create links
 * based on the current HttpServletRequest.
 *
 * <p><strong>Note:</strong> methods in this class do not extract
 * {@code "Forwarded"} and {@code "X-Forwarded-*"} headers that specify the
 * client-originated address. Please, use {@link ForwardedHeaderFilter
 * ForwardedHeaderFilter}, or similar from the underlying server, to extract
 * and use such headers, or to discard them.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/7 21:30
 */
public class ServletUriComponentsBuilder extends UriComponentsBuilder {

  @Nullable
  private String originalPath;

  /**
   * Default constructor. Protected to prevent direct instantiation.
   *
   * @see #fromContextPath(HttpServletRequest)
   * @see #fromServletMapping(HttpServletRequest)
   * @see #fromRequest(HttpServletRequest)
   * @see #fromCurrentContextPath()
   * @see #fromCurrentServletMapping()
   * @see #fromCurrentRequest()
   */
  protected ServletUriComponentsBuilder() { }

  /**
   * Create a deep copy of the given ServletUriComponentsBuilder.
   *
   * @param other the other builder to copy from
   */
  protected ServletUriComponentsBuilder(ServletUriComponentsBuilder other) {
    super(other);
    this.originalPath = other.originalPath;
  }

  // Factory methods based on an HttpServletRequest

  /**
   * Prepare a builder from the host, port, scheme, and context path of the
   * given HttpServletRequest.
   */
  public static ServletUriComponentsBuilder fromContextPath(HttpServletRequest request) {
    ServletUriComponentsBuilder builder = initFromRequest(request);
    builder.replacePath(request.getContextPath());
    return builder;
  }

  /**
   * Prepare a builder from the host, port, scheme, context path, and
   * servlet mapping of the given HttpServletRequest.
   * <p>If the servlet is mapped by name, e.g. {@code "/main/*"}, the path
   * will end with "/main". If the servlet is mapped otherwise, e.g.
   * {@code "/"} or {@code "*.do"}, the result will be the same as
   * if calling {@link #fromContextPath(HttpServletRequest)}.
   */
  public static ServletUriComponentsBuilder fromServletMapping(HttpServletRequest request) {
    ServletUriComponentsBuilder builder = fromContextPath(request);
    if (StringUtils.hasText(UrlPathHelper.defaultInstance.getPathWithinServletMapping(request))) {
      builder.path(request.getServletPath());
    }
    return builder;
  }

  /**
   * Prepare a builder from the host, port, scheme, and path (but not the query)
   * of the HttpServletRequest.
   */
  public static ServletUriComponentsBuilder fromRequestUri(HttpServletRequest request) {
    ServletUriComponentsBuilder builder = initFromRequest(request);
    builder.initPath(request.getRequestURI());
    return builder;
  }

  /**
   * Prepare a builder by copying the scheme, host, port, path, and
   * query string of an HttpServletRequest.
   */
  public static ServletUriComponentsBuilder fromRequest(HttpServletRequest request) {
    ServletUriComponentsBuilder builder = initFromRequest(request);
    builder.initPath(request.getRequestURI());
    builder.query(request.getQueryString());
    return builder;
  }

  /**
   * Initialize a builder with a scheme, host,and port (but not path and query).
   */
  private static ServletUriComponentsBuilder initFromRequest(HttpServletRequest request) {
    String scheme = request.getScheme();
    String host = request.getServerName();
    int port = request.getServerPort();

    ServletUriComponentsBuilder builder = new ServletUriComponentsBuilder();
    builder.scheme(scheme);
    builder.host(host);
    if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
      builder.port(port);
    }
    return builder;
  }

  // Alternative methods relying on RequestContextHolder to find the request

  /**
   * Same as {@link #fromContextPath(HttpServletRequest)} except the
   * request is obtained through {@link RequestContextHolder}.
   */
  public static ServletUriComponentsBuilder fromCurrentContextPath() {
    return fromContextPath(getCurrentRequest());
  }

  /**
   * Same as {@link #fromServletMapping(HttpServletRequest)} except the
   * request is obtained through {@link RequestContextHolder}.
   */
  public static ServletUriComponentsBuilder fromCurrentServletMapping() {
    return fromServletMapping(getCurrentRequest());
  }

  /**
   * Same as {@link #fromRequestUri(HttpServletRequest)} except the
   * request is obtained through {@link RequestContextHolder}.
   */
  public static ServletUriComponentsBuilder fromCurrentRequestUri() {
    return fromRequestUri(getCurrentRequest());
  }

  /**
   * Same as {@link #fromRequest(HttpServletRequest)} except the
   * request is obtained through {@link RequestContextHolder}.
   */
  public static ServletUriComponentsBuilder fromCurrentRequest() {
    return fromRequest(getCurrentRequest());
  }

  /**
   * Obtain current request through {@link RequestContextHolder}.
   */
  protected static HttpServletRequest getCurrentRequest() {
    return ServletUtils.getServletRequest(RequestContextHolder.get());
  }

  private void initPath(String path) {
    this.originalPath = path;
    replacePath(path);
  }

  /**
   * Remove any path extension from the {@link HttpServletRequest#getRequestURI()
   * requestURI}. This method must be invoked before any calls to {@link #path(String)}
   * or {@link #pathSegment(String...)}.
   * <pre>
   * GET http://www.foo.example/rest/books/6.json
   *
   * ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromRequestUri(this.request);
   * String ext = builder.removePathExtension();
   * String uri = builder.path("/pages/1.{ext}").buildAndExpand(ext).toUriString();
   * assertEquals("http://www.foo.example/rest/books/6/pages/1.json", result);
   * </pre>
   *
   * @return the removed path extension for possible re-use, or {@code null}
   */
  @Nullable
  public String removePathExtension() {
    String extension = null;
    if (this.originalPath != null) {
      extension = UriUtils.extractFileExtension(this.originalPath);
      if (StringUtils.isNotEmpty(extension)) {
        int end = this.originalPath.length() - (extension.length() + 1);
        replacePath(this.originalPath.substring(0, end));
      }
      this.originalPath = null;
    }
    return extension;
  }

  @Override
  public ServletUriComponentsBuilder cloneBuilder() {
    return new ServletUriComponentsBuilder(this);
  }

}
