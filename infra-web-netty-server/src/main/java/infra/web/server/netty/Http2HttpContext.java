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

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import infra.context.ApplicationContext;
import infra.http.support.Netty4HttpHeaders;
import infra.util.concurrent.Awaiter;
import infra.web.DispatcherHandler;
import infra.web.server.RequestBodySizeExceededException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.HeadersUtils;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataChunkedInput;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.stream.ChunkedInput;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.HEAD;

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

  private volatile @Nullable HttpHeaders headers;

  private final String uri;

  private final HttpMethod method;

  private final long contentLength;

  private final @Nullable BodyInputStream requestBody;

  private long receivedBytes = 0;

  Http2HttpContext(Http2StreamChannel channel, Http2Headers http2Headers,
          NettyRequestConfig config, ApplicationContext context, DispatcherHandler dispatcherHandler,
          HttpTrafficHandler httpTrafficHandler) {
    super(context, channel, config, dispatcherHandler, httpTrafficHandler);
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
    CharSequence method = http2Headers.method();
    this.method = method != null ? HttpMethod.valueOf(method.toString()) : HttpMethod.GET;
    this.contentLength = contentLength(http2Headers);
    this.requestBody = (contentLength == 0L || (contentLength == -1L && (this.method == GET || this.method == HEAD)))
            ? null : createRequestBody();
  }

  private BodyInputStream createRequestBody() {
    Awaiter awaiter = config.awaiterFactory.apply(this);
    if (config.autoRead) {
      return new BodyInputStream(awaiter, config.dataReceivedQueueCapacity);
    }
    channel.config().setAutoRead(false);
    return new ManualReadingBodyInputStream(awaiter, config.dataReceivedQueueCapacity);
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
  protected String readMethod() {
    return method.name();
  }

  @Override
  protected HttpHeaders nettyRequestHeaders() {
    HttpHeaders headers = this.headers;
    if (headers == null) {
      headers = createHeaders();
      this.headers = headers;
    }
    return headers;
  }

  /**
   * Copy regular headers lazily, only needed when the generic
   * {@code requestHeaders()} view is requested.
   */
  private HttpHeaders createHeaders() {
    HttpHeaders headers = new DefaultHttpHeaders(false);
    for (var entry : http2Headers) {
      CharSequence name = entry.getKey();
      if (!Http2Headers.PseudoHeaderName.isPseudoHeader(name)) {
        headers.add(name, entry.getValue());
      }
    }
    return headers;
  }

  @Override
  public @Nullable String getHeader(String name) {
    CharSequence value = http2Headers.get(name);
    return value != null ? value.toString() : null;
  }

  @Override
  public List<String> getHeaders(String name) {
    List<CharSequence> values = http2Headers.getAll(name);
    if (values.isEmpty()) {
      return List.of();
    }
    List<String> result = new ArrayList<>(values.size());
    for (CharSequence value : values) {
      result.add(value.toString());
    }
    return result;
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
    return new Netty4HttpHeaders(nettyRequestHeaders());
  }

  @Override
  public long getContentLength() {
    return contentLength;
  }

  @Override
  protected InputStream createInputStream() {
    return requestBody != null ? requestBody : InputStream.nullInputStream();
  }

  /**
   * Consume a request body chunk. Ownership of {@code data} is transferred to the
   * request body stream, or released when the chunk is rejected or empty.
   */
  @Override
  protected void onDataReceived(ByteBuf data, boolean last) {
    final int chunkSize = data.readableBytes();
    final long received = receivedBytes + chunkSize;

    if (received > config.maxContentLength && !continueExpected) {
      data.release();
      if (requestBody != null) {
        requestBody.onError(new IOException(new RequestBodySizeExceededException(config.maxContentLength)));
      }
      return;
    }

    receivedBytes = received;

    if (requestBody != null) {
      if (chunkSize > 0) {
        requestBody.onDataReceived(data);
      }
      else {
        data.release();
      }
      if (last) {
        requestBody.onComplete();
      }
    }
    else {
      data.release();
    }
  }

  @Override
  public void onError(IOException error) {
    if (requestBody != null) {
      requestBody.onError(error);
    }
  }

  @Override
  protected void writeResponseHead(HttpResponseStatus status, HttpHeaders headers) {
    Http2Headers http2Headers = HttpConversionUtil.toHttp2Headers(headers, false);
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
  protected void requestCompletedInternal(@Nullable Throwable notHandled) {
    if (!config.autoRead) {
      channel.config().setAutoRead(true);
    }
    cleanup(null);
    super.requestCompletedInternal(notHandled);
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

  @Override
  protected void onChannelInactive() {
    cleanup(new ClosedChannelException());
  }

  private void cleanup(@Nullable IOException error) {
    if (requestBody != null) {
      if (error != null) {
        requestBody.onError(error);
      }
      requestBody.close();
    }
  }

  private static long contentLength(Http2Headers headers) {
    CharSequence value = headers.get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value.toString());
    }
    return -1L;
  }

  private final class ManualReadingBodyInputStream extends BodyInputStream {

    private ManualReadingBodyInputStream(Awaiter awaiter, int capacity) {
      super(awaiter, capacity);
    }

    @Override
    protected void requestNext() {
      channel.read();
    }

  }

}
