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

package infra.web.client.reactive;

import java.net.URI;

import infra.http.AbstractHttpRequest;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.HttpStatusCode;
import infra.http.client.reactive.ClientHttpConnector;
import infra.http.client.reactive.ClientHttpResponse;
import infra.http.codec.HttpMessageWriter;
import infra.http.codec.LoggingCodecSupport;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.LogFormatUtils;
import reactor.core.publisher.Mono;

/**
 * Static factory methods to create an {@link ExchangeFunction}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ExchangeFunctions {

  private static final Logger log = LoggerFactory.getLogger(ExchangeFunctions.class);

  /**
   * Create an {@code ExchangeFunction} with the given {@code ClientHttpConnector}.
   * This is the same as calling
   * {@link #create(ClientHttpConnector, ExchangeStrategies)} and passing
   * {@link ExchangeStrategies#withDefaults()}.
   *
   * @param connector the connector to use for connecting to servers
   * @return the created {@code ExchangeFunction}
   */
  public static ExchangeFunction create(ClientHttpConnector connector) {
    return create(connector, ExchangeStrategies.withDefaults());
  }

  /**
   * Create an {@code ExchangeFunction} with the given
   * {@code ClientHttpConnector} and {@code ExchangeStrategies}.
   *
   * @param connector the connector to use for connecting to servers
   * @param strategies the {@code ExchangeStrategies} to use
   * @return the created {@code ExchangeFunction}
   */
  public static ExchangeFunction create(ClientHttpConnector connector, ExchangeStrategies strategies) {
    return new DefaultExchangeFunction(connector, strategies);
  }

  private static class DefaultExchangeFunction implements ExchangeFunction {

    private final ClientHttpConnector connector;

    private final ExchangeStrategies strategies;

    private boolean enableLoggingRequestDetails;

    public DefaultExchangeFunction(ClientHttpConnector connector, ExchangeStrategies strategies) {
      Assert.notNull(connector, "ClientHttpConnector is required");
      Assert.notNull(strategies, "ExchangeStrategies is required");
      this.connector = connector;
      this.strategies = strategies;

      for (HttpMessageWriter<?> httpMessageWriter : strategies.messageWriters()) {
        if (httpMessageWriter instanceof LoggingCodecSupport codecSupport) {
          if (codecSupport.isEnableLoggingRequestDetails()) {
            this.enableLoggingRequestDetails = true;
          }
        }
      }
    }

    @Override
    public Mono<ClientResponse> exchange(ClientRequest clientRequest) {
      Assert.notNull(clientRequest, "ClientRequest is required");
      HttpMethod httpMethod = clientRequest.method();
      URI uri = clientRequest.uri();

      var responseMono = connector.connect(
              httpMethod, uri, httpRequest -> clientRequest.writeTo(httpRequest, strategies));

      if (log.isDebugEnabled()) {
        responseMono = responseMono.doOnRequest(n -> logRequest(clientRequest))
                .doOnCancel(() -> log.debug(clientRequest.logPrefix() + "Cancel signal (to close connection)"));
      }

      return responseMono
              .onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, t -> wrapException(t, clientRequest))
              .map(httpResponse -> {
                String logPrefix = getLogPrefix(clientRequest, httpResponse);
                logResponse(httpResponse, logPrefix);
                return new DefaultClientResponse(
                        httpResponse, strategies, logPrefix, httpMethod.name() + " " + uri,
                        () -> createRequest(clientRequest));
              });
    }

    private void logRequest(ClientRequest request) {
      LogFormatUtils.traceDebug(log, traceOn ->
              request.logPrefix() + "HTTP " + request.method() + " " + request.uri() +
                      (traceOn ? ", headers=" + formatHeaders(request.headers()) : "")
      );
    }

    private String getLogPrefix(ClientRequest request, ClientHttpResponse response) {
      return request.logPrefix() + "[" + response.getId() + "] ";
    }

    private void logResponse(ClientHttpResponse response, String logPrefix) {
      LogFormatUtils.traceDebug(log, traceOn -> {
        HttpStatusCode code = response.getStatusCode();
        return logPrefix + "Response " + code +
                (traceOn ? ", headers=" + formatHeaders(response.getHeaders()) : "");
      });
    }

    private String formatHeaders(HttpHeaders headers) {
      return this.enableLoggingRequestDetails ? headers.toString() : headers.isEmpty() ? "{}" : "{masked}";
    }

    private <T> Mono<T> wrapException(Throwable t, ClientRequest r) {
      return Mono.error(() -> new WebClientRequestException(t, r.method(), r.uri(), r.headers()));
    }

    private HttpRequest createRequest(ClientRequest request) {
      return new AbstractHttpRequest() {

        @Override
        public HttpMethod getMethod() {
          return request.method();
        }

        @Override
        public URI getURI() {
          return request.uri();
        }

        @Override
        public HttpHeaders getHeaders() {
          return request.headers();
        }
      };
    }
  }

}
