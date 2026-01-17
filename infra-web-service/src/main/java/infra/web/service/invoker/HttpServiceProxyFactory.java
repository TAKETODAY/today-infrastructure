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

package infra.web.service.invoker;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import infra.aop.ProxyMethodInvocation;
import infra.aop.framework.ProxyFactory;
import infra.core.MethodIntrospector;
import infra.core.MethodParameter;
import infra.core.ReactiveAdapterRegistry;
import infra.core.StringValueResolver;
import infra.core.annotation.MergedAnnotations;
import infra.core.annotation.MergedAnnotations.SearchStrategy;
import infra.core.annotation.RepeatableContainers;
import infra.core.conversion.ConversionService;
import infra.format.support.ApplicationConversionService;
import infra.format.support.DefaultFormattingConversionService;
import infra.lang.Assert;
import infra.web.annotation.RequestMapping;
import infra.web.service.annotation.HttpExchange;

/**
 * Factory to create a client proxy from an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * <p>To create an instance, use static methods to obtain a
 * {@link Builder Builder}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class HttpServiceProxyFactory {

  private final HttpExchangeAdapter exchangeAdapter;

  private final List<HttpServiceArgumentResolver> argumentResolvers;

  @Nullable
  private final StringValueResolver embeddedValueResolver;

  private final HttpRequestValues.Processor requestValuesProcessor;

  private HttpServiceProxyFactory(HttpExchangeAdapter exchangeAdapter,
          List<HttpServiceArgumentResolver> argumentResolvers,
          @Nullable StringValueResolver embeddedValueResolver,
          ArrayList<HttpRequestValues.Processor> requestValuesProcessors) {

    this.exchangeAdapter = exchangeAdapter;
    this.argumentResolvers = argumentResolvers;
    this.embeddedValueResolver = embeddedValueResolver;
    this.requestValuesProcessor = new CompositeHttpRequestValuesProcessor(requestValuesProcessors);
  }

  /**
   * Return a proxy that implements the given HTTP service interface to perform
   * HTTP requests and retrieve responses through an HTTP client.
   *
   * @param serviceType the HTTP service to create a proxy for
   * @param <S> the HTTP service type
   * @return the created proxy
   */
  @SuppressWarnings("unchecked")
  public <S> S createClient(Class<S> serviceType) {
    List<HttpServiceMethod> httpServiceMethods =
            MethodIntrospector.filterMethods(serviceType, this::isExchangeMethod).stream()
                    .map(method -> createHttpServiceMethod(serviceType, method))
                    .toList();

    MethodInterceptor interceptor = new HttpServiceMethodInterceptor(httpServiceMethods);
    ProxyFactory factory = new ProxyFactory(serviceType, interceptor);
    return (S) factory.getProxy(serviceType.getClassLoader());
  }

  private boolean isExchangeMethod(Method method) {
    var annotations = MergedAnnotations.from(method, SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.NONE);
    return annotations.isPresent(HttpExchange.class)
            || annotations.isPresent(RequestMapping.class);
  }

  private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
    Assert.notNull(this.argumentResolvers,
            "No argument resolvers: afterPropertiesSet was not called");

    return new HttpServiceMethod(
            method, serviceType, this.argumentResolvers, this.exchangeAdapter, this.embeddedValueResolver, requestValuesProcessor);
  }

  /**
   * Return a builder that's initialized with the given client.
   */
  public static Builder forAdapter(HttpExchangeAdapter exchangeAdapter) {
    return new Builder().exchangeAdapter(exchangeAdapter);
  }

  /**
   * Return an empty builder, with the client to be provided to builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to create an {@link HttpServiceProxyFactory}.
   */
  public static final class Builder {

    private @Nullable HttpExchangeAdapter exchangeAdapter;

    private final List<HttpServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();

    private @Nullable ConversionService conversionService;

    private @Nullable StringValueResolver embeddedValueResolver;

    /**
     * @since 5.0
     */
    private boolean addFallbackArgumentResolver;

    /**
     * @since 5.0
     */
    private @Nullable HttpServiceArgumentResolver fallbackArgumentResolver;

    private final ArrayList<HttpRequestValues.Processor> requestValuesProcessors = new ArrayList<>();

    /**
     * Provide the HTTP client to perform requests through.
     *
     * @param adapter a client adapted to {@link HttpExchangeAdapter}
     * @return this same builder instance
     */
    public Builder exchangeAdapter(@Nullable HttpExchangeAdapter adapter) {
      this.exchangeAdapter = adapter;
      return this;
    }

    /**
     * Register a custom argument resolver, invoked ahead of default resolvers.
     *
     * @param resolver the resolver to add
     * @return this same builder instance
     */
    public Builder customArgumentResolver(HttpServiceArgumentResolver resolver) {
      this.customArgumentResolvers.add(resolver);
      return this;
    }

    /**
     * Register a default fallback argument resolver, invoked behind of default resolvers.
     *
     * @return this same builder instance
     * @since 5.0
     */
    public Builder useDefaultFallbackArgumentResolver() {
      this.addFallbackArgumentResolver = true;
      return this;
    }

    /**
     * Register a custom fallback argument resolver, invoked behind of default resolvers.
     *
     * @return this same builder instance
     * @since 5.0
     */
    public Builder fallbackArgumentResolver(@Nullable HttpServiceArgumentResolver fallbackArgumentResolver) {
      this.fallbackArgumentResolver = fallbackArgumentResolver;
      return this;
    }

    /**
     * Set the {@link ConversionService} to use where input values need to
     * be formatted as Strings.
     * <p>By default this is {@link DefaultFormattingConversionService}.
     *
     * @return this same builder instance
     */
    public Builder conversionService(@Nullable ConversionService conversionService) {
      this.conversionService = conversionService;
      return this;
    }

    /**
     * Register an {@link HttpRequestValues} processor that can further
     * customize request values based on the method and all arguments.
     *
     * @param processor the processor to add
     * @return this same builder instance
     * @since 5.0
     */
    public Builder httpRequestValuesProcessor(HttpRequestValues.Processor processor) {
      this.requestValuesProcessors.add(processor);
      return this;
    }

    /**
     * Set the {@link StringValueResolver} to use for resolving placeholders
     * and expressions embedded in {@link HttpExchange#url()}.
     *
     * @param embeddedValueResolver the resolver to use
     * @return this same builder instance
     */
    public Builder embeddedValueResolver(StringValueResolver embeddedValueResolver) {
      this.embeddedValueResolver = embeddedValueResolver;
      return this;
    }

    /**
     * Set the {@link ReactiveAdapterRegistry} to use to support different
     * asynchronous types for HTTP service method return values.
     * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
     *
     * @return this same builder instance
     */
    public Builder reactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
      if (this.exchangeAdapter instanceof AbstractReactorHttpExchangeAdapter settable) {
        settable.setReactiveAdapterRegistry(registry);
      }
      return this;
    }

    /**
     * Configure how long to block for the response of an HTTP service method
     * with a synchronous (blocking) method signature.
     * <p>By default this is not set, in which case the behavior depends on
     * connection and request timeout settings of the underlying HTTP client.
     * We recommend configuring timeout values directly on the underlying HTTP
     * client, which provides more control over such settings.
     *
     * @param blockTimeout the timeout value
     * @return this same builder instance
     */
    public Builder blockTimeout(@Nullable Duration blockTimeout) {
      if (this.exchangeAdapter instanceof AbstractReactorHttpExchangeAdapter settable) {
        settable.setBlockTimeout(blockTimeout);
      }
      return this;
    }

    /**
     * Build the {@link HttpServiceProxyFactory} instance.
     */
    public HttpServiceProxyFactory build() {
      Assert.notNull(exchangeAdapter, "HttpClientAdapter is required");

      return new HttpServiceProxyFactory(
              exchangeAdapter, initArgumentResolvers(), embeddedValueResolver, requestValuesProcessors);
    }

    /**
     * Return a proxy that implements the given HTTP service interface to perform
     * HTTP requests and retrieve responses through an HTTP client.
     *
     * @param serviceType the HTTP service to create a proxy for
     * @param <S> the HTTP service type
     * @return the created proxy
     */
    public <S> S createClient(Class<S> serviceType) {
      return build().createClient(serviceType);
    }

    @SuppressWarnings("DataFlowIssue")
    private List<HttpServiceArgumentResolver> initArgumentResolvers() {
      List<HttpServiceArgumentResolver> resolvers = new ArrayList<>(customArgumentResolvers);

      ConversionService service = conversionService != null ?
              conversionService : ApplicationConversionService.getSharedInstance();

      // Annotation-based
      resolvers.add(new RequestHeaderArgumentResolver(service));
      resolvers.add(new RequestBodyArgumentResolver(exchangeAdapter));
      resolvers.add(new PathVariableArgumentResolver(service));
      resolvers.add(new RequestParamArgumentResolver(service));
      resolvers.add(new RequestPartArgumentResolver(exchangeAdapter));
      resolvers.add(new CookieValueArgumentResolver(service));
      if (exchangeAdapter.supportsRequestAttributes()) {
        resolvers.add(new RequestAttributeArgumentResolver());
      }

      // Specific type
      resolvers.add(new UrlArgumentResolver());
      resolvers.add(new UriBuilderFactoryArgumentResolver());
      resolvers.add(new HttpMethodArgumentResolver());

      if (addFallbackArgumentResolver) {
        resolvers.add(new RequestParamArgumentResolver(service, true));
      }

      if (fallbackArgumentResolver != null) {
        resolvers.add(fallbackArgumentResolver);
      }
      return resolvers;
    }
  }

  /**
   * {@link MethodInterceptor} that invokes an {@link HttpServiceMethod}.
   */
  private static final class HttpServiceMethodInterceptor implements MethodInterceptor {

    private final Map<Method, HttpServiceMethod> httpServiceMethods;

    private HttpServiceMethodInterceptor(List<HttpServiceMethod> methods) {
      this.httpServiceMethods = methods.stream()
              .collect(Collectors.toMap(HttpServiceMethod::getMethod, Function.identity()));
    }

    @Nullable
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Method method = invocation.getMethod();
      HttpServiceMethod httpServiceMethod = httpServiceMethods.get(method);
      if (httpServiceMethod != null) {
        return httpServiceMethod.invoke(invocation.getArguments());
      }
      if (method.isDefault()) {
        if (invocation instanceof ProxyMethodInvocation inv) {
          Object proxy = inv.getProxy();
          return InvocationHandler.invokeDefault(proxy, method, invocation.getArguments());
        }
      }
      throw new IllegalStateException("Unexpected method invocation: " + method);
    }
  }

  /**
   * Processor that delegates to a list of other processors.
   */
  private record CompositeHttpRequestValuesProcessor(List<HttpRequestValues.Processor> processors)
          implements HttpRequestValues.Processor {

    @Override
    @SuppressWarnings("NullAway")
    public void process(Method method, MethodParameter[] parameters, @Nullable Object[] arguments,
            HttpRequestValues.Builder builder) {

      for (HttpRequestValues.Processor processor : this.processors) {
        processor.process(method, parameters, arguments, builder);
      }
    }
  }

}
