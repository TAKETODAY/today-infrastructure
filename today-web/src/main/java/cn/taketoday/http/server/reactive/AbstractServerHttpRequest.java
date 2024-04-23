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

package cn.taketoday.http.server.reactive;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.server.RequestPath;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Common base class for {@link ServerHttpRequest} implementations.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class AbstractServerHttpRequest implements ServerHttpRequest {

  private static final Pattern QUERY_PATTERN = Pattern.compile("([^&=]+)(=?)([^&]+)?");

  private final URI uri;

  private final RequestPath path;

  private final HttpHeaders headers;

  @Nullable
  private MultiValueMap<String, String> queryParams;

  @Nullable
  private MultiValueMap<String, HttpCookie> cookies;

  @Nullable
  private SslInfo sslInfo;

  @Nullable
  private String id;

  @Nullable
  private String logPrefix;

  @Nullable
  private HttpMethod method;

  /**
   * Constructor with the URI and headers for the request.
   *
   * @param uri the URI for the request
   * @param contextPath the context path for the request
   * @param headers the headers for the request (as {@link MultiValueMap})
   */
  public AbstractServerHttpRequest(URI uri, @Nullable String contextPath, MultiValueMap<String, String> headers) {
    this.uri = uri;
    this.path = RequestPath.parse(uri, contextPath);
    this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
  }

  /**
   * Constructor with the URI and headers for the request.
   *
   * @param uri the URI for the request
   * @param contextPath the context path for the request
   * @param headers the headers for the request (as {@link HttpHeaders})
   */
  public AbstractServerHttpRequest(URI uri, @Nullable String contextPath, HttpHeaders headers) {
    this.uri = uri;
    this.path = RequestPath.parse(uri, contextPath);
    this.headers = headers.asReadOnly();
  }

  /**
   * Constructor with the method, URI and headers for the request.
   *
   * @param method the HTTP method for the request
   * @param uri the URI for the request
   * @param contextPath the context path for the request
   * @param headers the headers for the request (as {@link MultiValueMap})
   */
  public AbstractServerHttpRequest(HttpMethod method, URI uri,
          @Nullable String contextPath, MultiValueMap<String, String> headers) {

    Assert.notNull(method, "Method is required");
    Assert.notNull(uri, "Uri is required");
    Assert.notNull(headers, "Headers is required");

    this.method = method;
    this.uri = uri;
    this.path = RequestPath.parse(uri, contextPath);
    this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
  }

  @Override
  public String getId() {
    if (this.id == null) {
      this.id = initId();
      if (this.id == null) {
        this.id = ObjectUtils.getIdentityHexString(this);
      }
    }
    return this.id;
  }

  /**
   * Obtain the request id to use, or {@code null} in which case the Object
   * identity of this request instance is used.
   */
  @Nullable
  protected String initId() {
    return null;
  }

  @Override
  public HttpMethod getMethod() {
    HttpMethod method = this.method;
    if (method == null) {
      method = HttpMethod.valueOf(getMethodValue());
      this.method = method;
    }
    return method;
  }

  @Override
  public URI getURI() {
    return this.uri;
  }

  @Override
  public RequestPath getPath() {
    return this.path;
  }

  @Override
  public HttpHeaders getHeaders() {
    return this.headers;
  }

  @Override
  public MultiValueMap<String, String> getQueryParams() {
    if (this.queryParams == null) {
      this.queryParams = initQueryParams();
    }
    return this.queryParams;
  }

  /**
   * A method for parsing of the query into name-value pairs. The return
   * value is turned into an immutable map and cached.
   * <p>Note that this method is invoked lazily on first access to
   * {@link #getQueryParams()}. The invocation is not synchronized but the
   * parsing is thread-safe nevertheless.
   */
  protected MultiValueMap<String, String> initQueryParams() {
    LinkedMultiValueMap<String, String> queryParams = MultiValueMap.forLinkedHashMap();
    String query = getURI().getRawQuery();
    if (query != null) {
      Matcher matcher = QUERY_PATTERN.matcher(query);
      while (matcher.find()) {
        String name = decodeQueryParam(matcher.group(1));
        String eq = matcher.group(2);
        String value = matcher.group(3);
        if (value != null) {
          value = decodeQueryParam(value);
        }
        else if (StringUtils.isNotEmpty(eq)) {
          value = "";
        }
        queryParams.add(name, value);
      }
    }
    return queryParams;
  }

  private String decodeQueryParam(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  @Override
  public MultiValueMap<String, HttpCookie> getCookies() {
    if (this.cookies == null) {
      this.cookies = initCookies();
    }
    return this.cookies;
  }

  /**
   * Obtain the cookies from the underlying "native" request and adapt those to
   * an {@link HttpCookie} map. The return value is turned into an immutable
   * map and cached.
   * <p>Note that this method is invoked lazily on access to
   * {@link #getCookies()}. Sub-classes should synchronize cookie
   * initialization if the underlying "native" request does not provide
   * thread-safe access to cookie data.
   */
  protected abstract MultiValueMap<String, HttpCookie> initCookies();

  @Nullable
  @Override
  public SslInfo getSslInfo() {
    if (this.sslInfo == null) {
      this.sslInfo = initSslInfo();
    }
    return this.sslInfo;
  }

  /**
   * Obtain SSL session information from the underlying "native" request.
   *
   * @return the session information, or {@code null} if none available
   */
  @Nullable
  protected abstract SslInfo initSslInfo();

  /**
   * Return the underlying server response.
   */
  public abstract <T> T getNativeRequest();

  /**
   * For internal use in logging at the HTTP adapter layer.
   */
  String getLogPrefix() {
    if (this.logPrefix == null) {
      this.logPrefix = "[" + initLogPrefix() + "] ";
    }
    return this.logPrefix;
  }

  /**
   * Subclasses can override this to provide the prefix to use for log messages.
   * <p>By default, this is {@link #getId()}.
   */
  protected String initLogPrefix() {
    return getId();
  }

}
