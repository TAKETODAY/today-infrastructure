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

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.function.Consumer;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;
import reactor.core.publisher.Flux;

/**
 * Package-private default implementation of {@link ServerHttpRequest.Builder}.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultServerHttpRequestBuilder implements ServerHttpRequest.Builder {

  private URI uri;

  private final HttpHeaders headers;

  private String httpMethodValue;

  @Nullable
  private String uriPath;

  @Nullable
  private String contextPath;

  @Nullable
  private SslInfo sslInfo;

  @Nullable
  private InetSocketAddress remoteAddress;

  private final Flux<DataBuffer> body;

  private final ServerHttpRequest originalRequest;

  public DefaultServerHttpRequestBuilder(ServerHttpRequest original) {
    Assert.notNull(original, "ServerHttpRequest is required");

    this.uri = original.getURI();
    this.headers = original.getHeaders().asWritable();
    this.httpMethodValue = original.getMethodValue();
    this.contextPath = original.getPath().contextPath().value();
    this.remoteAddress = original.getRemoteAddress();
    this.body = original.getBody();
    this.originalRequest = original;
  }

  @Override
  public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
    this.httpMethodValue = httpMethod.name();
    return this;
  }

  @Override
  public ServerHttpRequest.Builder uri(URI uri) {
    this.uri = uri;
    return this;
  }

  @Override
  public ServerHttpRequest.Builder path(String path) {
    if (!path.startsWith("/")) {
      throw new IllegalArgumentException("The path does not have a leading slash: " + path);
    }
    this.uriPath = path;
    return this;
  }

  @Override
  public ServerHttpRequest.Builder contextPath(String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  @Override
  public ServerHttpRequest.Builder header(String headerName, String... headerValues) {
    this.headers.put(headerName, Arrays.asList(headerValues));
    return this;
  }

  @Override
  public ServerHttpRequest.Builder headers(Consumer<HttpHeaders> headersConsumer) {
    Assert.notNull(headersConsumer, "'headersConsumer' is required");
    headersConsumer.accept(this.headers);
    return this;
  }

  @Override
  public ServerHttpRequest.Builder sslInfo(SslInfo sslInfo) {
    this.sslInfo = sslInfo;
    return this;
  }

  @Override
  public ServerHttpRequest.Builder remoteAddress(InetSocketAddress remoteAddress) {
    this.remoteAddress = remoteAddress;
    return this;
  }

  @Override
  public ServerHttpRequest build() {
    return new MutatedServerHttpRequest(
            getUriToUse(), this.contextPath, this.httpMethodValue,
            this.sslInfo, this.remoteAddress, this.headers, this.body, this.originalRequest);
  }

  private URI getUriToUse() {
    if (this.uriPath == null) {
      return this.uri;
    }

    StringBuilder uriBuilder = new StringBuilder();
    if (this.uri.getScheme() != null) {
      uriBuilder.append(this.uri.getScheme()).append(':');
    }
    if (this.uri.getRawUserInfo() != null || this.uri.getHost() != null) {
      uriBuilder.append("//");
      if (this.uri.getRawUserInfo() != null) {
        uriBuilder.append(this.uri.getRawUserInfo()).append('@');
      }
      if (this.uri.getHost() != null) {
        uriBuilder.append(this.uri.getHost());
      }
      if (this.uri.getPort() != -1) {
        uriBuilder.append(':').append(this.uri.getPort());
      }
    }
    if (StringUtils.isNotEmpty(this.uriPath)) {
      uriBuilder.append(this.uriPath);
    }
    if (this.uri.getRawQuery() != null) {
      uriBuilder.append('?').append(this.uri.getRawQuery());
    }
    if (this.uri.getRawFragment() != null) {
      uriBuilder.append('#').append(this.uri.getRawFragment());
    }
    try {
      return new URI(uriBuilder.toString());
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException("Invalid URI path: \"" + this.uriPath + "\"", ex);
    }
  }

  private static class MutatedServerHttpRequest extends AbstractServerHttpRequest {

    private final String methodValue;
    private final Flux<DataBuffer> body;
    private final ServerHttpRequest originalRequest;

    @Nullable
    private final SslInfo sslInfo;

    @Nullable
    private final InetSocketAddress remoteAddress;

    public MutatedServerHttpRequest(
            URI uri, @Nullable String contextPath,
            String methodValue, @Nullable SslInfo sslInfo, @Nullable InetSocketAddress remoteAddress,
            HttpHeaders headers, Flux<DataBuffer> body, ServerHttpRequest originalRequest) {

      super(uri, contextPath, headers);
      this.methodValue = methodValue;
      this.remoteAddress = (remoteAddress != null ? remoteAddress : originalRequest.getRemoteAddress());
      this.sslInfo = (sslInfo != null ? sslInfo : originalRequest.getSslInfo());
      this.body = body;
      this.originalRequest = originalRequest;
    }

    @Override
    public String getMethodValue() {
      return this.methodValue;
    }

    @Override
    protected MultiValueMap<String, HttpCookie> initCookies() {
      return this.originalRequest.getCookies();
    }

    @Override
    @Nullable
    public InetSocketAddress getLocalAddress() {
      return this.originalRequest.getLocalAddress();
    }

    @Override
    @Nullable
    public InetSocketAddress getRemoteAddress() {
      return this.remoteAddress;
    }

    @Override
    @Nullable
    protected SslInfo initSslInfo() {
      return this.sslInfo;
    }

    @Override
    public Flux<DataBuffer> getBody() {
      return this.body;
    }

    @Override
    public <T> T getNativeRequest() {
      return ServerHttpRequestDecorator.getNativeRequest(this.originalRequest);
    }

    @Override
    public String getId() {
      return this.originalRequest.getId();
    }
  }

}
