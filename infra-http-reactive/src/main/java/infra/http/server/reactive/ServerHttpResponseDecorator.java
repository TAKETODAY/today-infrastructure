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

package infra.http.server.reactive;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.util.function.Supplier;

import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferFactory;
import infra.http.HttpMessageDecorator;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ResponseCookie;
import infra.lang.Assert;
import infra.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * Wraps another {@link ServerHttpResponse} and delegates all methods to it.
 * Sub-classes can override specific methods selectively.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ServerHttpResponseDecorator extends HttpMessageDecorator implements ServerHttpResponse {

  private final ServerHttpResponse delegate;

  public ServerHttpResponseDecorator(ServerHttpResponse delegate) {
    super(delegate);
    Assert.notNull(delegate, "Delegate is required");
    this.delegate = delegate;
  }

  @Override
  public ServerHttpResponse delegate() {
    return this.delegate;
  }

  // ServerHttpResponse delegation methods...

  @Override
  public boolean setStatusCode(@Nullable HttpStatus status) {
    return delegate.setStatusCode(status);
  }

  @Nullable
  @Override
  public HttpStatusCode getStatusCode() {
    return delegate.getStatusCode();
  }

  @Override
  public boolean setRawStatusCode(@Nullable Integer value) {
    return delegate.setRawStatusCode(value);
  }

  @Nullable
  @Override
  public Integer getRawStatusCode() {
    return delegate.getRawStatusCode();
  }

  @Override
  public MultiValueMap<String, ResponseCookie> getCookies() {
    return delegate.getCookies();
  }

  @Override
  public void addCookie(ResponseCookie cookie) {
    delegate.addCookie(cookie);
  }

  @Override
  public DataBufferFactory bufferFactory() {
    return delegate.bufferFactory();
  }

  @Override
  public void beforeCommit(Supplier<? extends Mono<Void>> action) {
    delegate.beforeCommit(action);
  }

  @Override
  public boolean isCommitted() {
    return delegate.isCommitted();
  }

  @Override
  public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
    return delegate.writeWith(body);
  }

  @Override
  public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
    return delegate.writeAndFlushWith(body);
  }

  @Override
  public Mono<Void> setComplete() {
    return delegate.setComplete();
  }

  /**
   * Return the native response of the underlying server API, if possible,
   * also unwrapping {@link ServerHttpResponseDecorator} if necessary.
   *
   * @param response the response to check
   * @param <T> the expected native response type
   * @throws IllegalArgumentException if the native response can't be obtained
   */
  public static <T> T getNativeResponse(ServerHttpResponse response) {
    if (response instanceof AbstractServerHttpResponse) {
      return ((AbstractServerHttpResponse) response).getNativeResponse();
    }
    else if (response instanceof ServerHttpResponseDecorator) {
      return getNativeResponse(((ServerHttpResponseDecorator) response).delegate());
    }
    else {
      throw new IllegalArgumentException(
              "Can't find native response in " + response.getClass().getName());
    }
  }

}
