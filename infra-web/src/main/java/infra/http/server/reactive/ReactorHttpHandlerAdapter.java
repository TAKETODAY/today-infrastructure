/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
