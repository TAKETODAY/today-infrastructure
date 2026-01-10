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

package infra.test.web.reactive.server;

import org.reactivestreams.Publisher;

import java.io.Serial;
import java.net.URI;
import java.util.function.Function;

import infra.core.io.buffer.DataBuffer;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.client.reactive.ClientHttpConnector;
import infra.http.client.reactive.ClientHttpRequest;
import infra.http.client.reactive.ClientHttpResponse;
import infra.http.server.reactive.HttpHandler;
import infra.http.server.reactive.HttpHeadResponseDecorator;
import infra.http.server.reactive.ServerHttpRequest;
import infra.http.server.reactive.ServerHttpResponse;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.mock.http.client.reactive.MockClientHttpRequest;
import infra.mock.http.client.reactive.MockClientHttpResponse;
import infra.mock.http.server.reactive.MockServerHttpRequest;
import infra.mock.http.server.reactive.MockServerHttpResponse;
import infra.util.MultiValueMap;
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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class HttpHandlerConnector implements ClientHttpConnector {

  private static final Logger logger = LoggerFactory.getLogger(HttpHandlerConnector.class);

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
    HttpStatusCode status = response.getStatusCode();
    MockClientHttpResponse clientResponse = new MockClientHttpResponse((status != null) ? status : HttpStatus.OK);
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
   */
  public static final class FailureAfterResponseCompletedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

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
