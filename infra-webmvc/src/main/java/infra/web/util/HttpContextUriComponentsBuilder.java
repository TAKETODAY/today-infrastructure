/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.util;

import org.jspecify.annotations.Nullable;

import infra.util.StringUtils;
import infra.web.HttpContext;
import infra.web.HttpContextHolder;
import infra.web.filter.ForwardedHeaderFilter;

/**
 * {@link UriComponentsBuilder} with additional static factory methods to create
 * links based on an {@link HttpContext}.
 *
 * <p>Methods in this class do not extract {@code "Forwarded"} or
 * {@code "X-Forwarded-*"} headers. Use {@link ForwardedHeaderFilter}, or similar
 * support from the underlying server, to extract and use such headers, or to
 * discard them, before invoking these methods.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class HttpContextUriComponentsBuilder extends UriComponentsBuilder {

  private @Nullable String originalPath;

  /**
   * Default constructor. Protected to prevent direct instantiation.
   *
   * @see #fromContextPath(HttpContext)
   * @see #fromRequestUri(HttpContext)
   * @see #fromRequest(HttpContext)
   */
  protected HttpContextUriComponentsBuilder() {
  }

  /**
   * Create a deep copy of the given builder.
   *
   * @param other the other builder to copy from
   */
  protected HttpContextUriComponentsBuilder(HttpContextUriComponentsBuilder other) {
    super(other);
    this.originalPath = other.originalPath;
  }

  /**
   * Prepare a builder from the scheme, host, port, and context path of the request.
   *
   * @param context the request context
   * @return the initialized builder
   */
  public static HttpContextUriComponentsBuilder fromContextPath(HttpContext context) {
    HttpContextUriComponentsBuilder builder = initFromRequest(context);
    builder.replacePath(context.getRequestPath().contextPath().value());
    return builder;
  }

  /**
   * Prepare a builder from the scheme, host, port, and path, but not the query.
   *
   * @param context the request context
   * @return the initialized builder
   */
  public static HttpContextUriComponentsBuilder fromRequestUri(HttpContext context) {
    HttpContextUriComponentsBuilder builder = initFromRequest(context);
    builder.initPath(context.getRequestURI());
    return builder;
  }

  /**
   * Prepare a builder by copying the scheme, host, port, path, and query string
   * of the request. Encoded path and query components are preserved as supplied.
   *
   * @param context the request context
   * @return the initialized builder
   */
  public static HttpContextUriComponentsBuilder fromRequest(HttpContext context) {
    HttpContextUriComponentsBuilder builder = initFromRequest(context);
    builder.initPath(context.getRequestURI());
    builder.query(context.getQueryString());
    return builder;
  }

  private static HttpContextUriComponentsBuilder initFromRequest(HttpContext context) {
    String scheme = context.getScheme();
    String host = context.getServerName();
    int port = context.getServerPort();

    HttpContextUriComponentsBuilder builder = new HttpContextUriComponentsBuilder();
    builder.scheme(scheme);
    builder.host(host);
    if (("http".equals(scheme) && port != 80) || ("https".equals(scheme) && port != 443)) {
      builder.port(port);
    }
    return builder;
  }

  /**
   * Variant of {@link #fromContextPath(HttpContext)} using the current request.
   *
   * @return the initialized builder
   * @throws IllegalStateException if no request context is bound to the thread
   */
  public static HttpContextUriComponentsBuilder fromCurrentContextPath() {
    return fromContextPath(getCurrentRequest());
  }

  /**
   * Variant of {@link #fromRequestUri(HttpContext)} using the current request.
   *
   * @return the initialized builder
   * @throws IllegalStateException if no request context is bound to the thread
   */
  public static HttpContextUriComponentsBuilder fromCurrentRequestUri() {
    return fromRequestUri(getCurrentRequest());
  }

  /**
   * Variant of {@link #fromRequest(HttpContext)} using the current request.
   *
   * @return the initialized builder
   * @throws IllegalStateException if no request context is bound to the thread
   */
  public static HttpContextUriComponentsBuilder fromCurrentRequest() {
    return fromRequest(getCurrentRequest());
  }

  /**
   * Obtain the current request through {@link HttpContextHolder}.
   *
   * @return the current request context
   * @throws IllegalStateException if no request context is bound to the thread
   */
  protected static HttpContext getCurrentRequest() {
    return HttpContextHolder.required();
  }

  private void initPath(String path) {
    this.originalPath = path;
    replacePath(path);
  }

  /**
   * Remove any path extension from the original request URI. Invoke this method
   * before any calls to {@link #path(String)} or {@link #pathSegment(String...)}.
   * <pre>
   * HttpContextUriComponentsBuilder builder = HttpContextUriComponentsBuilder.fromRequestUri(context);
   * String extension = builder.removePathExtension();
   * String uri = builder.path("/pages/1.{ext}").buildAndExpand(extension).toUriString();
   * </pre>
   *
   * @return the removed extension for possible re-use, or {@code null}
   */
  public @Nullable String removePathExtension() {
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
  public HttpContextUriComponentsBuilder cloneBuilder() {
    return new HttpContextUriComponentsBuilder(this);
  }

}
