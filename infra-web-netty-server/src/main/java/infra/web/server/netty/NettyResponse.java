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
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * Response writer for the current exchange.
 * <p>
 * Response contexts depend on this abstraction instead of constructing Netty
 * HTTP/1.x response objects directly, so the HTTP/2 implementation can later
 * write native frames without changing the context implementation.
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
   * HTTP/2 response writer. Still delegates to the HTTP/1.x stream codec for now.
   */
  final class Http2NettyResponse implements NettyResponse {

    private final Channel channel;

    Http2NettyResponse(Channel channel) {
      this.channel = channel;
    }

    @Override
    public void writeHead(HttpVersion version, HttpResponseStatus status, HttpHeaders headers) {
      channel.write(new DefaultHttpResponse(version, status, headers));
    }

    @Override
    public void writeBody(ByteBuf body) {
      channel.writeAndFlush(new DefaultHttpContent(body));
    }

    @Override
    public void writeFile(Object file) {
      channel.writeAndFlush(file);
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

}
