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

package infra.web.service.support;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.http.HttpCookie;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.HttpMethod;
import infra.http.ResponseEntity;
import infra.http.StreamingHttpOutputMessage;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;
import infra.web.client.RestClient;
import infra.web.service.invoker.HttpExchangeAdapter;
import infra.web.service.invoker.HttpRequestValues;
import infra.web.service.invoker.HttpServiceProxyFactory;
import infra.web.service.invoker.RequestExecution;
import infra.web.service.invoker.RequestExecutionFactory;
import infra.web.service.invoker.WrapOptionalExecutionDecorator;
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
public final class RestClientAdapter implements HttpExchangeAdapter, RequestExecutionFactory<HttpRequestValues> {

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

  @Override
  public HttpRequestValues.Builder createBuilder() {
    return HttpRequestValues.builder();
  }

  /**
   * Create the {@link RequestExecution} that matches the method return type.
   */
  @Override
  public RequestExecution<HttpRequestValues> createRequestExecution(Method method) {
    MethodParameter param = returnType(method);
    return param.getParameterType() == Optional.class
            ? new WrapOptionalExecutionDecorator<>(createRequestExecution(param))
            : createRequestExecution(param);
  }

  private RequestExecution<HttpRequestValues> createRequestExecution(MethodParameter param) {
    Class<?> returnType = param.getParameterType();
    if (isAsync(returnType)) {
      RequestExecution<HttpRequestValues> execution = createResponseFunctionAsync(param);
      if (CompletionStage.class.isAssignableFrom(returnType)) {
        return request -> {
          Future<?> result = (Future<?>) execution.execute(request);
          return result.completable();  // result non-null
        };
      }
      return execution;
    }

    Class<?> paramType = param.getNestedParameterType();
    if (ClassUtils.isVoidType(paramType)) {
      return request -> {
        exchange(request).close();
        return null;
      };
    }
    else if (paramType == HttpInputMessage.class
            || paramType == ClientHttpResponse.class
            || paramType == ClientResponse.class) {
      return this::exchange;
    }
    else if (paramType == HttpHeaders.class) {
      return request -> {
        try (var response = exchange(request)) {
          return response.getHeaders();
        }
      };
    }
    else if (paramType == ResponseEntity.class) {
      MethodParameter bodyParam = param.nested();
      if (bodyParam.getNestedParameterType().equals(Void.class)) {
        return this::exchangeForBodilessEntity;
      }
      else {
        var bodyTypeRef = ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
        return request -> exchangeForEntity(request, bodyTypeRef);
      }
    }

    var sharedRegistry = ReactiveAdapterRegistry.getSharedInstance();
    ReactiveAdapter returnAdapter = sharedRegistry.getAdapter(returnType);
    if (returnAdapter != null) {
      ReactiveAdapter reactiveAdapter = sharedRegistry.getAdapter(Future.class);
      // Future reactive adapter
      if (reactiveAdapter == null) {
        throw new IllegalStateException("Return type: '%s' reactive adapter not found".formatted(Future.class.getName()));
      }
      RequestExecution<HttpRequestValues> execution = createResponseFunctionAsync(param.nested());
      return request -> returnAdapter.fromPublisher(reactiveAdapter.toPublisher(execution.execute(request)));
    }

    var bodyTypeRef = ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
    return request -> exchangeForBody(request, bodyTypeRef);
  }

  private static boolean isAsync(Class<?> parameterType) {
    return java.util.concurrent.Future.class == parameterType || Future.class == parameterType
            || CompletionStage.class == parameterType || CompletableFuture.class == parameterType;
  }

  // @since 5.0
  private RequestExecution<HttpRequestValues> createResponseFunctionAsync(MethodParameter param) {
    Class<?> paramType = param.getNestedParameterType();

    if (ClassUtils.isVoidType(paramType)) {
      // Future<Void> auto close response
      return this::exchangeAsyncVoid;
    }
    if (paramType == ClientHttpResponse.class
            || paramType == ClientResponse.class) {
      // Future<ClientHttpResponse/ConvertibleClientHttpResponse> close by user
      return this::exchangeAsync;
    }
    else if (paramType == HttpHeaders.class) {
      // Future<HttpHeaders>
      return request -> this.exchangeAsync(request)
              .onSuccess(ClientHttpResponse::close)
              .map(ClientHttpResponse::getHeaders);
    }
    else if (paramType == ResponseEntity.class) {
      MethodParameter bodyParam = param.nested();
      if (bodyParam.getNestedParameterType().equals(Void.class)) {
        // Future<ResponseEntity<Void>>
        return this::exchangeForBodilessEntityAsync;
      }
      else {
        // Future<ResponseEntity<T>>
        var bodyTypeRef = ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
        return request -> exchangeForEntityAsync(request, bodyTypeRef);
      }
    }
    else {
      // Future<T>, Future<List<T>>
      var bodyTypeRef = ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
      return request -> exchangeAsyncBody(request, bodyTypeRef);
    }
  }

  private static MethodParameter returnType(Method method) {
    MethodParameter param = new MethodParameter(method, -1).nestedIfOptional();
    if (isAsync(param.getParameterType())) {
      param = param.nested();
    }
    return param;
  }
}
