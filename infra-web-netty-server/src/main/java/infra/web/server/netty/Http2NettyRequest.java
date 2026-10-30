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

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;

import infra.util.ExceptionUtils;

/**
 * {@link NettyRequest} backed by native HTTP/2 headers. Pseudo headers are mapped
 * once into a regular header view so the rest of the request handling code remains
 * protocol agnostic.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http2NettyRequest implements NettyRequest {

  private static final CharSequence CONTINUE_TOKEN = "100-continue";

  private final HttpHeaders headers;

  private final String uri;

  private final HttpMethod method;

  Http2NettyRequest(Http2Headers http2Headers, int streamId) {
    CharSequence path = http2Headers.path();
    this.uri = path != null ? path.toString() : "/";
    CharSequence method = http2Headers.method();
    this.method = method != null ? HttpMethod.valueOf(method.toString()) : HttpMethod.GET;
    this.headers = new DefaultHttpHeaders(false);
    try {
      HttpConversionUtil.addHttp2ToHttpHeaders(streamId, http2Headers, headers, HttpVersion.HTTP_1_1, false, false);
    }
    catch (Http2Exception ex) {
      throw ExceptionUtils.sneakyThrow(ex);
    }
  }

  @Override
  public String uri() {
    return uri;
  }

  @Override
  public HttpMethod method() {
    return method;
  }

  @Override
  public HttpHeaders headers() {
    return headers;
  }

  @Override
  public boolean isKeepAlive() {
    return true;
  }

  @Override
  public boolean is100ContinueExpected() {
    return headers.contains(HttpHeaderNames.EXPECT, CONTINUE_TOKEN, true);
  }

  @Override
  public @Nullable Throwable decoderFailure() {
    return null;
  }

  @Override
  public long contentLength() {
    CharSequence value = headers.get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value.toString());
    }
    return -1L;
  }

  @Override
  public @Nullable HttpRequest unwrap() {
    return null;
  }

}
