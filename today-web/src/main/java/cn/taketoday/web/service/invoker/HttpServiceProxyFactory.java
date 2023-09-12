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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.core.MethodIntrospector;
import cn.taketoday.core.ReactiveAdapterRegistry;
import cn.taketoday.core.StringValueResolver;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.format.support.DefaultFormattingConversionService;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.reactive.function.client.support.WebClientAdapter;
import cn.taketoday.web.service.annotation.HttpExchange;

/**
 * Factory to create a client proxy from an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * <p>To create an instance, use static methods to obtain a
 * {@link Builder Builder}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see WebClientAdapter
 * @since 4.0
 */
public final class HttpServiceProxyFactory {

  private final HttpExchangeAdapter exchangeAdapter;

  private final List<HttpServiceArgumentResolver> argumentResolvers;

  @Nullable
  private final StringValueResolver embeddedValueResolver;

  private HttpServiceProxyFactory(HttpExchangeAdapter exchangeAdapter,
          List<HttpServiceArgumentResolver> argumentResolvers,
          @Nullable StringValueResolver embeddedValueResolver) {

    this.exchangeAdapter = exchangeAdapter;
    this.argumentResolvers = argumentResolvers;
    this.embeddedValueResolver = embeddedValueResolver;
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
    List<HttpServiceMethod> httpServiceMethods =
            MethodIntrospector.filterMethods(serviceType, this::isExchangeMethod).stream()
                    .map(method -> createHttpServiceMethod(serviceType, method))
                    .toList();

    return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(httpServiceMethods));
  }

  private boolean isExchangeMethod(Method method) {
    return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
  }

  private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
    Assert.notNull(this.argumentResolvers,
            "No argument resolvers: afterPropertiesSet was not called");

    return new HttpServiceMethod(
            method, serviceType, this.argumentResolvers, this.exchangeAdapter, this.embeddedValueResolver);
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

    @Nullable
    private HttpExchangeAdapter exchangeAdapter;

    private final List<HttpServiceArgumentResolver> customArgumentResolvers = new ArrayList<>();

    @Nullable
    private ConversionService conversionService;

    @Nullable
    private StringValueResolver embeddedValueResolver;

    /**
     * Provide the HTTP client to perform requests through.
     *
     * @param adapter a client adapted to {@link HttpExchangeAdapter}
     * @return this same builder instance
     */
    public Builder exchangeAdapter(HttpExchangeAdapter adapter) {
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
     * Set the {@link ConversionService} to use where input values need to
     * be formatted as Strings.
     * <p>By default this is {@link DefaultFormattingConversionService}.
     *
     * @return this same builder instance
     */
    public Builder conversionService(ConversionService conversionService) {
      this.conversionService = conversionService;
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
              exchangeAdapter, initArgumentResolvers(), embeddedValueResolver);
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

      // Custom
      List<HttpServiceArgumentResolver> resolvers = new ArrayList<>(customArgumentResolvers);

      ConversionService service = (conversionService != null ?
                                   conversionService : new DefaultFormattingConversionService());

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
      resolvers.add(new HttpMethodArgumentResolver());

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

}
