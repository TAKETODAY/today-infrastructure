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

package infra.http.client;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;

/**
 * Test handler that answers {@code /redirect} with a redirect and every other
 * request with an echo of the method and request target.
 * <p>
 * Supports both the HTTP/1.x objects produced by {@code HttpServerCodec} and the
 * native HTTP/2 frames delivered on an {@code Http2StreamChannel}, because the
 * HTTP/2 pipeline no longer converts stream frames to HTTP objects.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/2 19:07
 */
public class RedirectHandler extends ChannelInboundHandlerAdapter {

  private static final String REDIRECT_PATH = "/redirect";

  private static final String REDIRECT_LOCATION = "/redirected";

  @Override
  public boolean isSharable() {
    return true;
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    ctx.close();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) {
    if (msg instanceof HttpRequest request) {
      handleHttpRequest(ctx, request);
    }
    else if (msg instanceof Http2HeadersFrame headersFrame) {
      handleHttp2Headers(ctx, headersFrame);
    }
    else if (msg instanceof Http2DataFrame dataFrame) {
      // the response is written as soon as the request headers arrive
      dataFrame.release();
    }
    else {
      ctx.fireChannelRead(msg);
    }
  }

  private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
    if (REDIRECT_PATH.equals(request.uri())) {
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
      response.headers().set(HttpHeaderNames.LOCATION, REDIRECT_LOCATION);
      ctx.writeAndFlush(response)
              .addListener(ChannelFutureListener.CLOSE);
    }
    else {
      ByteBuf body = Unpooled.copiedBuffer(responseBody(request.method().name(), request.uri()));
      DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, body);
      ctx.writeAndFlush(response)
              .addListener(ChannelFutureListener.CLOSE);
    }
  }

  private void handleHttp2Headers(ChannelHandlerContext ctx, Http2HeadersFrame frame) {
    Http2Headers headers = frame.headers();
    CharSequence method = headers.method();
    if (method == null) {
      // trailing headers of an already answered request
      return;
    }

    CharSequence path = headers.path();
    String uri = path != null ? path.toString() : "/";
    if (REDIRECT_PATH.equals(uri)) {
      Http2Headers responseHeaders = new DefaultHttp2Headers()
              .status(HttpResponseStatus.FOUND.codeAsText())
              .set(HttpHeaderNames.LOCATION, REDIRECT_LOCATION);
      ctx.writeAndFlush(new DefaultHttp2HeadersFrame(responseHeaders, true));
    }
    else {
      ByteBuf body = Unpooled.copiedBuffer(responseBody(method.toString(), uri));
      Http2Headers responseHeaders = new DefaultHttp2Headers()
              .status(HttpResponseStatus.OK.codeAsText())
              .setInt(HttpHeaderNames.CONTENT_LENGTH, body.readableBytes());
      ctx.write(new DefaultHttp2HeadersFrame(responseHeaders, false));
      ctx.writeAndFlush(new DefaultHttp2DataFrame(body, true));
    }
  }

  private static byte[] responseBody(String method, String uri) {
    return ("Received " + method + " request to " + uri).getBytes(StandardCharsets.UTF_8);
  }

}
