/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.net.URISyntaxException;
import java.util.function.BiFunction;

import cn.taketoday.core.io.buffer.Netty5DataBufferFactory;
import cn.taketoday.http.HttpLogging;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import io.netty5.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty5.http.server.HttpServerRequest;
import reactor.netty5.http.server.HttpServerResponse;

/**
 * Adapt {@link HttpHandler} to the Reactor Netty 5 channel handling function.
 *
 * <p>This class is based on {@link ReactorHttpHandlerAdapter}.
 *
 * @author Violeta Georgieva
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorNetty2HttpHandlerAdapter implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {

  private static final Logger logger = HttpLogging.forLogName(ReactorNetty2HttpHandlerAdapter.class);

  private final HttpHandler httpHandler;

  public ReactorNetty2HttpHandlerAdapter(HttpHandler httpHandler) {
    Assert.notNull(httpHandler, "HttpHandler must not be null");
    this.httpHandler = httpHandler;
  }

  @Override
  public Mono<Void> apply(HttpServerRequest reactorRequest, HttpServerResponse reactorResponse) {
    Netty5DataBufferFactory bufferFactory = new Netty5DataBufferFactory(reactorResponse.alloc());
    try {
      ReactorNetty2ServerHttpRequest request = new ReactorNetty2ServerHttpRequest(reactorRequest, bufferFactory);
      ServerHttpResponse response = new ReactorNetty2ServerHttpResponse(reactorResponse, bufferFactory);

      if (request.getMethod() == HttpMethod.HEAD) {
        response = new HttpHeadResponseDecorator(response);
      }

      return this.httpHandler.handle(request, response)
              .doOnError(ex -> logger.trace(request.getLogPrefix() + "Failed to complete: " + ex.getMessage()))
              .doOnSuccess(aVoid -> logger.trace(request.getLogPrefix() + "Handling completed"));
    }
    catch (URISyntaxException ex) {
      if (logger.isDebugEnabled()) {
        logger.debug("Failed to get request URI: " + ex.getMessage());
      }
      reactorResponse.status(HttpResponseStatus.BAD_REQUEST);
      return Mono.empty();
    }
  }

}
