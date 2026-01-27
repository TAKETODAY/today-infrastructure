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

import infra.context.ApplicationContext;
import infra.util.concurrent.Awaiter;
import infra.web.DispatcherHandler;
import infra.web.RequestContextHolder;
import infra.web.server.RequestBodySizeExceededException;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;

import static io.netty.handler.codec.http.DefaultHttpHeadersFactory.trailersFactory;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

final class HttpContext extends NettyRequestContext implements Runnable {

  private final HttpTrafficHandler httpTrafficHandler;

  private final long contentLength;

  private long receivedBytes = 0;

  @Nullable
  private volatile BodyInputStream requestBody;

  private volatile boolean readCompleted;

  private volatile boolean continueExpected = false;

  public HttpContext(Channel channel, HttpRequest request, NettyRequestConfig config,
          ApplicationContext context, DispatcherHandler dispatcherHandler, HttpTrafficHandler httpTrafficHandler) {
    super(context, channel, request, config, dispatcherHandler);
    this.httpTrafficHandler = httpTrafficHandler;
    this.contentLength = HttpUtil.getContentLength(request, -1L);
    if (request instanceof HttpContent content) {
      onDataReceived(content);
    }
  }

  public void onDataReceived(HttpContent httpContent) {
    final int chunkSize = httpContent.content().readableBytes();
    final long received = receivedBytes + chunkSize;

    if (received > config.maxContentLength && !continueExpected) {
      httpContent.release();
      readCompleted = true;
      BodyInputStream requestBody = this.requestBody;
      if (requestBody != null) {
        requestBody.onError(new IOException(new RequestBodySizeExceededException(config.maxContentLength)));
      }
      return;
    }

    receivedBytes = received;

    if (httpContent instanceof LastHttpContent) {
      readCompleted = true;
      BodyInputStream inputStream = this.requestBody;
      if (inputStream != null) {
        if (chunkSize > 0) {
          inputStream.onDataReceived(httpContent.content());
        }
        inputStream.onComplete();
      }
      else if (chunkSize > 0) {
        inputStream = requestBody();
        inputStream.onDataReceived(httpContent.content());
        inputStream.onComplete();
      }
    }
    else {
      requestBody().onDataReceived(httpContent.content());
    }
  }

  @Override
  public long getContentLength() {
    return contentLength;
  }

  private BodyInputStream requestBody() {
    BodyInputStream requestBody = this.requestBody;
    if (requestBody == null) {
      synchronized(this) {
        requestBody = this.requestBody;
        if (requestBody == null) {
          if (config.autoRead) {
            requestBody = new BodyInputStream(config.awaiterFactory.apply(this), config.dataReceivedQueueCapacity);
          }
          else {
            channel.config().setAutoRead(false);
            requestBody = new ManualReadingBodyInputStream(config.awaiterFactory.apply(this), config.dataReceivedQueueCapacity);
          }
        }
        this.requestBody = requestBody;
      }
    }
    return requestBody;
  }

  @Override
  protected InputStream createInputStream() {
    if (readCompleted) {
      BodyInputStream requestBody = this.requestBody;
      if (requestBody == null) {
        return InputStream.nullInputStream();
      }
      return requestBody;
    }
    return requestBody();
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
  public void run() {
    RequestContextHolder.set(this);

    try {
      if (HttpUtil.is100ContinueExpected(request)) {
        HttpResponse accept = acceptMessage();
        if (accept == null) {
          // the expectation failed so we refuse the request.
          channel.writeAndFlush(rejectResponse())
                  .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
          return;
        }

        continueExpected = true;
        channel.writeAndFlush(accept)
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        request.headers().remove(HttpHeaderNames.EXPECT);
      }
      else if (contentLength != -1 && contentLength > config.maxContentLength) {
        processException(new RequestBodySizeExceededException(config.maxContentLength));
        return;
      }

      if (request.decoderResult().cause() != null) {
        processException(request.decoderResult().cause());
      }
      else {
        dispatcherHandler.handleRequest(this); // handling HTTP request
      }
    }
    catch (Throwable e) {
      httpTrafficHandler.handleException(channel, e);
    }
    finally {
      RequestContextHolder.cleanup();
    }
  }

  private void cleanup(@Nullable IOException error) {
    BodyInputStream requestBody = this.requestBody;
    if (requestBody != null) {
      if (error != null) {
        requestBody.onError(error);
      }
      requestBody.close();
      this.requestBody = null;
    }

  }

  public void channelInactive() {
    cleanup(new ClosedChannelException());
  }

  /**
   * Produces a {@link HttpResponse} for {@link HttpRequest}s which define an expectation.
   * Returns {@code null} if the request should be rejected. See {@link #rejectResponse()}.
   */
  private @Nullable HttpResponse acceptMessage() {
    Boolean continueExpected = dispatcherHandler.requestContinueExpected(this);
    if (continueExpected != null) {
      if (!continueExpected) {
        return null;
      }
    }
    // fallback to default max content-length check
    else if (contentLength > config.maxContentLength) {
      return null;
    }

    return new DefaultFullHttpResponse(version(), HttpResponseStatus.CONTINUE, Unpooled.EMPTY_BUFFER,
            config.httpHeadersFactory, trailersFactory());
  }

  /**
   * Returns the appropriate 4XX {@link HttpResponse} for the given {@link HttpRequest}.
   */
  private HttpResponse rejectResponse() {
    DefaultFullHttpResponse response = new DefaultFullHttpResponse(version(), HttpResponseStatus.EXPECTATION_FAILED, Unpooled.EMPTY_BUFFER,
            config.httpHeadersFactory, trailersFactory());
    response.headers().set(CONTENT_LENGTH, 0);
    return response;
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

