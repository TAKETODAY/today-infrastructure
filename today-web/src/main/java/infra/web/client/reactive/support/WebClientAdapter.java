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

package infra.web.client.reactive.support;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.io.InputStream;

import infra.core.ParameterizedTypeReference;
import infra.core.io.buffer.DataBuffer;
import infra.core.io.buffer.DataBufferUtils;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.http.ResponseEntity;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StreamUtils;
import infra.util.concurrent.Future;
import infra.web.client.reactive.ClientResponse;
import infra.web.client.reactive.WebClient;
import infra.web.service.invoker.AbstractReactorHttpExchangeAdapter;
import infra.web.service.invoker.HttpRequestValues;
import infra.web.service.invoker.HttpServiceProxyFactory;
import infra.web.service.invoker.ReactiveHttpRequestValues;
import infra.web.service.invoker.ReactorHttpExchangeAdapter;
import infra.web.util.UriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * {@link ReactorHttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link WebClient} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@code HttpServiceProxyFactory} configured with a given {@code WebClient}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class WebClientAdapter extends AbstractReactorHttpExchangeAdapter {

  private final WebClient webClient;

  /**
   * Package private constructor. See static factory methods.
   */
  private WebClientAdapter(WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  public boolean supportsRequestAttributes() {
    return true;
  }

  @Override
  public Mono<Void> exchangeForMono(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity().then();
  }

  @Override
  public Mono<HttpHeaders> exchangeForHeadersMono(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity().map(ResponseEntity::getHeaders);
  }

  @Override
  public <T> Mono<T> exchangeForBodyMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().bodyToMono(bodyType);
  }

  @Override
  public <T> Flux<T> exchangeForBodyFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().bodyToFlux(bodyType);
  }

  @Override
  public Mono<ResponseEntity<Void>> exchangeForBodilessEntityMono(HttpRequestValues requestValues) {
    return newRequest(requestValues).retrieve().toBodilessEntity();
  }

  @Override
  public <T> Mono<ResponseEntity<T>> exchangeForEntityMono(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().toEntity(bodyType);
  }

  @Override
  public <T> Mono<ResponseEntity<Flux<T>>> exchangeForEntityFlux(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyType) {
    return newRequest(requestValues).retrieve().toEntityFlux(bodyType);
  }

  @Override
  public infra.web.client.ClientResponse exchange(HttpRequestValues requestValues) {
    return blockingGet(newRequest(requestValues).exchange().map(ReactorClientResponse::new));
  }

  @Override
  public Future<infra.web.client.ClientResponse> exchangeAsync(HttpRequestValues requestValues) {
    return Future.forAdaption(newRequest(requestValues).exchange().toFuture())
            .map(ReactorClientResponse::new);
  }

  @Override
  public <T> Future<T> exchangeAsyncBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyTypeRef) {
    return Future.forAdaption(newRequest(requestValues).retrieve().bodyToMono(bodyTypeRef).toFuture());
  }

  @Override
  public Future<Void> exchangeAsyncVoid(HttpRequestValues requestValues) {
    return Future.forAdaption(newRequest(requestValues).retrieve().toBodiless().toFuture());
  }

  @Override
  public Mono<ClientResponse> exchangeMono(HttpRequestValues requestValues) {
    return newRequest(requestValues).exchange();
  }

  @SuppressWarnings("ReactiveStreamsUnusedPublisher")
  private WebClient.RequestBodySpec newRequest(HttpRequestValues values) {
    HttpMethod httpMethod = values.getHttpMethod();
    Assert.notNull(httpMethod, "HttpMethod is required");

    WebClient.RequestBodyUriSpec uriSpec = this.webClient.method(httpMethod);

    WebClient.RequestBodySpec bodySpec;
    if (values.getUri() != null) {
      bodySpec = uriSpec.uri(values.getUri());
    }
    else if (values.getUriTemplate() != null) {
      UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
      if (uriBuilderFactory != null) {
        bodySpec = uriSpec.uri(uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables()));
      }
      else {
        bodySpec = uriSpec.uri(values.getUriTemplate(), values.getUriVariables());
      }
    }
    else {
      throw new IllegalStateException("Neither full URL nor URI template");
    }

    bodySpec.headers(values.getHeaders())
            .cookies(values.getCookies())
            .attributes(values.getAttributes());

    if (values.getBodyValue() != null) {
      bodySpec.bodyValue(values.getBodyValue());
    }
    else if (values instanceof ReactiveHttpRequestValues rrv) {
      Publisher<?> body = rrv.getBodyPublisher();
      if (body != null) {
        ParameterizedTypeReference<?> elementType = rrv.getBodyPublisherElementType();
        Assert.notNull(elementType, "Publisher body element type is required");
        bodySpec.body(body, elementType);
      }
    }

    return bodySpec;
  }

  /**
   * Create a {@link WebClientAdapter} for the given {@code WebClient} instance.
   *
   * @param webClient the client to use
   * @return the created adapter instance
   */
  public static WebClientAdapter forClient(WebClient webClient) {
    return new WebClientAdapter(webClient);
  }

  /**
   * @since 5.0
   */
  final class ReactorClientResponse implements ClientHttpResponse, infra.web.client.ClientResponse {

    private final ClientResponse clientResponse;

    @Nullable
    private volatile InputStream body;

    private ReactorClientResponse(ClientResponse clientResponse) {
      this.clientResponse = clientResponse;
    }

    @Override
    public HttpStatusCode getStatusCode() {
      return clientResponse.statusCode();
    }

    @Override
    public String getStatusText() {
      if (getStatusCode() instanceof HttpStatus status) {
        return status.getReasonPhrase();
      }
      else {
        return "";
      }
    }

    @Override
    public void close() {
      InputStream body = this.body;
      if (body != null) {
        try (body) {
          StreamUtils.drain(this.body);
        }
        catch (IOException ignored) {

        }
      }
    }

    @Override
    public InputStream getBody() {
      InputStream body = this.body;
      if (body != null) {
        return body;
      }

      Mono<InputStream> inputStreamMono = clientResponse.body((response, context) ->
              DataBufferUtils.join(response.getBody())).map(DataBuffer::asInputStream);

      body = blockingGet(inputStreamMono);
      if (body == null) {
        body = InputStream.nullInputStream();
      }
      this.body = body;
      return body;
    }

    @Override
    public HttpHeaders getHeaders() {
      return clientResponse.headers().asHttpHeaders();
    }

    @Nullable
    @Override
    public <T> T bodyTo(Class<T> bodyType) {
      return blockingGet(clientResponse.bodyToMono(bodyType));
    }

    @Nullable
    @Override
    public <T> T bodyTo(ParameterizedTypeReference<T> bodyType) {
      return blockingGet(clientResponse.bodyToMono(bodyType));
    }

  }

}
