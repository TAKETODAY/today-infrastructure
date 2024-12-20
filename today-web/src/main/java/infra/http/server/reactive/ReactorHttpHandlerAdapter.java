/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.http.server.reactive;

import java.net.URISyntaxException;
import java.util.function.BiFunction;

import infra.core.io.buffer.NettyDataBufferFactory;
import infra.http.HttpLogging;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.logging.Logger;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;

/**
 * Adapt {@link HttpHandler} to the Reactor Netty channel handling function.
 *
 * @author Stephane Maldini
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ReactorHttpHandlerAdapter implements BiFunction<HttpServerRequest, HttpServerResponse, Mono<Void>> {
  private static final Logger log = HttpLogging.forLogName(ReactorHttpHandlerAdapter.class);

  private final HttpHandler httpHandler;

  public ReactorHttpHandlerAdapter(HttpHandler httpHandler) {
    Assert.notNull(httpHandler, "HttpHandler is required");
    this.httpHandler = httpHandler;
  }

  @Override
  public Mono<Void> apply(HttpServerRequest reactorRequest, HttpServerResponse reactorResponse) {
    NettyDataBufferFactory bufferFactory = new NettyDataBufferFactory(reactorResponse.alloc());
    try {
      ServerHttpResponse response = new ReactorServerHttpResponse(reactorResponse, bufferFactory);
      ReactorServerHttpRequest request = new ReactorServerHttpRequest(reactorRequest, bufferFactory);

      if (request.getMethod() == HttpMethod.HEAD) {
        response = new HttpHeadResponseDecorator(response);
      }

      if (log.isDebugEnabled()) {
        return httpHandler.handle(request, response)
                .doOnError(ex -> log.trace("{}Failed to complete: {}", request.getLogPrefix(), ex.getMessage()))
                .doOnSuccess(aVoid -> log.trace("{}Handling completed", request.getLogPrefix()));
      }
      return httpHandler.handle(request, response);
    }
    catch (URISyntaxException ex) {
      if (log.isDebugEnabled()) {
        log.debug("Failed to get request URI: {}", ex.getMessage());
      }
      reactorResponse.status(HttpResponseStatus.BAD_REQUEST);
      return Mono.empty();
    }
  }

}
