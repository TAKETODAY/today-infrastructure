/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web.server.netty;

import org.jspecify.annotations.Nullable;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;

/**
 * Read-only view of the request line and headers for the current exchange.
 * <p>
 * Request contexts depend on this abstraction instead of a concrete Netty
 * {@link HttpRequest}, so the HTTP/2 path can later feed native
 * {@code Http2HeadersFrame} data without going through the HTTP/1.x object model.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
interface NettyRequest {

  /**
   * Create a request view backed by an HTTP/1.x {@link HttpRequest}.
   */
  static NettyRequest of(HttpRequest request) {
    return new DefaultNettyRequest(request);
  }

  /**
   * Return the request target, including the query string.
   */
  String uri();

  /**
   * Return the request method.
   */
  HttpMethod method();

  /**
   * Return the mutable request headers.
   */
  HttpHeaders headers();

  /**
   * Return whether the connection should be kept alive after the response.
   */
  boolean isKeepAlive();

  /**
   * Return whether the request expects a {@code 100 Continue} response.
   */
  boolean is100ContinueExpected();

  /**
   * Return the decoder failure, or {@code null} when decoding succeeded.
   */
  @Nullable
  Throwable decoderFailure();

  /**
   * Return the declared content length, or {@code -1} when absent.
   */
  long contentLength();

  /**
   * Return the underlying Netty HTTP/1.x request, or {@code null} when the
   * exchange is not backed by the HTTP/1.x object model.
   */
  @Nullable
  HttpRequest unwrap();

  /**
   * Default {@link NettyRequest} implementation backed by an HTTP/1.x request.
   */
  final class DefaultNettyRequest implements NettyRequest {

    private final HttpRequest request;

    DefaultNettyRequest(HttpRequest request) {
      this.request = request;
    }

    @Override
    public String uri() {
      return request.uri();
    }

    @Override
    public HttpMethod method() {
      return request.method();
    }

    @Override
    public HttpHeaders headers() {
      return request.headers();
    }

    @Override
    public boolean isKeepAlive() {
      return HttpUtil.isKeepAlive(request);
    }

    @Override
    public boolean is100ContinueExpected() {
      return HttpUtil.is100ContinueExpected(request);
    }

    @Override
    public @Nullable Throwable decoderFailure() {
      return request.decoderResult().cause();
    }

    @Override
    public long contentLength() {
      String value = request.headers().get(HttpHeaderNames.CONTENT_LENGTH);
      if (value != null) {
        return Long.parseLong(value);
      }
      return -1L;
    }

    @Override
    public HttpRequest unwrap() {
      return request;
    }

  }

}
