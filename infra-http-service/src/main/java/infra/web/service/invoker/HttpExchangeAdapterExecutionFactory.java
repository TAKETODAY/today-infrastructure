package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import infra.core.MethodParameter;
import infra.core.ParameterizedTypeReference;
import infra.core.ReactiveAdapter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.ReactiveStreams;
import infra.http.HttpHeaders;
import infra.http.HttpInputMessage;
import infra.http.ResponseEntity;
import infra.http.client.ClientHttpResponse;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.concurrent.Future;
import infra.web.client.ClientResponse;
import reactor.core.publisher.Flux;

/**
 * Factory for creating request executions with HTTP exchange adapters.
 *
 * @author <a href="https://github.com/TAKETODAY">TAKETODAY</a>
 * @since 5.0
 */
final class HttpExchangeAdapterExecutionFactory implements RequestExecutionFactory<HttpRequestValues> {

  private final HttpExchangeAdapter exchangeAdapter;

  private final boolean isReactorAdapter;

  public HttpExchangeAdapterExecutionFactory(HttpExchangeAdapter exchangeAdapter) {
    this.exchangeAdapter = exchangeAdapter;
    this.isReactorAdapter = ReactiveStreams.reactorPresent && exchangeAdapter instanceof ReactorHttpExchangeAdapter;
  }

  @Override
  public RequestExecution<HttpRequestValues> createRequestExecution(Method method) {
    RequestExecution<HttpRequestValues> responseFunction = null;
    if (isReactorAdapter) {
      responseFunction = ReactorExchangeResponseFunction.create((ReactorHttpExchangeAdapter) exchangeAdapter, method);
    }

    if (responseFunction == null) {
      responseFunction = createResponseFunction(exchangeAdapter, method);
    }

    return method.getReturnType() == Optional.class
            ? new WrapOptionalExecutionDecorator<>(responseFunction)
            : responseFunction;
  }

  @Override
  public HttpRequestValues.Builder createBuilder() {
    return isReactorAdapter ? ReactiveHttpRequestValues.builder() : HttpRequestValues.builder();
  }

  @Override
  public boolean supportsRequestAttributes() {
    return exchangeAdapter.supportsRequestAttributes();
  }

  /**
   * Create the {@code ResponseFunction} that matches the method return type.
   */
  private static RequestExecution<HttpRequestValues> createResponseFunction(HttpExchangeAdapter client, Method method) {
    MethodParameter param = returnType(method);

    Class<?> returnType = param.getParameterType();
    if (isAsync(returnType)) {
      RequestExecution<HttpRequestValues> function = createResponseFunctionAsync(client, param);
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
    else if (paramType == HttpInputMessage.class
            || paramType == ClientHttpResponse.class
            || paramType == ClientResponse.class) {
      return client::exchange;
    }
    else if (paramType == HttpHeaders.class) {
      return request -> {
        try (var response = client.exchange(request)) {
          return response.getHeaders();
        }
      };
    }
    else if (paramType == ResponseEntity.class) {
      MethodParameter bodyParam = param.nested();
      if (bodyParam.getNestedParameterType().equals(Void.class)) {
        return client::exchangeForBodilessEntity;
      }
      else {
        var bodyTypeRef = ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
        return request -> client.exchangeForEntity(request, bodyTypeRef);
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
      RequestExecution<HttpRequestValues> responseFunction = createResponseFunctionAsync(client, param.nested());
      return request -> returnAdapter.fromPublisher(reactiveAdapter.toPublisher(responseFunction.execute(request)));
    }

    var bodyTypeRef = ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
    return request -> client.exchangeForBody(request, bodyTypeRef);
  }

  private static boolean isAsync(Class<?> parameterType) {
    return java.util.concurrent.Future.class == parameterType || Future.class == parameterType
            || CompletionStage.class == parameterType || CompletableFuture.class == parameterType;
  }

  // @since 5.0
  private static RequestExecution<HttpRequestValues> createResponseFunctionAsync(HttpExchangeAdapter client, MethodParameter param) {
    Class<?> paramType = param.getNestedParameterType();

    if (ClassUtils.isVoidType(paramType)) {
      // Future<Void> auto close response
      return client::exchangeAsyncVoid;
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
      return request -> client.exchangeAsyncBody(request, bodyTypeRef);
    }
  }

  private static MethodParameter returnType(Method method) {
    MethodParameter param = new MethodParameter(method, -1).nestedIfOptional();
    if (isAsync(param.getParameterType())) {
      param = param.nested();
    }
    return param;
  }

  /**
   * {@link RequestExecution} for {@link ReactorHttpExchangeAdapter}.
   */
  private record ReactorExchangeResponseFunction(Function<HttpRequestValues, Publisher<?>> responseFunction,
          ReactiveAdapter returnTypeAdapter) implements RequestExecution<HttpRequestValues> {

    @Override
    public Object execute(HttpRequestValues requestValues) {
      Publisher<?> responsePublisher = responseFunction.apply(requestValues);
      return returnTypeAdapter.fromPublisher(responsePublisher);
    }

    /**
     * Create the {@code RequestExecution} that matches the method return type.
     */
    @Nullable
    public static RequestExecution<HttpRequestValues> create(ReactorHttpExchangeAdapter client, Method method) {
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
      else if (actualType == infra.web.reactive.client.ClientResponse.class) {
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

    @SuppressWarnings("NullAway")
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
                return new ResponseEntity<>(body, entity.getHeaders(), entity.getStatusCode());
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
