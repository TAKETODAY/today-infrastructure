/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.test.web.reactive.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;

import java.net.URI;
import java.util.function.Function;

import cn.taketoday.core.io.buffer.DataBuffer;
import cn.taketoday.http.HttpCookie;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.reactive.ClientHttpConnector;
import cn.taketoday.http.client.reactive.ClientHttpRequest;
import cn.taketoday.http.client.reactive.ClientHttpResponse;
import cn.taketoday.http.server.reactive.HttpHandler;
import cn.taketoday.http.server.reactive.HttpHeadResponseDecorator;
import cn.taketoday.http.server.reactive.ServerHttpRequest;
import cn.taketoday.http.server.reactive.ServerHttpResponse;
import cn.taketoday.lang.Assert;
import cn.taketoday.mock.http.client.reactive.MockClientHttpRequest;
import cn.taketoday.mock.http.client.reactive.MockClientHttpResponse;
import cn.taketoday.mock.http.server.reactive.MockServerHttpRequest;
import cn.taketoday.mock.http.server.reactive.MockServerHttpResponse;
import cn.taketoday.core.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

/**
 * Connector that handles requests by invoking an {@link HttpHandler} rather
 * than making actual requests to a network socket.
 *
 * <p>Internally the connector uses and adapts<br>
 * {@link MockClientHttpRequest} and {@link MockClientHttpResponse} to<br>
 * {@link MockServerHttpRequest} and {@link MockServerHttpResponse}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HttpHandlerConnector implements ClientHttpConnector {

  private static final Log logger = LogFactory.getLog(HttpHandlerConnector.class);

  private final HttpHandler handler;

  /**
   * Constructor with the {@link HttpHandler} to handle requests with.
   */
  public HttpHandlerConnector(HttpHandler handler) {
    Assert.notNull(handler, "HttpHandler is required");
    this.handler = handler;
  }

  @Override
  public Mono<ClientHttpResponse> connect(HttpMethod httpMethod, URI uri,
          Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    return Mono.defer(() -> doConnect(httpMethod, uri, requestCallback))
            .subscribeOn(Schedulers.parallel());
  }

  private Mono<ClientHttpResponse> doConnect(
          HttpMethod httpMethod, URI uri, Function<? super ClientHttpRequest, Mono<Void>> requestCallback) {

    // unsafe(): we're intercepting, already serialized Publisher signals
    Sinks.Empty<Void> requestWriteSink = Sinks.unsafe().empty();
    Sinks.Empty<Void> handlerSink = Sinks.unsafe().empty();
    ClientHttpResponse[] savedResponse = new ClientHttpResponse[1];

    MockClientHttpRequest mockClientRequest = new MockClientHttpRequest(httpMethod, uri);
    MockServerHttpResponse mockServerResponse = new MockServerHttpResponse();

    mockClientRequest.setWriteHandler(requestBody -> {
      log("Invoking HttpHandler for ", httpMethod, uri);
      ServerHttpRequest mockServerRequest = adaptRequest(mockClientRequest, requestBody);
      ServerHttpResponse responseToUse = prepareResponse(mockServerResponse, mockServerRequest);
      this.handler.handle(mockServerRequest, responseToUse).subscribe(
              aVoid -> { },
              handlerSink::tryEmitError,  // Ignore result: signals cannot compete
              handlerSink::tryEmitEmpty);
      return Mono.empty();
    });

    mockServerResponse.setWriteHandler(responseBody ->
            Mono.fromRunnable(() -> {
              log("Creating client response for ", httpMethod, uri);
              savedResponse[0] = adaptResponse(mockServerResponse, responseBody);
            }));

    log("Writing client request for ", httpMethod, uri);
    requestCallback.apply(mockClientRequest).subscribe(
            aVoid -> { },
            requestWriteSink::tryEmitError,  // Ignore result: signals cannot compete
            requestWriteSink::tryEmitEmpty);

    return Mono.when(requestWriteSink.asMono(), handlerSink.asMono())
            .onErrorMap(ex -> {
              ClientHttpResponse response = savedResponse[0];
              return response != null ? new FailureAfterResponseCompletedException(response, ex) : ex;
            })
            .then(Mono.fromCallable(() -> savedResponse[0] != null ?
                                          savedResponse[0] : adaptResponse(mockServerResponse, Flux.empty())));
  }

  private void log(String message, HttpMethod httpMethod, URI uri) {
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("%s %s \"%s\"", message, httpMethod, uri));
    }
  }

  private ServerHttpRequest adaptRequest(MockClientHttpRequest request, Publisher<DataBuffer> body) {
    HttpMethod method = request.getMethod();
    URI uri = request.getURI();
    HttpHeaders headers = request.getHeaders();
    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
    return MockServerHttpRequest.method(method, uri).headers(headers).cookies(cookies).body(body);
  }

  private ServerHttpResponse prepareResponse(ServerHttpResponse response, ServerHttpRequest request) {
    return (request.getMethod() == HttpMethod.HEAD ? new HttpHeadResponseDecorator(response) : response);
  }

  private ClientHttpResponse adaptResponse(MockServerHttpResponse response, Flux<DataBuffer> body) {
    Integer status = response.getRawStatusCode();
    MockClientHttpResponse clientResponse = new MockClientHttpResponse((status != null) ? status : 200);
    clientResponse.getHeaders().putAll(response.getHeaders());
    clientResponse.getCookies().putAll(response.getCookies());
    clientResponse.setBody(body);
    return clientResponse;
  }

  /**
   * Indicates that an error occurred after the server response was completed,
   * via {@link ServerHttpResponse#writeWith} or {@link ServerHttpResponse#setComplete()},
   * and can no longer be changed. This exception wraps the error and also
   * provides {@link #getCompletedResponse() access} to the response.
   * <p>What happens on an actual running server depends on when the server
   * commits the response and the error may or may not change the response.
   * Therefore in tests without a server the exception is wrapped and allowed
   * to propagate so the application is alerted.
   *
   * @since 4.0
   */
  @SuppressWarnings("serial")
  public static final class FailureAfterResponseCompletedException extends RuntimeException {

    private final ClientHttpResponse completedResponse;

    private FailureAfterResponseCompletedException(ClientHttpResponse response, Throwable cause) {
      super("Error occurred after response was completed: " + response, cause);
      this.completedResponse = response;
    }

    public ClientHttpResponse getCompletedResponse() {
      return this.completedResponse;
    }
  }

}
