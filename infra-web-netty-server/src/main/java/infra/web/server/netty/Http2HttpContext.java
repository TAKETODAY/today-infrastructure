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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import infra.context.ApplicationContext;
import infra.http.HttpHeaders;
import infra.http.support.NettyHeaders;
import infra.web.DispatcherHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
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
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameStream;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2StreamChannel;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedWriteHandler;

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

  /**
   * All pseudo headers known to Netty. Cached to avoid cloning the array returned
   * by {@link Http2Headers.PseudoHeaderName#values()} for every request.
   */
  private static final Http2Headers.PseudoHeaderName[] PSEUDO_HEADERS =
          Http2Headers.PseudoHeaderName.values();

  private final Http2Headers http2Headers;

  private final String uri;

  Http2HttpContext(Http2StreamChannel channel, Http2Headers http2Headers, NettyRequestConfig config,
          ApplicationContext context, DispatcherHandler dispatcherHandler, HttpTrafficHandler httpTrafficHandler) {
    super(context, channel, config, dispatcherHandler, httpTrafficHandler, contentLength(http2Headers), method(http2Headers),
            new DefaultHttp2Headers(true, true, 8));
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
    for (Http2Headers.PseudoHeaderName pseudoHeader : PSEUDO_HEADERS) {
      http2Headers.remove(pseudoHeader.value());
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
    return HeadersUtils.getAsString(http2Headers, normalizeHeaderName(name));
  }

  @Override
  public List<String> getHeaders(String name) {
    return HeadersUtils.getAllAsString(http2Headers, normalizeHeaderName(name));
  }

  @Override
  public boolean containsHeader(String name) {
    return http2Headers.contains(normalizeHeaderName(name));
  }

  @Override
  public boolean containsHeaderValue(String headerName, String value) {
    return http2Headers.contains(normalizeHeaderName(headerName), value, true);
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

  /**
   * HTTP/2 stream channels only accept {@code Http2StreamFrame} writes, so files
   * must be sent through a {@link ChunkedWriteHandler} and wrapped into DATA frames.
   */
  @Override
  protected boolean supportsFileRegion() {
    return false;
  }

  @Override
  protected infra.http.HttpHeaders createResponseHeaders() {
    return new NettyHeaders(nettyResponseHeaders, true);
  }

  @Override
  protected <T extends Headers<CharSequence, CharSequence, T>>
  void writeResponseHead(HttpResponseStatus status, DefaultHeaders<CharSequence, @Nullable CharSequence, T> headers) {
    // connection-specific headers are forbidden in HTTP/2
    for (CharSequence name : CONNECTION_SPECIFIC_HEADERS) {
      headers.remove(name);
    }

    headers.set(Http2Headers.PseudoHeaderName.STATUS.value(), status.codeAsText());
    channel.write(new DefaultHttp2HeadersFrame((Http2Headers) headers, false));
  }

  @Override
  protected void writeResponseBody(ByteBuf body) {
    channel.writeAndFlush(new DefaultHttp2DataFrame(body, false));
  }

  @Override
  protected void endResponse() {
    HttpHeaders trailers = evaluateResponseTrailers();
    if (trailers == null) {
      channel.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, true));
      return;
    }

    Http2Headers http2Trailers = new DefaultHttp2Headers();
    for (var entry : trailers.entries()) {
      for (String value : entry.getValue()) {
        http2Trailers.add(normalizeHeaderName(entry.getKey()), value);
      }
    }
    // trailer HEADERS carry no pseudo headers and end the stream
    channel.writeAndFlush(new DefaultHttp2HeadersFrame(http2Trailers, true));
  }

  @Override
  protected boolean is100ContinueExpected() {
    return http2Headers.contains(HttpHeaderNames.EXPECT, HttpHeaders.CONTINUE, true);
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
    return new Http2DataFrameChunkedInput(input, ((Http2StreamChannel) channel).stream());
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

  /**
   * Adapts file chunks to HTTP/2 DATA frames without ending the stream.
   * <p>Netty's {@code Http2DataChunkedInput} marks the last DATA frame with
   * {@code endStream=true}, which would require {@link #endResponse()} to skip
   * its own terminator. Keeping the terminator in {@code endResponse()} lets
   * file and in-memory responses complete through the same path.
   */
  private static final class Http2DataFrameChunkedInput implements ChunkedInput<Http2DataFrame> {

    private final ChunkedInput<@Nullable ByteBuf> input;

    private final Http2FrameStream stream;

    Http2DataFrameChunkedInput(ChunkedInput<ByteBuf> input, Http2FrameStream stream) {
      this.input = input;
      this.stream = stream;
    }

    @Override
    public boolean isEndOfInput() throws Exception {
      return input.isEndOfInput();
    }

    @Override
    public void close() throws Exception {
      input.close();
    }

    @Override
    @Deprecated
    public @Nullable Http2DataFrame readChunk(ChannelHandlerContext ctx) throws Exception {
      return readChunk(ctx.alloc());
    }

    @Override
    public @Nullable Http2DataFrame readChunk(ByteBufAllocator allocator) throws Exception {
      ByteBuf buffer = input.readChunk(allocator);
      return buffer != null ? new DefaultHttp2DataFrame(buffer, false).stream(stream) : null;
    }

    @Override
    public long length() {
      return input.length();
    }

    @Override
    public long progress() {
      return input.progress();
    }
  }

}
