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

import infra.context.ApplicationContext;
import infra.web.DispatcherHandler;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;

/**
 * Internal {@link NettyHttpContext} implementation that handles the lifecycle of an HTTP request
 * within the Netty transport layer. This class manages request body streaming, content length validation,
 * {@code 100 Continue} expectation handling, and delegates request processing to the configured
 * {@link DispatcherHandler}.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class HttpContext extends NettyHttpContext implements Runnable {

  public final HttpRequest request;

  public HttpContext(Channel channel, HttpRequest request, NettyRequestConfig config,
          ApplicationContext context, DispatcherHandler dispatcherHandler, HttpTrafficHandler httpTrafficHandler) {
    super(context, channel, config, dispatcherHandler, httpTrafficHandler,
            getContentLength(request), request.method());
    this.request = request;
    if (request instanceof HttpContent content) {
      onDataReceived(content);
    }
  }

  @Override
  public HttpVersion version() {
    return request.protocolVersion();
  }

  @Override
  protected String requestUri() {
    return request.uri();
  }

  @Override
  protected HttpHeaders nettyRequestHeaders() {
    return request.headers();
  }

  @Override
  protected boolean is100ContinueExpected() {
    return HttpUtil.is100ContinueExpected(request);
  }

  @Override
  protected void removeExpectHeader() {
    request.headers().remove(HttpHeaderNames.EXPECT);
  }

  /**
   * Complete the response, closing the connection when it is not kept alive.
   */
  @Override
  protected void endResponse() {
    if (HttpUtil.isKeepAlive(request)) {
      channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
    }
    else {
      channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
              .addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * Write an informational {@code 100 Continue} response without ending the exchange.
   */
  @Override
  protected void writeResponseContinue() {
    channel.writeAndFlush(new DefaultFullHttpResponse(version(), HttpResponseStatus.CONTINUE,
            Unpooled.EMPTY_BUFFER));
  }

  @Override
  protected void writeRejectResponseContinue() {
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(version(),
            HttpResponseStatus.EXPECTATION_FAILED, Unpooled.EMPTY_BUFFER);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
    channel.writeAndFlush(response);
  }

  @Override
  protected void processRequest() throws Exception {
    if (request.decoderResult().cause() != null) {
      processException(request.decoderResult().cause());
    }
    else {
      dispatcherHandler.handleRequest(this);
    }
  }

  /**
   * Returns the length of the content or the specified default value if the message does not have the {@code
   * "Content-Length" header}. Please note that this value is not retrieved from {@link HttpContent#content()} but
   * from the {@code "Content-Length"} header, and thus they are independent from each other.
   *
   * @param message the message
   * @return the content length or the specified default value
   * @throws NumberFormatException if the {@code "Content-Length"} header does not parse as a long
   */
  static long getContentLength(HttpMessage message) {
    String value = message.headers().get(HttpHeaderNames.CONTENT_LENGTH);
    if (value != null) {
      return Long.parseLong(value);
    }
    return -1L;
  }

}

