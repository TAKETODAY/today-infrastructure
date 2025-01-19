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

package infra.web.service.invoker;

import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import infra.core.MethodParameter;
import infra.core.ParameterNameDiscoverer;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ReactiveStreams;
import infra.core.StringValueResolver;
import infra.core.annotation.SynthesizingMethodParameter;
import infra.http.HttpHeaders;
import infra.http.ResponseEntity;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;
import infra.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;

/**
 * Implements the invocation of an {@link HttpExchange @HttpExchange}-annotated,
 * {@link HttpServiceProxyFactory#createClient(Class) HTTP service proxy} method
 * by delegating to an {@link HttpExchangeAdapter} to perform actual requests.
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @author Olga Maciaszek-Sharma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class HttpServiceMethod {

  private final Method method;

  private final MethodParameter[] parameters;

  private final List<HttpServiceArgumentResolver> argumentResolvers;

  private final HttpRequestValuesInitializer requestValuesInitializer;

  private final ResponseFunction responseFunction;

  HttpServiceMethod(Method method, Class<?> containingClass, List<HttpServiceArgumentResolver> argumentResolvers,
          HttpExchangeAdapter adapter, @Nullable StringValueResolver embeddedValueResolver) {

    this.method = method;
    this.parameters = initMethodParameters(method);
    this.argumentResolvers = argumentResolvers;

    boolean isReactorAdapter = ReactiveStreams.reactorPresent && adapter instanceof ReactorHttpExchangeAdapter;

    this.requestValuesInitializer = HttpRequestValuesInitializer.create(method, containingClass, embeddedValueResolver,
            isReactorAdapter ? ReactiveHttpRequestValues::builder : HttpRequestValues::builder);

    ResponseFunction responseFunction = null;
    if (isReactorAdapter) {
      responseFunction = ReactorExchangeResponseFunction.create((ReactorHttpExchangeAdapter) adapter, method);
    }

    if (responseFunction == null) {
      responseFunction = createResponseFunction(adapter, method);
    }
    this.responseFunction = responseFunction;
  }

  private static MethodParameter[] initMethodParameters(Method method) {
    int count = method.getParameterCount();
    if (count == 0) {
      return new MethodParameter[0];
    }

    ParameterNameDiscoverer nameDiscoverer = ParameterNameDiscoverer.getSharedInstance();
    MethodParameter[] parameters = new MethodParameter[count];
    for (int i = 0; i < count; i++) {
      parameters[i] = new SynthesizingMethodParameter(method, i);
      parameters[i].initParameterNameDiscovery(nameDiscoverer);
    }
    return parameters;
  }

  public Method getMethod() {
    return this.method;
  }

  @Nullable
  public Object invoke(Object[] arguments) {
    var requestValues = requestValuesInitializer.initializeRequestValuesBuilder();
    applyArguments(requestValues, arguments);
    return responseFunction.execute(requestValues.build());
  }

  private void applyArguments(HttpRequestValues.Builder requestValues, Object[] arguments) {
    MethodParameter[] parameters = this.parameters;
    Assert.isTrue(arguments.length == parameters.length, "Method argument mismatch");
    for (int i = 0; i < arguments.length; i++) {
      Object value = arguments[i];
      boolean resolved = false;
      for (HttpServiceArgumentResolver resolver : this.argumentResolvers) {
        if (resolver.resolve(value, parameters[i], requestValues)) {
          resolved = true;
          break;
        }
      }
      if (!resolved) {
        throw new IllegalStateException("Could not resolve parameter [%d] in %s: No suitable resolver"
                .formatted(this.parameters[i].getParameterIndex(), this.parameters[i].getExecutable().toGenericString()));
      }
    }
  }

  /**
   * Execute a request, obtain a response, and adapt to the expected return type.
   */
  private interface ResponseFunction {

    @Nullable
    Object execute(HttpRequestValues requestValues);

  }

  /**
   * Create the {@code ResponseFunction} that matches the method return type.
   */
  private static ResponseFunction createResponseFunction(HttpExchangeAdapter client, Method method) {
    MethodParameter param = returnType(method);

    Class<?> returnType = param.getParameterType();
    if (isAsync(returnType)) {
      ResponseFunction function = createResponseFunctionAsync(client, param);
      if (CompletionStage.class.isAssignableFrom(returnType)) {
        return request -> {
          Future<?> result = (Future<?>) function.execute(request);
          return result.completable();  // result non-null
        };
      }
      return function;
    }

    Class<?> paramType = param.getNestedParameterType();
    if (ClassUtils.isVoidType(paramType)) {
      return request -> {
        client.exchange(request).close();
        return null;
      };
    }
    else if (paramType == ClientHttpResponse.class || paramType == ClientResponse.class) {
      return client::exchange;
    }
    else if (paramType == HttpHeaders.class) {
      return request -> {
        try (var response = client.exchange(request)) {
          return asOptionalIfNecessary(response.getHeaders(), param);
        }
      };
    }
    else if (paramType == ResponseEntity.class) {
      MethodParameter bodyParam = param.nested();
      if (bodyParam.getNestedParameterType().equals(Void.class)) {
        return request ->
                asOptionalIfNecessary(client.exchangeForBodilessEntity(request), param);
      }
      else {
        var bodyTypeRef = ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
        return request ->
                asOptionalIfNecessary(client.exchangeForEntity(request, bodyTypeRef), param);
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
      ResponseFunction responseFunction = createResponseFunctionAsync(client, param.nested());
      return request -> returnAdapter.fromPublisher(reactiveAdapter.toPublisher(responseFunction.execute(request)));
    }

    var bodyTypeRef = ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
    return request ->
            asOptionalIfNecessary(client.exchangeForBody(request, bodyTypeRef), param);
  }

  private static boolean isAsync(Class<?> parameterType) {
    return java.util.concurrent.Future.class == parameterType || Future.class == parameterType
            || CompletionStage.class == parameterType || CompletableFuture.class == parameterType;
  }

  // @since 5.0
  private static ResponseFunction createResponseFunctionAsync(HttpExchangeAdapter client, MethodParameter param) {
    Class<?> paramType = param.getNestedParameterType();

    if (ClassUtils.isVoidType(paramType)) {
      // Future<Void> auto close response
      return request -> client.exchangeAsync(request)
              .onSuccess(ClientHttpResponse::close);
    }
    if (paramType == ClientHttpResponse.class
            || paramType == ClientResponse.class) {
      // Future<ClientHttpResponse/ConvertibleClientHttpResponse> close by user
      return client::exchangeAsync;
    }
    else if (paramType == HttpHeaders.class) {
      // Future<HttpHeaders>
      return request -> client.exchangeAsync(request)
              .onSuccess(ClientHttpResponse::close)
              .map(ClientHttpResponse::getHeaders);
    }
    else if (paramType == ResponseEntity.class) {
      MethodParameter bodyParam = param.nested();
      if (bodyParam.getNestedParameterType().equals(Void.class)) {
        // Future<ResponseEntity<Void>>
        return client::exchangeForBodilessEntityAsync;
      }
      else {
        // Future<ResponseEntity<T>>
        var bodyTypeRef = ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
        return request -> client.exchangeForEntityAsync(request, bodyTypeRef);
      }
    }
    else {
      // Future<T>, Future<List<T>>
      var bodyTypeRef = ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
      return request -> client.exchangeAsync(request)
              .map(response -> response.bodyTo(bodyTypeRef));
    }
  }

  private static MethodParameter returnType(Method method) {
    MethodParameter param = new MethodParameter(method, -1).nestedIfOptional();
    if (isAsync(param.getParameterType())) {
      param = param.nested();
    }
    return param;
  }

  @Nullable
  private static Object asOptionalIfNecessary(@Nullable Object response, MethodParameter param) {
    return param.getParameterType() == Optional.class ? Optional.ofNullable(response) : response;
  }

  /**
   * {@link ResponseFunction} for {@link ReactorHttpExchangeAdapter}.
   */
  private record ReactorExchangeResponseFunction(Function<HttpRequestValues, Publisher<?>> responseFunction,
          ReactiveAdapter returnTypeAdapter) implements ResponseFunction {

    @Override
    public Object execute(HttpRequestValues requestValues) {
      Publisher<?> responsePublisher = responseFunction.apply(requestValues);
      return returnTypeAdapter.fromPublisher(responsePublisher);
    }

    /**
     * Create the {@code ResponseFunction} that matches the method return type.
     */
    @Nullable
    public static ResponseFunction create(ReactorHttpExchangeAdapter client, Method method) {
      MethodParameter returnParam = new MethodParameter(method, -1);
      Class<?> returnType = returnParam.getParameterType();

      ReactiveAdapter reactiveAdapter = client.getReactiveAdapterRegistry().getAdapter(returnType);
      if (reactiveAdapter == null) {
        return null;
      }
      MethodParameter actualParam = returnParam.nested();
      Class<?> actualType = actualParam.getNestedParameterType();

      Function<HttpRequestValues, Publisher<?>> responseFunction;
      if (ClassUtils.isVoidType(actualType)) {
        responseFunction = client::exchangeForMono;
      }
      else if (reactiveAdapter.isNoValue()) {
        responseFunction = client::exchangeForMono;
      }
      else if (actualType == HttpHeaders.class) {
        responseFunction = client::exchangeForHeadersMono;
      }
      else if (actualType == ResponseEntity.class) {
        MethodParameter bodyParam = actualParam.nested();
        Class<?> bodyType = bodyParam.getNestedParameterType();
        if (bodyType.equals(Void.class)) {
          responseFunction = client::exchangeForBodilessEntityMono;
        }
        else {
          ReactiveAdapter bodyAdapter = client.getReactiveAdapterRegistry().getAdapter(bodyType);
          responseFunction = initResponseEntityFunction(client, bodyParam, bodyAdapter);
        }
      }
      else if (actualType == infra.web.client.reactive.ClientResponse.class) {
        responseFunction = client::exchangeMono;
      }
      else if (actualType == ClientResponse.class) {
        ReactiveAdapter futureAdapter = client.getReactiveAdapterRegistry().getAdapter(Future.class);
        Assert.state(futureAdapter != null, "Future reactive adapter not found");
        responseFunction = request -> futureAdapter.toPublisher(client.exchangeAsync(request));
      }
      else {
        responseFunction = initBodyFunction(client, actualParam, reactiveAdapter);
      }

      return new ReactorExchangeResponseFunction(responseFunction, reactiveAdapter);
    }

    @SuppressWarnings("ConstantConditions")
    private static Function<HttpRequestValues, Publisher<?>> initResponseEntityFunction(
            ReactorHttpExchangeAdapter client, MethodParameter methodParam, @Nullable ReactiveAdapter reactiveAdapter) {

      if (reactiveAdapter == null) {
        return request -> client.exchangeForEntityMono(
                request, ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType()));
      }

      Assert.isTrue(reactiveAdapter.isMultiValue(),
              "ResponseEntity body must be a concrete value or a multi-value Publisher");

      ParameterizedTypeReference<?> bodyType =
              ParameterizedTypeReference.forType(methodParam.nested().getNestedGenericParameterType());

      // Shortcut for Flux
      if (reactiveAdapter.getReactiveType().equals(Flux.class)) {
        return request -> client.exchangeForEntityFlux(request, bodyType);
      }

      return request -> client.exchangeForEntityFlux(request, bodyType)
              .map(entity -> {
                Object body = reactiveAdapter.fromPublisher(entity.getBody());
                return new ResponseEntity<>(body, entity.headers(), entity.getStatusCode());
              });
    }

    private static Function<HttpRequestValues, Publisher<?>> initBodyFunction(
            ReactorHttpExchangeAdapter client, MethodParameter methodParam, ReactiveAdapter reactiveAdapter) {

      ParameterizedTypeReference<?> bodyType =
              ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType());

      return reactiveAdapter.isMultiValue()
              ? request -> client.exchangeForBodyFlux(request, bodyType)
              : request -> client.exchangeForBodyMono(request, bodyType);
    }
  }

}
