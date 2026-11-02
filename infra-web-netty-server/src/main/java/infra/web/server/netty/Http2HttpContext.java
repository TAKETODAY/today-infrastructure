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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import infra.context.ApplicationContext;
import infra.http.support.NettyHeaders;
import infra.web.DispatcherHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.DefaultHeaders;
import io.netty.handler.codec.Headers;
import io.netty.handler.codec.HeadersUtils;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataChunkedInput;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.stream.ChunkedInput;

/**
 * {@link NettyHttpContext} backed by native HTTP/2 frames.
 * <p>
 * Request metadata is taken from {@link Http2Headers}; pseudo headers are mapped
 * once into a regular header view so the rest of the context can stay protocol
 * agnostic.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class Http2HttpContext extends NettyHttpContext implements Runnable {

  private static final CharSequence CONTINUE_TOKEN = "100-continue";

  private static final HttpVersion HTTP2 = HttpVersion.valueOf("HTTP/2.0");

  /**
   * Connection-specific headers are forbidden in HTTP/2 and must not be exposed
   * as request headers.
   */
  @SuppressWarnings("deprecation")
  private static final CharSequence[] CONNECTION_SPECIFIC_HEADERS = {
          HttpHeaderNames.CONNECTION,
          HttpHeaderNames.KEEP_ALIVE,
          HttpHeaderNames.PROXY_CONNECTION,
          HttpHeaderNames.TRANSFER_ENCODING,
          HttpHeaderNames.UPGRADE
  };

  private final Http2Headers http2Headers;

  private final String uri;

  Http2HttpContext(Http2StreamChannel channel, Http2Headers http2Headers, NettyRequestConfig config,
          ApplicationContext context, DispatcherHandler dispatcherHandler, HttpTrafficHandler httpTrafficHandler) {
    super(context, channel, config, dispatcherHandler, httpTrafficHandler, contentLength(http2Headers), method(http2Headers),
            new DefaultHttp2Headers(true, true, 16));
    this.http2Headers = http2Headers;

    // expose :authority as the regular host header
    CharSequence authority = http2Headers.authority();
    if (authority != null) {
      http2Headers.set(HttpHeaderNames.HOST, authority);
    }
    // connection-specific headers are forbidden in HTTP/2
    for (CharSequence name : CONNECTION_SPECIFIC_HEADERS) {
      http2Headers.remove(name);
    }

    CharSequence path = http2Headers.path();
    this.uri = path != null ? path.toString() : "/";

    // request metadata is cached above; pseudo headers are not exposed as regular headers
    for (CharSequence name : new ArrayList<>(http2Headers.names())) {
      if (Http2Headers.PseudoHeaderName.isPseudoHeader(name)) {
        http2Headers.remove(name);
      }
    }
  }

  @Override
  public HttpVersion version() {
    return HTTP2;
  }

  @Override
  protected String requestUri() {
    return uri;
  }

  @Override
  public @Nullable String getHeader(String name) {
    return HeadersUtils.getAsString(http2Headers, name);
  }

  @Override
  public List<String> getHeaders(String name) {
    return HeadersUtils.getAllAsString(http2Headers, name);
  }

  @Override
  public boolean containsHeader(String name) {
    return http2Headers.contains(name);
  }

  @Override
  public boolean containsHeaderValue(String headerName, String value) {
    return http2Headers.contains(headerName, value, true);
  }

  @Override
  public Collection<String> getHeaderNames() {
    return HeadersUtils.namesAsString(http2Headers);
  }

  @Override
  protected infra.http.HttpHeaders createRequestHeaders() {
    return new NettyHeaders(http2Headers, true);
  }

  /**
   * HTTP/2 requires lower-case header field names.
   */
  @Override
  protected CharSequence normalizeHeaderName(String name) {
    return name.toLowerCase(Locale.ROOT);
  }

  /**
   * HTTP/2 forbids connection-specific headers such as {@code Transfer-Encoding}.
   */
  @Override
  protected boolean supportsTransferEncoding() {
    return false;
  }

  @Override
  protected infra.http.HttpHeaders createResponseHeaders() {
    return new NettyHeaders(nettyResponseHeaders, true);
  }

  @Override
  protected void writeResponseHead(HttpResponseStatus status, DefaultHeaders<CharSequence, @Nullable CharSequence, ?> headers) {
    Http2Headers http2Headers = (Http2Headers) (Headers<CharSequence, CharSequence, ?>) headers;
    // connection-specific headers are forbidden in HTTP/2
    for (CharSequence name : CONNECTION_SPECIFIC_HEADERS) {
      http2Headers.remove(name);
    }
    http2Headers.status(status.codeAsText());
    channel.write(new DefaultHttp2HeadersFrame(http2Headers, false));
  }

  @Override
  protected void writeResponseBody(ByteBuf body) {
    channel.writeAndFlush(new DefaultHttp2DataFrame(body, false));
  }

  @Override
  protected void endResponse() {
    channel.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, true));
  }

  @Override
  protected boolean is100ContinueExpected() {
    return http2Headers.contains(HttpHeaderNames.EXPECT, CONTINUE_TOKEN, true);
  }

  @Override
  protected void removeExpectHeader() {
    http2Headers.remove(HttpHeaderNames.EXPECT);
  }

  @Override
  protected void writeResponseContinue() {
    Http2Headers headers = new DefaultHttp2Headers(false, 1).status(HttpResponseStatus.CONTINUE.codeAsText());
    channel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
  }

  @Override
  protected void writeRejectResponseContinue() {
    Http2Headers headers = new DefaultHttp2Headers()
            .status(HttpResponseStatus.EXPECTATION_FAILED.codeAsText())
            .setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
    channel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, true))
            .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
  }

  @Override
  protected Object modifyChunkedFile(ChunkedInput<ByteBuf> input) {
    return new Http2DataChunkedInput(input, ((Http2StreamChannel) channel).stream());
  }

  private static HttpMethod method(Http2Headers headers) {
    CharSequence method = headers.method();
    return method != null ? HttpMethod.valueOf(method.toString()) : HttpMethod.GET;
  }

  private static long contentLength(Http2Headers headers) {
    CharSequence value = headers.get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value.toString());
    }
    return -1L;
  }

}
