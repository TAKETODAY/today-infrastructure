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

package infra.http.reactive.server;

import org.jspecify.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

import infra.core.io.buffer.DataBuffer;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.util.Assert;
import infra.util.MultiValueMap;
import infra.util.StringUtils;
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

  private final HttpHeaders headers;

  private final Flux<DataBuffer> body;

  private final ServerHttpRequest originalRequest;

  private URI uri;

  private HttpMethod httpMethod;

  private @Nullable String uriPath;

  private @Nullable String contextPath;

  private @Nullable SslInfo sslInfo;

  private @Nullable InetSocketAddress remoteAddress;

  private @Nullable InetSocketAddress localAddress;

  public DefaultServerHttpRequestBuilder(ServerHttpRequest original) {
    Assert.notNull(original, "ServerHttpRequest is required");

    this.uri = original.getURI();
    // Some containers (including Jetty and Netty4) can have an immutable
    // representation of headers. Since mutability is always desirable here,
    // we always create a mutable case-insensitive copy of the original
    // headers by using the basic constructor and addAll.
    this.headers = HttpHeaders.forWritable();
    this.headers.addAll(original.getHeaders());
    this.httpMethod = original.getMethod();
    this.contextPath = original.getPath().contextPath().value();
    this.remoteAddress = original.getRemoteAddress();
    this.body = original.getBody();
    this.originalRequest = original;
  }

  @Override
  public ServerHttpRequest.Builder method(HttpMethod httpMethod) {
    this.httpMethod = httpMethod;
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
  public ServerHttpRequest.Builder contextPath(@Nullable String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  @Override
  public ServerHttpRequest.Builder header(String headerName, String @Nullable ... headerValues) {
    this.headers.setOrRemove(headerName, headerValues);
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
  public ServerHttpRequest.Builder localAddress(InetSocketAddress localAddress) {
    this.localAddress = localAddress;
    return this;
  }

  @Override
  public ServerHttpRequest build() {
    return new MutatedServerHttpRequest(getUriToUse(), this.contextPath, this.httpMethod,
            this.sslInfo, this.remoteAddress, localAddress, this.headers, this.body, this.originalRequest);
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

    private final Flux<DataBuffer> body;

    private final ServerHttpRequest originalRequest;

    private final @Nullable SslInfo sslInfo;

    private final @Nullable InetSocketAddress remoteAddress;

    private final @Nullable InetSocketAddress localAddress;

    public MutatedServerHttpRequest(URI uri, @Nullable String contextPath,
            HttpMethod method, @Nullable SslInfo sslInfo, @Nullable InetSocketAddress remoteAddress,
            @Nullable InetSocketAddress localAddress, HttpHeaders headers, Flux<DataBuffer> body, ServerHttpRequest originalRequest) {
      super(method, uri, contextPath, headers);
      this.localAddress = localAddress;
      this.body = body;
      this.originalRequest = originalRequest;
      this.sslInfo = sslInfo != null ? sslInfo : originalRequest.getSslInfo();
      this.remoteAddress = remoteAddress != null ? remoteAddress : originalRequest.getRemoteAddress();
    }

    @Override
    protected MultiValueMap<String, HttpCookie> initCookies() {
      return this.originalRequest.getCookies();
    }

    @Override
    public @Nullable InetSocketAddress getLocalAddress() {
      return localAddress;
    }

    @Override
    public @Nullable InetSocketAddress getRemoteAddress() {
      return remoteAddress;
    }

    @Override
    protected @Nullable SslInfo initSslInfo() {
      return this.sslInfo;
    }

    @Override
    public Flux<DataBuffer> getBody() {
      return this.body;
    }

    @Override
    public <T> T getNativeRequest() {
      return DelegatingServerHttpRequest.getNativeRequest(this.originalRequest);
    }

    @Override
    public String getId() {
      return this.originalRequest.getId();
    }
  }

}
