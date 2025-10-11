/*
 * Copyright 2017 - 2025 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.server.support;

import org.jspecify.annotations.Nullable;

import infra.context.ApplicationContext;
import infra.http.HttpMethod;
import infra.http.MediaType;
import infra.util.StringUtils;
import infra.util.concurrent.Awaiter;
import infra.web.DispatcherHandler;
import infra.web.RequestContextHolder;
import io.netty.channel.Channel;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.TooLongHttpContentException;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostStandardRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpPostRequestDecoder;

final class HttpContext extends NettyRequestContext implements Runnable {

  private final NettyChannelHandler channelHandler;

  private final Awaiter awaiter;

  private final long contentLength;

  private long receivedBytes = 0;

  @Nullable
  private volatile BodyInputStream requestBody;

  @Nullable
  private Boolean formURLEncoded;

  @Nullable
  private InterfaceHttpPostRequestDecoder requestDecoder;

  public HttpContext(Channel channel, HttpRequest request, NettyRequestConfig config,
          ApplicationContext context, DispatcherHandler dispatcherHandler, NettyChannelHandler channelHandler) {
    super(context, channel, request, config, dispatcherHandler);
    this.channelHandler = channelHandler;
    this.contentLength = HttpUtil.getContentLength(request, -1L);
    if (contentLength != -1 && contentLength > config.maxContentLength) {
      // todo handle maxContentLength
      request.setDecoderResult(DecoderResult.failure(new TooLongHttpContentException(
              String.format("Content length exceeded '%d' bytes", config.maxContentLength))));
//      processException(request.decoderResult().cause());
//      throw new TooLongHttpContentException(String.format("Content length exceeded '%d' bytes", config.maxContentLength));
    }
    this.awaiter = config.awaiterFactory.apply(this);
  }

  public void onDataReceived(HttpContent httpContent) {
    long currentBytes = receivedBytes;
    int bufferSize = httpContent.content().readableBytes();
    if (currentBytes + bufferSize > config.maxContentLength) {
      httpContent.release();
      BodyInputStream requestBody = this.requestBody;
      if (requestBody != null) {
        requestBody.onError(new TooLongHttpContentException(String.format("Content length exceeded %d bytes", config.maxContentLength)));
      }
      return;
    }

    receivedBytes += bufferSize;

    if (isMultipart() || isFormURLEncoded()) {
      requestDecoderInternal().offer(httpContent);
      if (httpContent instanceof LastHttpContent) {
        awaiter.resume();
      }
    }
    else {
      if (httpContent instanceof LastHttpContent) {
        BodyInputStream inputStream = this.requestBody;
        if (inputStream != null) {
          if (bufferSize > 0) {
            inputStream.onDataReceived(httpContent.content());
          }
          inputStream.onComplete();
        }
        else if (bufferSize > 0) {
          inputStream = requestBody();
          inputStream.onDataReceived(httpContent.content());
          inputStream.onComplete();
        }
      }
      else {
        requestBody().onDataReceived(httpContent.content());
      }
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
          requestBody = new BodyInputStream(awaiter);
        }
        this.requestBody = requestBody;
      }
    }
    return requestBody;
  }

  @Override
  protected BodyInputStream createInputStream() {
    return requestBody();
  }

  @Override
  protected boolean isFormURLEncoded() {
    if (formURLEncoded == null) {
      HttpMethod method = getMethod();
      formURLEncoded = method != HttpMethod.GET && method != HttpMethod.HEAD
              && StringUtils.startsWithIgnoreCase(getContentType(), MediaType.APPLICATION_FORM_URLENCODED_VALUE);
    }
    return formURLEncoded;
  }

  private InterfaceHttpPostRequestDecoder requestDecoderInternal() {
    InterfaceHttpPostRequestDecoder requestDecoder = this.requestDecoder;
    if (requestDecoder == null) {
      if (isMultipart()) {
        requestDecoder = new HttpPostMultipartRequestDecoder(config.httpDataFactory, request, config.postRequestDecoderCharset);
      }
      else {
        requestDecoder = new HttpPostStandardRequestDecoder(config.httpDataFactory, request, config.postRequestDecoderCharset);
      }
      this.requestDecoder = requestDecoder;
    }
    return requestDecoder;
  }

  @Override
  protected InterfaceHttpPostRequestDecoder requestDecoder() {
    awaiter.await();
    return requestDecoderInternal();
  }

  @Override
  protected void requestCompletedInternal(@Nullable Throwable notHandled) {
    cleanup();
    super.requestCompletedInternal(notHandled);
  }

  @Override
  public void run() {
    RequestContextHolder.set(this);
    try {
      if (request.decoderResult().cause() != null) {
        processException(request.decoderResult().cause());
      }
      else {
        dispatcherHandler.handleRequest(this); // handling HTTP request
      }
    }
    catch (Throwable e) {
      channelHandler.handleException(channel, e);
    }
    finally {
      RequestContextHolder.cleanup();
    }
  }

  private void cleanup() {
    BodyInputStream requestBody = this.requestBody;
    if (requestBody != null) {
      requestBody.close();
      this.requestBody = null;
    }

    if (requestDecoder != null) {
      requestDecoder.destroy();
      requestDecoder = null;
    }
  }

  public void channelInactive() {
    cleanup();
  }

}

