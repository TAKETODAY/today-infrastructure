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

package infra.web.client.support;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.Executor;

import infra.core.ParameterizedTypeReference;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpMethod;
import infra.http.ResponseEntity;
import infra.http.StreamingHttpOutputMessage;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;
import infra.web.client.RestClient;
import infra.web.service.invoker.HttpExchangeAdapter;
import infra.web.service.invoker.HttpRequestValues;
import infra.web.service.invoker.HttpServiceProxyFactory;
import infra.web.util.UriBuilderFactory;

/**
 * {@link HttpExchangeAdapter} that enables an {@link HttpServiceProxyFactory}
 * to use {@link RestClient} for request execution.
 *
 * <p>Use static factory methods in this class to create an
 * {@link HttpServiceProxyFactory} configured with the given {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class RestClientAdapter implements HttpExchangeAdapter {

  private final RestClient restClient;

  @Nullable
  private final Executor asyncExecutor;

  private RestClientAdapter(RestClient restClient, @Nullable Executor asyncExecutor) {
    this.restClient = restClient;
    this.asyncExecutor = asyncExecutor;
  }

  @Override
  public boolean supportsRequestAttributes() {
    return true;
  }

  @Override
  public ClientResponse exchange(HttpRequestValues requestValues) {
    return newRequest(requestValues).execute(false);
  }

  @Override
  public Future<ClientResponse> exchangeAsync(HttpRequestValues requestValues) {
    return newRequest(requestValues).send(asyncExecutor);
  }

  @Override
  public <T> Future<T> exchangeAsyncBody(HttpRequestValues requestValues, ParameterizedTypeReference<T> bodyTypeRef) {
    return newRequest(requestValues).async(asyncExecutor).body(bodyTypeRef);
  }

  @Override
  public Future<Void> exchangeAsyncVoid(HttpRequestValues requestValues) {
    return newRequest(requestValues).async(asyncExecutor).toBodiless();
  }

  @Nullable
  @Override
  public <T> T exchangeForBody(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
    return newRequest(values).retrieve().body(bodyType);
  }

  @Override
  public ResponseEntity<Void> exchangeForBodilessEntity(HttpRequestValues values) {
    return newRequest(values).retrieve().toBodilessEntity();
  }

  @Override
  public Future<ResponseEntity<Void>> exchangeForBodilessEntityAsync(HttpRequestValues values) {
    return newRequest(values).async(asyncExecutor).toBodilessEntity();
  }

  @Override
  public <T> ResponseEntity<T> exchangeForEntity(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
    return newRequest(values).retrieve().toEntity(bodyType);
  }

  @Override
  public <T> Future<ResponseEntity<T>> exchangeForEntityAsync(HttpRequestValues values, ParameterizedTypeReference<T> bodyType) {
    return newRequest(values).async(asyncExecutor).toEntity(bodyType);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private RestClient.RequestBodySpec newRequest(HttpRequestValues values) {
    HttpMethod httpMethod = values.getHttpMethod();
    Assert.notNull(httpMethod, "HttpMethod is required");

    RestClient.RequestBodyUriSpec uriSpec = this.restClient.method(httpMethod);

    RestClient.RequestBodySpec bodySpec;
    if (values.getURI() != null) {
      bodySpec = uriSpec.uri(values.getURI());
    }
    else if (values.getUriTemplate() != null) {
      UriBuilderFactory uriBuilderFactory = values.getUriBuilderFactory();
      if (uriBuilderFactory != null) {
        URI uri = uriBuilderFactory.expand(values.getUriTemplate(), values.getUriVariables());
        bodySpec = uriSpec.uri(uri);
      }
      else {
        bodySpec = uriSpec.uri(values.getUriTemplate(), values.getUriVariables());
      }
    }
    else {
      throw new IllegalStateException("Neither full URL nor URI template");
    }

    bodySpec.headers(values.getHeaders())
            .attributes(values.getAttributes());

    if (!values.getCookies().isEmpty()) {
      ArrayList<String> cookies = new ArrayList<>();
      for (var entry : values.getCookies().entrySet()) {
        String name = entry.getKey();
        for (String value : entry.getValue()) {
          HttpCookie cookie = new HttpCookie(name, value);
          cookies.add(cookie.toString());
        }
      }
      bodySpec.header(HttpHeaders.COOKIE, String.join("; ", cookies));
    }

    var body = values.getBodyValue();
    if (body != null) {
      if (body instanceof StreamingHttpOutputMessage.Body sb) {
        bodySpec.body(sb);
      }
      else if (values.getBodyValueType() != null) {
        bodySpec.body(body, (ParameterizedTypeReference) values.getBodyValueType());
      }
      else {
        bodySpec.body(body);
      }
    }

    if (values.getApiVersion() != null) {
      bodySpec.apiVersion(values.getApiVersion());
    }

    return bodySpec;
  }

  /**
   * Create a {@link RestClientAdapter} for the given {@link RestClient}.
   */
  public static RestClientAdapter create(RestClient restClient) {
    return create(restClient, null);
  }

  /**
   * Create a {@link RestClientAdapter} for the given {@link RestClient}.
   *
   * @param asyncExecutor for async request
   * @see RestClient.RequestHeadersSpec#async(Executor)
   * @since 5.0
   */
  public static RestClientAdapter create(RestClient restClient, @Nullable Executor asyncExecutor) {
    return new RestClientAdapter(restClient, asyncExecutor);
  }

}
