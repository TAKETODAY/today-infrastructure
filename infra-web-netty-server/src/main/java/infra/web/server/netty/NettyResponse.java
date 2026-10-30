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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;

import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;

/**
 * Response writer for the current exchange.
 * <p>
 * Response contexts depend on this abstraction instead of constructing Netty
 * HTTP/1.x response objects directly. The HTTP/1.1 implementation writes the
 * HTTP/1.x object model, while the HTTP/2 implementation writes native frames.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
interface NettyResponse {

  /**
   * Create a response writer for the given channel and protocol.
   */
  static NettyResponse of(Channel channel, HttpProtocol protocol) {
    return protocol.isHttp2() ? new Http2NettyResponse(channel) : new Http1NettyResponse(channel);
  }

  /**
   * Write the response headers without flushing.
   */
  void writeHead(HttpVersion version, HttpResponseStatus status, HttpHeaders headers);

  /**
   * Write and flush a response body chunk.
   */
  void writeBody(ByteBuf body);

  /**
   * Write and flush a file or chunked input.
   */
  void writeFile(Object file);

  /**
   * Write an informational {@code 100 Continue} response without ending the stream.
   */
  void writeContinue();

  /**
   * Write an error response, optionally closing the connection or stream.
   */
  void writeError(HttpResponseStatus status, boolean close);

  /**
   * Complete the response, closing the connection when it is not kept alive.
   */
  void end(boolean keepAlive);

  /**
   * HTTP/1.1 response writer.
   */
  final class Http1NettyResponse implements NettyResponse {

    private final Channel channel;

    Http1NettyResponse(Channel channel) {
      this.channel = channel;
    }

    @Override
    public void writeHead(HttpVersion version, HttpResponseStatus status, HttpHeaders headers) {
      channel.write(new DefaultHttpResponse(version, status, headers));
    }

    @Override
    public void writeBody(ByteBuf body) {
      channel.writeAndFlush(body);
    }

    @Override
    public void writeFile(Object file) {
      channel.writeAndFlush(file);
    }

    @Override
    public void writeContinue() {
      channel.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE,
              Unpooled.EMPTY_BUFFER, io.netty.handler.codec.http.DefaultHttpHeadersFactory.headersFactory(), trailersFactory()));
    }

    @Override
    public void writeError(HttpResponseStatus status, boolean close) {
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
              Unpooled.EMPTY_BUFFER, io.netty.handler.codec.http.DefaultHttpHeadersFactory.headersFactory(), trailersFactory());
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
      ChannelFuture future = channel.writeAndFlush(response);
      if (close) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    }

    @Override
    public void end(boolean keepAlive) {
      if (keepAlive) {
        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
      }
      else {
        channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
                .addListener(ChannelFutureListener.CLOSE);
      }
    }

  }

  /**
   * HTTP/2 response writer that emits native frames.
   */
  final class Http2NettyResponse implements NettyResponse {

    private final Channel channel;

    Http2NettyResponse(Channel channel) {
      this.channel = channel;
    }

    @Override
    public void writeHead(HttpVersion version, HttpResponseStatus status, HttpHeaders headers) {
      Http2Headers http2Headers = toHttp2Headers(headers);
      http2Headers.status(status.codeAsText());
      channel.write(new DefaultHttp2HeadersFrame(http2Headers, false));
    }

    @Override
    public void writeBody(ByteBuf body) {
      channel.writeAndFlush(new DefaultHttp2DataFrame(body, false));
    }

    @Override
    public void writeFile(Object file) {
      channel.writeAndFlush(file);
    }

    @Override
    public void writeContinue() {
      Http2Headers headers = new DefaultHttp2Headers().status(HttpResponseStatus.CONTINUE.codeAsText());
      channel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, false));
    }

    @Override
    public void writeError(HttpResponseStatus status, boolean close) {
      Http2Headers headers = new DefaultHttp2Headers()
              .status(status.codeAsText())
              .setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
      ChannelFuture future = channel.writeAndFlush(new DefaultHttp2HeadersFrame(headers, true));
      if (close) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    }

    @Override
    public void end(boolean keepAlive) {
      ChannelFuture future = channel.writeAndFlush(new DefaultHttp2DataFrame(Unpooled.EMPTY_BUFFER, true));
      if (!keepAlive) {
        future.addListener(ChannelFutureListener.CLOSE);
      }
    }

    private static Http2Headers toHttp2Headers(HttpHeaders headers) {
      return HttpConversionUtil.toHttp2Headers(headers, false);
    }

  }

}
