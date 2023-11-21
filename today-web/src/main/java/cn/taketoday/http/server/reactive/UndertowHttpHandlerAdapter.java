/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.http.server.reactive;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.net.URISyntaxException;

import cn.taketoday.core.io.buffer.DataBufferFactory;
import cn.taketoday.core.io.buffer.DefaultDataBufferFactory;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import io.undertow.server.HttpServerExchange;

/**
 * Adapt {@link HttpHandler} to the Undertow {@link io.undertow.server.HttpHandler}.
 *
 * @author Marek Hawrylczak
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @since 4.0
 */
public class UndertowHttpHandlerAdapter implements io.undertow.server.HttpHandler {
  private static final Logger log = HttpLogging.forLogName(UndertowHttpHandlerAdapter.class);

  private final HttpHandler httpHandler;
  private DataBufferFactory bufferFactory = DefaultDataBufferFactory.sharedInstance;

  public UndertowHttpHandlerAdapter(HttpHandler httpHandler) {
    Assert.notNull(httpHandler, "HttpHandler is required");
    this.httpHandler = httpHandler;
  }

  public void setDataBufferFactory(DataBufferFactory bufferFactory) {
    Assert.notNull(bufferFactory, "DataBufferFactory is required");
    this.bufferFactory = bufferFactory;
  }

  public DataBufferFactory getDataBufferFactory() {
    return this.bufferFactory;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) {
    UndertowServerHttpRequest request;
    try {
      request = new UndertowServerHttpRequest(exchange, getDataBufferFactory());
    }
    catch (URISyntaxException ex) {
      if (log.isWarnEnabled()) {
        log.debug("Failed to get request URI: {}", ex.getMessage());
      }
      exchange.setStatusCode(400);
      return;
    }
    ServerHttpResponse response = new UndertowServerHttpResponse(exchange, getDataBufferFactory(), request);
    if (request.getMethod() == HttpMethod.HEAD) {
      response = new HttpHeadResponseDecorator(response);
    }
    HandlerResultSubscriber resultSubscriber = new HandlerResultSubscriber(exchange, request);
    httpHandler.handle(request, response).subscribe(resultSubscriber);
  }

  private static class HandlerResultSubscriber implements Subscriber<Void> {

    private final String logPrefix;
    private final HttpServerExchange exchange;

    public HandlerResultSubscriber(HttpServerExchange exchange, UndertowServerHttpRequest request) {
      this.exchange = exchange;
      this.logPrefix = request.getLogPrefix();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onNext(Void aVoid) {
      // no-op
    }

    @Override
    public void onError(Throwable ex) {
      if (log.isDebugEnabled()) {
        log.trace("{}Failed to complete: {}", logPrefix, ex.getMessage());
      }
      if (exchange.isResponseStarted()) {
        try {
          if (log.isDebugEnabled()) {
            log.debug("{}Closing connection", logPrefix);
          }
          exchange.getConnection().close();
        }
        catch (IOException ex2) {
          // ignore
        }
      }
      else {
        if (log.isDebugEnabled()) {
          log.debug("{}Setting HttpServerExchange status to 500 Server Error", logPrefix);
        }
        exchange.setStatusCode(500);
        exchange.endExchange();
      }
    }

    @Override
    public void onComplete() {
      if (log.isDebugEnabled()) {
        log.trace("{}Handling completed", logPrefix);
      }
      exchange.endExchange();
    }
  }

}
