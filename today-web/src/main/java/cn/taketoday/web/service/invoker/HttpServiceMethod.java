/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.service.invoker;

import org.reactivestreams.Publisher;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterizedTypeReference;
import cn.taketoday.core.ReactiveAdapter;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.annotation.SynthesizingMethodParameter;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.MediaType;
import cn.taketoday.http.ResponseEntity;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.service.annotation.HttpExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

  private static final boolean REACTOR_PRESENT =
          ClassUtils.isPresent("reactor.core.publisher.Mono", HttpServiceMethod.class.getClassLoader());

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

    boolean isReactorAdapter = REACTOR_PRESENT && adapter instanceof ReactorHttpExchangeAdapter;

    this.requestValuesInitializer =
            HttpRequestValuesInitializer.create(
                    method, containingClass, embeddedValueResolver,
                    (isReactorAdapter ? ReactiveHttpRequestValues::builder : HttpRequestValues::builder));

    this.responseFunction =
            isReactorAdapter
            ? ReactorExchangeResponseFunction.create((ReactorHttpExchangeAdapter) adapter, method)
            : ExchangeResponseFunction.create(adapter, method);
  }

  private static MethodParameter[] initMethodParameters(Method method) {
    int count = method.getParameterCount();
    if (count == 0) {
      return new MethodParameter[0];
    }

    DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();
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
    HttpRequestValues.Builder requestValues = this.requestValuesInitializer.initializeRequestValuesBuilder();
    applyArguments(requestValues, arguments);
    return this.responseFunction.execute(requestValues.build());
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
        throw new IllegalStateException("Could not resolve parameter [" + parameters[i].getParameterIndex() + "] in " +
                parameters[i].getExecutable().toGenericString() +
                (StringUtils.hasText("No suitable resolver") ? ": " + "No suitable resolver" : ""));
      }

    }
  }

  /**
   * Factory for {@link HttpRequestValues} with values extracted from the type
   * and method-level {@link HttpExchange @HttpRequest} annotations.
   */
  private record HttpRequestValuesInitializer(
          @Nullable HttpMethod httpMethod, @Nullable String url,
          @Nullable MediaType contentType, @Nullable List<MediaType> acceptMediaTypes,
          Supplier<HttpRequestValues.Builder> requestValuesSupplier) {

    public HttpRequestValues.Builder initializeRequestValuesBuilder() {
      HttpRequestValues.Builder requestValues = this.requestValuesSupplier.get();
      if (this.httpMethod != null) {
        requestValues.setHttpMethod(this.httpMethod);
      }
      if (this.url != null) {
        requestValues.setUriTemplate(this.url);
      }
      if (this.contentType != null) {
        requestValues.setContentType(this.contentType);
      }
      if (this.acceptMediaTypes != null) {
        requestValues.setAccept(this.acceptMediaTypes);
      }
      return requestValues;
    }

    /**
     * Introspect the method and create the request factory for it.
     */
    public static HttpRequestValuesInitializer create(Method method, Class<?> containingClass,
            @Nullable StringValueResolver embeddedValueResolver,
            Supplier<HttpRequestValues.Builder> requestValuesSupplier) {

      HttpExchange annot1 = AnnotatedElementUtils.findMergedAnnotation(containingClass, HttpExchange.class);
      HttpExchange annot2 = AnnotatedElementUtils.findMergedAnnotation(method, HttpExchange.class);

      Assert.notNull(annot2, "Expected HttpRequest annotation");

      HttpMethod httpMethod = initHttpMethod(annot1, annot2);
      String url = initUrl(annot1, annot2, embeddedValueResolver);
      MediaType contentType = initContentType(annot1, annot2);
      List<MediaType> acceptableMediaTypes = initAccept(annot1, annot2);

      return new HttpRequestValuesInitializer(
              httpMethod, url, contentType, acceptableMediaTypes, requestValuesSupplier);
    }

    @Nullable
    private static HttpMethod initHttpMethod(@Nullable HttpExchange typeAnnot, HttpExchange annot) {

      String value1 = (typeAnnot != null ? typeAnnot.method() : null);
      String value2 = annot.method();

      if (StringUtils.hasText(value2)) {
        return HttpMethod.valueOf(value2);
      }

      if (StringUtils.hasText(value1)) {
        return HttpMethod.valueOf(value1);
      }

      return null;
    }

    @Nullable
    private static String initUrl(@Nullable HttpExchange typeAnnot,
            HttpExchange annot, @Nullable StringValueResolver embeddedValueResolver) {

      String url1 = (typeAnnot != null ? typeAnnot.url() : null);
      String url2 = annot.url();

      if (embeddedValueResolver != null) {
        url1 = (url1 != null ? embeddedValueResolver.resolveStringValue(url1) : null);
        url2 = embeddedValueResolver.resolveStringValue(url2);
      }

      boolean hasUrl1 = StringUtils.hasText(url1);
      boolean hasUrl2 = StringUtils.hasText(url2);

      if (hasUrl1 && hasUrl2) {
        return (url1 + (!url1.endsWith("/") && !url2.startsWith("/") ? "/" : "") + url2);
      }

      if (!hasUrl1 && !hasUrl2) {
        return null;
      }

      return (hasUrl2 ? url2 : url1);
    }

    @Nullable
    private static MediaType initContentType(@Nullable HttpExchange typeAnnot, HttpExchange annot) {

      String value1 = (typeAnnot != null ? typeAnnot.contentType() : null);
      String value2 = annot.contentType();

      if (StringUtils.hasText(value2)) {
        return MediaType.parseMediaType(value2);
      }

      if (StringUtils.hasText(value1)) {
        return MediaType.parseMediaType(value1);
      }

      return null;
    }

    @Nullable
    private static List<MediaType> initAccept(@Nullable HttpExchange typeAnnot, HttpExchange annot) {

      String[] value1 = (typeAnnot != null ? typeAnnot.accept() : null);
      String[] value2 = annot.accept();

      if (ObjectUtils.isNotEmpty(value2)) {
        return MediaType.parseMediaTypes(Arrays.asList(value2));
      }

      if (ObjectUtils.isNotEmpty(value1)) {
        return MediaType.parseMediaTypes(Arrays.asList(value1));
      }

      return null;
    }

  }

  /**
   * Execute a request, obtain a response, and adapt to the expected return type.
   */
  private interface ResponseFunction {

    @Nullable
    Object execute(HttpRequestValues requestValues);

  }

  private record ExchangeResponseFunction(
          Function<HttpRequestValues, Object> responseFunction) implements ResponseFunction {

    @Override
    public Object execute(HttpRequestValues requestValues) {
      return this.responseFunction.apply(requestValues);
    }

    /**
     * Create the {@code ResponseFunction} that matches the method return type.
     */
    public static ResponseFunction create(HttpExchangeAdapter client, Method method) {
      MethodParameter param = new MethodParameter(method, -1).nestedIfOptional();
      Class<?> paramType = param.getNestedParameterType();

      Function<HttpRequestValues, Object> responseFunction;
      if (paramType.equals(void.class) || paramType.equals(Void.class)) {
        responseFunction = requestValues -> {
          client.exchange(requestValues);
          return null;
        };
      }
      else if (paramType.equals(HttpHeaders.class)) {
        responseFunction = request -> asOptionalIfNecessary(client.exchangeForHeaders(request), param);
      }
      else if (paramType.equals(ResponseEntity.class)) {
        MethodParameter bodyParam = param.nested();
        if (bodyParam.getNestedParameterType().equals(Void.class)) {
          responseFunction = request ->
                  asOptionalIfNecessary(client.exchangeForBodilessEntity(request), param);
        }
        else {
          ParameterizedTypeReference<?> bodyTypeRef =
                  ParameterizedTypeReference.forType(bodyParam.getNestedGenericParameterType());
          responseFunction = request ->
                  asOptionalIfNecessary(client.exchangeForEntity(request, bodyTypeRef), param);
        }
      }
      else {
        ParameterizedTypeReference<?> bodyTypeRef =
                ParameterizedTypeReference.forType(param.getNestedGenericParameterType());
        responseFunction = request ->
                asOptionalIfNecessary(client.exchangeForBody(request, bodyTypeRef), param);
      }

      return new ExchangeResponseFunction(responseFunction);
    }

    private static @Nullable Object asOptionalIfNecessary(@Nullable Object response, MethodParameter param) {
      return param.getParameterType().equals(Optional.class) ? Optional.ofNullable(response) : response;
    }
  }

  /**
   * {@link ResponseFunction} for {@link ReactorHttpExchangeAdapter}.
   */
  private record ReactorExchangeResponseFunction(
          Function<HttpRequestValues, Publisher<?>> responseFunction,
          @Nullable ReactiveAdapter returnTypeAdapter,
          boolean blockForOptional, @Nullable Duration blockTimeout) implements ResponseFunction {

    @Nullable
    public Object execute(HttpRequestValues requestValues) {

      Publisher<?> responsePublisher = this.responseFunction.apply(requestValues);

      if (this.returnTypeAdapter != null) {
        return this.returnTypeAdapter.fromPublisher(responsePublisher);
      }

      if (this.blockForOptional) {
        return (this.blockTimeout != null ?
                ((Mono<?>) responsePublisher).blockOptional(this.blockTimeout) :
                ((Mono<?>) responsePublisher).blockOptional());
      }
      else {
        return (this.blockTimeout != null ?
                ((Mono<?>) responsePublisher).block(this.blockTimeout) :
                ((Mono<?>) responsePublisher).block());
      }
    }

    /**
     * Create the {@code ResponseFunction} that matches the method return type.
     */
    public static ResponseFunction create(ReactorHttpExchangeAdapter client, Method method) {
      MethodParameter returnParam = new MethodParameter(method, -1);
      Class<?> returnType = returnParam.getParameterType();

      ReactiveAdapter reactiveAdapter = client.getReactiveAdapterRegistry().getAdapter(returnType);

      MethodParameter actualParam = (reactiveAdapter != null ? returnParam.nested() : returnParam.nestedIfOptional());
      Class<?> actualType = actualParam.getNestedParameterType();

      Function<HttpRequestValues, Publisher<?>> responseFunction;
      if (actualType.equals(void.class) || actualType.equals(Void.class)) {
        responseFunction = client::exchangeForMono;
      }
      else if (reactiveAdapter != null && reactiveAdapter.isNoValue()) {
        responseFunction = client::exchangeForMono;
      }
      else if (actualType.equals(HttpHeaders.class)) {
        responseFunction = client::exchangeForHeadersMono;
      }
      else if (actualType.equals(ResponseEntity.class)) {
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
      else {
        responseFunction = initBodyFunction(client, actualParam, reactiveAdapter);
      }

      return new ReactorExchangeResponseFunction(
              responseFunction, reactiveAdapter, returnType.equals(Optional.class), client.getBlockTimeout());
    }

    @SuppressWarnings("ConstantConditions")
    private static Function<HttpRequestValues, Publisher<?>> initResponseEntityFunction(
            ReactorHttpExchangeAdapter client, MethodParameter methodParam,
            @Nullable ReactiveAdapter reactiveAdapter) {

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
            ReactorHttpExchangeAdapter client, MethodParameter methodParam,
            @Nullable ReactiveAdapter reactiveAdapter) {

      ParameterizedTypeReference<?> bodyType =
              ParameterizedTypeReference.forType(methodParam.getNestedGenericParameterType());

      return reactiveAdapter != null && reactiveAdapter.isMultiValue()
             ? request -> client.exchangeForBodyFlux(request, bodyType)
             : request -> client.exchangeForBodyMono(request, bodyType);
    }
  }

}
