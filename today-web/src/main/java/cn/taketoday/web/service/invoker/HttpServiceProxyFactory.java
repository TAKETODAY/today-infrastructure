/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

import cn.taketoday.aop.ProxyMethodInvocation;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.expression.EmbeddedValueResolverAware;
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
 * Factory for creating a client proxy given an HTTP service interface with
 * {@link HttpExchange @HttpExchange} methods.
 *
 * <p>This class is intended to be declared as a bean in a Infra configuration.
 *
 * @author Rossen Stoyanchev
 * @see WebClientAdapter
 * @since 4.0
 */
public final class HttpServiceProxyFactory implements InitializingBean, EmbeddedValueResolverAware {

  private final HttpClientAdapter clientAdapter;

  @Nullable
  private List<HttpServiceArgumentResolver> customArgumentResolvers;

  @Nullable
  private List<HttpServiceArgumentResolver> argumentResolvers;

  @Nullable
  private ConversionService conversionService;

  @Nullable
  private StringValueResolver embeddedValueResolver;

  private ReactiveAdapterRegistry reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();

  private Duration blockTimeout = Duration.ofSeconds(5);

  /**
   * Create an instance with the underlying HTTP client to use.
   *
   * @param clientAdapter an adapter for the client
   * @see WebClientAdapter#createHttpServiceProxyFactory(cn.taketoday.web.reactive.function.client.WebClient)
   */
  public HttpServiceProxyFactory(HttpClientAdapter clientAdapter) {
    Assert.notNull(clientAdapter, "HttpClientAdapter is required");
    this.clientAdapter = clientAdapter;
  }

  /**
   * Register a custom argument resolver, invoked ahead of default resolvers.
   *
   * @param resolver the resolver to add
   */
  public void addCustomArgumentResolver(HttpServiceArgumentResolver resolver) {
    if (this.customArgumentResolvers == null) {
      this.customArgumentResolvers = new ArrayList<>();
    }
    this.customArgumentResolvers.add(resolver);
  }

  /**
   * Set the custom argument resolvers to use, ahead of default resolvers.
   *
   * @param resolvers the resolvers to use
   */
  public void setCustomArgumentResolvers(List<HttpServiceArgumentResolver> resolvers) {
    this.customArgumentResolvers = new ArrayList<>(resolvers);
  }

  /**
   * Set the {@link ConversionService} to use where input values need to
   * be formatted as Strings.
   * <p>By default this is {@link DefaultFormattingConversionService}.
   */
  public void setConversionService(ConversionService conversionService) {
    this.conversionService = conversionService;
  }

  /**
   * Set the StringValueResolver to use for resolving placeholders and
   * expressions in {@link HttpExchange#url()}.
   *
   * @param resolver the resolver to use
   */
  @Override
  public void setEmbeddedValueResolver(StringValueResolver resolver) {
    this.embeddedValueResolver = resolver;
  }

  /**
   * Set the {@link ReactiveAdapterRegistry} to use to support different
   * asynchronous types for HTTP service method return values.
   * <p>By default this is {@link ReactiveAdapterRegistry#getSharedInstance()}.
   */
  public void setReactiveAdapterRegistry(ReactiveAdapterRegistry registry) {
    this.reactiveAdapterRegistry = registry;
  }

  /**
   * Configure how long to wait for a response for an HTTP service method
   * with a synchronous (blocking) method signature.
   * <p>By default this is 5 seconds.
   *
   * @param blockTimeout the timeout value
   */
  public void setBlockTimeout(Duration blockTimeout) {
    this.blockTimeout = blockTimeout;
  }

  @Override
  public void afterPropertiesSet() {
    this.conversionService = (this.conversionService != null ?
                              this.conversionService : new DefaultFormattingConversionService());
    this.argumentResolvers = initArgumentResolvers(this.conversionService);
  }

  private List<HttpServiceArgumentResolver> initArgumentResolvers(ConversionService conversionService) {
    List<HttpServiceArgumentResolver> resolvers = new ArrayList<>();

    // Custom
    if (this.customArgumentResolvers != null) {
      resolvers.addAll(this.customArgumentResolvers);
    }

    // Annotation-based
    resolvers.add(new RequestHeaderArgumentResolver(conversionService));
    resolvers.add(new RequestBodyArgumentResolver(this.reactiveAdapterRegistry));
    resolvers.add(new PathVariableArgumentResolver(conversionService));
    resolvers.add(new RequestParamArgumentResolver(conversionService));
    resolvers.add(new RequestPartArgumentResolver(this.reactiveAdapterRegistry));
    resolvers.add(new CookieValueArgumentResolver(conversionService));
    resolvers.add(new RequestAttributeArgumentResolver());

    // Specific type
    resolvers.add(new UrlArgumentResolver());
    resolvers.add(new HttpMethodArgumentResolver());
    resolvers.add(new MultipartFileArgumentResolver());

    return resolvers;
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
    return ProxyFactory.getProxy(serviceType, new HttpServiceMethodInterceptor(serviceType));
  }

  private boolean isExchangeMethod(Method method) {
    return AnnotatedElementUtils.hasAnnotation(method, HttpExchange.class);
  }

  private <S> HttpServiceMethod createHttpServiceMethod(Class<S> serviceType, Method method) {
    Assert.notNull(this.argumentResolvers,
            "No argument resolvers: afterPropertiesSet was not called");

    return new HttpServiceMethod(
            method, serviceType, this.argumentResolvers, this.clientAdapter,
            this.embeddedValueResolver, this.reactiveAdapterRegistry, this.blockTimeout);
  }

  /**
   * {@link MethodInterceptor} that invokes an {@link HttpServiceMethod}.
   */
  private final class HttpServiceMethodInterceptor implements MethodInterceptor {
    private final Map<Method, HttpServiceMethod> httpServiceMethods;

    private HttpServiceMethodInterceptor(Class<?> serviceType) {
      this.httpServiceMethods = MethodIntrospector.selectMethods(serviceType, method -> {
        if (isExchangeMethod(method)) {
          return createHttpServiceMethod(serviceType, method);
        }
        return null;
      });
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
      Method method = invocation.getMethod();
      HttpServiceMethod httpServiceMethod = this.httpServiceMethods.get(method);
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
