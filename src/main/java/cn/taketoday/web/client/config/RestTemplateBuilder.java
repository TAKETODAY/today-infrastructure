/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.web.client.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.support.BeanUtils;
import cn.taketoday.http.client.ClientHttpRequest;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestFactoryWrapper;
import cn.taketoday.http.client.ClientHttpRequestInterceptor;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.http.converter.HttpMessageConverter;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.web.client.ResponseErrorHandler;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.util.UriTemplateHandler;
import reactor.netty.http.client.HttpClientRequest;

/**
 * Builder that can be used to configure and create a {@link RestTemplate}. Provides
 * convenience methods to register {@link #messageConverters(HttpMessageConverter...)
 * converters}, {@link #errorHandler(ResponseErrorHandler) error handlers} and
 * {@link #uriTemplateHandler(UriTemplateHandler) UriTemplateHandlers}.
 * <p>
 * By default the built {@link RestTemplate} will attempt to use the most suitable
 * {@link ClientHttpRequestFactory}, call {@link #detectRequestFactory(boolean)
 * detectRequestFactory(false)} if you prefer to keep the default. In a typical
 * auto-configured application this builder is available as a bean and can be
 * injected whenever a {@link RestTemplate} is needed.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Brian Clozel
 * @author Dmytro Nosan
 * @author Kevin Strijbos
 * @author Ilya Lukyanovich
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RestTemplateBuilder {

  private final RequestFactoryCustomizer requestFactoryCustomizer;

  private final boolean detectRequestFactory;

  @Nullable
  private final String rootUri;

  @Nullable
  private final Set<HttpMessageConverter<?>> messageConverters;

  private final Set<ClientHttpRequestInterceptor> interceptors;

  @Nullable
  private final Supplier<ClientHttpRequestFactory> requestFactory;

  @Nullable
  private final UriTemplateHandler uriTemplateHandler;

  @Nullable
  private final ResponseErrorHandler errorHandler;

  @Nullable
  private final BasicAuthentication basicAuthentication;

  private final Map<String, List<String>> defaultHeaders;

  private final Set<RestTemplateCustomizer> customizers;

  private final Set<RestTemplateRequestCustomizer<?>> requestCustomizers;

  /**
   * Create a new {@link RestTemplateBuilder} instance.
   *
   * @param customizers any {@link RestTemplateCustomizer RestTemplateCustomizers} that
   * should be applied when the {@link RestTemplate} is built
   */
  public RestTemplateBuilder(RestTemplateCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    this.requestFactoryCustomizer = new RequestFactoryCustomizer();
    this.detectRequestFactory = true;
    this.rootUri = null;
    this.messageConverters = null;
    this.interceptors = Collections.emptySet();
    this.requestFactory = null;
    this.uriTemplateHandler = null;
    this.errorHandler = null;
    this.basicAuthentication = null;
    this.defaultHeaders = Collections.emptyMap();
    this.customizers = copiedSetOf(customizers);
    this.requestCustomizers = Collections.emptySet();
  }

  private RestTemplateBuilder(
          RequestFactoryCustomizer requestFactoryCustomizer,
          boolean detectRequestFactory,
          @Nullable String rootUri,
          @Nullable Set<HttpMessageConverter<?>> messageConverters,
          Set<ClientHttpRequestInterceptor> interceptors,
          @Nullable Supplier<ClientHttpRequestFactory> requestFactorySupplier,
          @Nullable UriTemplateHandler uriTemplateHandler,
          @Nullable ResponseErrorHandler errorHandler,
          @Nullable BasicAuthentication basicAuthentication,
          Map<String, List<String>> defaultHeaders,
          Set<RestTemplateCustomizer> customizers,
          Set<RestTemplateRequestCustomizer<?>> requestCustomizers
  ) {
    this.requestFactoryCustomizer = requestFactoryCustomizer;
    this.detectRequestFactory = detectRequestFactory;
    this.rootUri = rootUri;
    this.messageConverters = messageConverters;
    this.interceptors = interceptors;
    this.requestFactory = requestFactorySupplier;
    this.uriTemplateHandler = uriTemplateHandler;
    this.errorHandler = errorHandler;
    this.basicAuthentication = basicAuthentication;
    this.defaultHeaders = defaultHeaders;
    this.customizers = customizers;
    this.requestCustomizers = requestCustomizers;
  }

  /**
   * Set if the {@link ClientHttpRequestFactory} should be detected based on the
   * classpath. Default if {@code true}.
   *
   * @param detectRequestFactory if the {@link ClientHttpRequestFactory} should be
   * detected
   * @return a new builder instance
   */
  public RestTemplateBuilder detectRequestFactory(boolean detectRequestFactory) {
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, defaultHeaders, customizers, requestCustomizers);
  }

  /**
   * Set a root URL that should be applied to each request that starts with {@code '/'}.
   * Since this works by adding a {@link UriTemplateHandler} to the
   * {@link RestTemplate}, the root URL will only apply when {@code String} variants of
   * the {@link RestTemplate} methods are used for specifying the request URL. See
   * {@link RootUriTemplateHandler} for details.
   *
   * @param rootUri the root URI or {@code null}
   * @return a new builder instance
   */
  public RestTemplateBuilder rootUri(String rootUri) {
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, defaultHeaders, customizers, requestCustomizers);
  }

  /**
   * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
   * the {@link RestTemplate}. Setting this value will replace any previously configured
   * converters and any converters configured on the builder will replace RestTemplate's
   * default converters.
   *
   * @param messageConverters the converters to set
   * @return a new builder instance
   * @see #additionalMessageConverters(HttpMessageConverter...)
   */
  public RestTemplateBuilder messageConverters(HttpMessageConverter<?>... messageConverters) {
    Assert.notNull(messageConverters, "MessageConverters must not be null");
    return messageConverters(Arrays.asList(messageConverters));
  }

  /**
   * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
   * the {@link RestTemplate}. Setting this value will replace any previously configured
   * converters and any converters configured on the builder will replace RestTemplate's
   * default converters.
   *
   * @param messageConverters the converters to set
   * @return a new builder instance
   * @see #additionalMessageConverters(HttpMessageConverter...)
   */
  public RestTemplateBuilder messageConverters(Collection<? extends HttpMessageConverter<?>> messageConverters) {
    Assert.notNull(messageConverters, "MessageConverters must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            copiedSetOf(messageConverters), interceptors, requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, defaultHeaders, customizers, requestCustomizers);
  }

  /**
   * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
   * used with the {@link RestTemplate}. Any converters configured on the builder will
   * replace RestTemplate's default converters.
   *
   * @param messageConverters the converters to add
   * @return a new builder instance
   * @see #messageConverters(HttpMessageConverter...)
   */
  public RestTemplateBuilder additionalMessageConverters(HttpMessageConverter<?>... messageConverters) {
    Assert.notNull(messageConverters, "MessageConverters must not be null");
    return additionalMessageConverters(Arrays.asList(messageConverters));
  }

  /**
   * Add additional {@link HttpMessageConverter HttpMessageConverters} that should be
   * used with the {@link RestTemplate}. Any converters configured on the builder will
   * replace RestTemplate's default converters.
   *
   * @param messageConverters the converters to add
   * @return a new builder instance
   * @see #messageConverters(HttpMessageConverter...)
   */
  public RestTemplateBuilder additionalMessageConverters(
          Collection<? extends HttpMessageConverter<?>> messageConverters) {
    Assert.notNull(messageConverters, "MessageConverters must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            append(this.messageConverters, messageConverters), interceptors, requestFactory,
            uriTemplateHandler, errorHandler, basicAuthentication, defaultHeaders,
            customizers, requestCustomizers);
  }

  /**
   * Set the {@link HttpMessageConverter HttpMessageConverters} that should be used with
   * the {@link RestTemplate} to the default set. Calling this method will replace any
   * previously defined converters.
   *
   * @return a new builder instance
   * @see #messageConverters(HttpMessageConverter...)
   */
  public RestTemplateBuilder defaultMessageConverters() {
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            copiedSetOf(new RestTemplate().getMessageConverters()), interceptors, requestFactory,
            uriTemplateHandler, errorHandler, basicAuthentication, defaultHeaders,
            customizers, requestCustomizers);
  }

  /**
   * Set the {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors} that
   * should be used with the {@link RestTemplate}. Setting this value will replace any
   * previously defined interceptors.
   *
   * @param interceptors the interceptors to set
   * @return a new builder instance
   * @see #additionalInterceptors(ClientHttpRequestInterceptor...)
   */
  public RestTemplateBuilder interceptors(ClientHttpRequestInterceptor... interceptors) {
    Assert.notNull(interceptors, "interceptors must not be null");
    return interceptors(Arrays.asList(interceptors));
  }

  /**
   * Set the {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors} that
   * should be used with the {@link RestTemplate}. Setting this value will replace any
   * previously defined interceptors.
   *
   * @param interceptors the interceptors to set
   * @return a new builder instance
   * @see #additionalInterceptors(ClientHttpRequestInterceptor...)
   */
  public RestTemplateBuilder interceptors(Collection<ClientHttpRequestInterceptor> interceptors) {
    Assert.notNull(interceptors, "interceptors must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, copiedSetOf(interceptors), requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, defaultHeaders, customizers, requestCustomizers);
  }

  /**
   * Add additional {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}
   * that should be used with the {@link RestTemplate}.
   *
   * @param interceptors the interceptors to add
   * @return a new builder instance
   * @see #interceptors(ClientHttpRequestInterceptor...)
   */
  public RestTemplateBuilder additionalInterceptors(ClientHttpRequestInterceptor... interceptors) {
    Assert.notNull(interceptors, "interceptors must not be null");
    return additionalInterceptors(Arrays.asList(interceptors));
  }

  /**
   * Add additional {@link ClientHttpRequestInterceptor ClientHttpRequestInterceptors}
   * that should be used with the {@link RestTemplate}.
   *
   * @param interceptors the interceptors to add
   * @return a new builder instance
   * @see #interceptors(ClientHttpRequestInterceptor...)
   */
  public RestTemplateBuilder additionalInterceptors(Collection<? extends ClientHttpRequestInterceptor> interceptors) {
    Assert.notNull(interceptors, "interceptors must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, append(this.interceptors, interceptors), requestFactory,
            uriTemplateHandler, errorHandler, basicAuthentication, defaultHeaders,
            customizers, requestCustomizers);
  }

  /**
   * Set the {@link ClientHttpRequestFactory} class that should be used with the
   * {@link RestTemplate}.
   *
   * @param requestFactory the request factory to use
   * @return a new builder instance
   */
  public RestTemplateBuilder requestFactory(Class<? extends ClientHttpRequestFactory> requestFactory) {
    Assert.notNull(requestFactory, "RequestFactory must not be null");
    return requestFactory(() -> createRequestFactory(requestFactory));
  }

  private ClientHttpRequestFactory createRequestFactory(Class<? extends ClientHttpRequestFactory> requestFactory) {
    try {
      Constructor<?> constructor = requestFactory.getDeclaredConstructor();
      constructor.setAccessible(true);
      return (ClientHttpRequestFactory) constructor.newInstance();
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Set the {@code Supplier} of {@link ClientHttpRequestFactory} that should be called
   * each time we {@link #build()} a new {@link RestTemplate} instance.
   *
   * @param requestFactory the supplier for the request factory
   * @return a new builder instance
   */
  public RestTemplateBuilder requestFactory(Supplier<ClientHttpRequestFactory> requestFactory) {
    Assert.notNull(requestFactory, "RequestFactory Supplier must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler, errorHandler,
            basicAuthentication, defaultHeaders, customizers, requestCustomizers);
  }

  /**
   * Set the {@link UriTemplateHandler} that should be used with the
   * {@link RestTemplate}.
   *
   * @param uriTemplateHandler the URI template handler to use
   * @return a new builder instance
   */
  public RestTemplateBuilder uriTemplateHandler(UriTemplateHandler uriTemplateHandler) {
    Assert.notNull(uriTemplateHandler, "UriTemplateHandler must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler, errorHandler,
            basicAuthentication, defaultHeaders, customizers, requestCustomizers);
  }

  /**
   * Set the {@link ResponseErrorHandler} that should be used with the
   * {@link RestTemplate}.
   *
   * @param errorHandler the error handler to use
   * @return a new builder instance
   */
  public RestTemplateBuilder errorHandler(ResponseErrorHandler errorHandler) {
    Assert.notNull(errorHandler, "ErrorHandler must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler, errorHandler,
            basicAuthentication, defaultHeaders, customizers, requestCustomizers);
  }

  /**
   * Add HTTP Basic Authentication to requests with the given username/password pair,
   * unless a custom Authorization header has been set before.
   *
   * @param username the user name
   * @param password the password
   * @return a new builder instance
   * @see #basicAuthentication(String, String, Charset)
   */
  public RestTemplateBuilder basicAuthentication(String username, String password) {
    return basicAuthentication(username, password, null);
  }

  /**
   * Add HTTP Basic Authentication to requests with the given username/password pair,
   * unless a custom Authorization header has been set before.
   *
   * @param username the user name
   * @param password the password
   * @param charset the charset to use
   * @return a new builder instance
   */
  public RestTemplateBuilder basicAuthentication(String username, String password, Charset charset) {
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler,
            errorHandler, new BasicAuthentication(username, password, charset), defaultHeaders,
            customizers, requestCustomizers);
  }

  /**
   * Add a default header that will be set if not already present on the outgoing
   * {@link HttpClientRequest}.
   *
   * @param name the name of the header
   * @param values the header values
   * @return a new builder instance
   */
  public RestTemplateBuilder defaultHeader(String name, String... values) {
    Assert.notNull(name, "Name must not be null");
    Assert.notNull(values, "Values must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, append(defaultHeaders, name, values),
            customizers, requestCustomizers);
  }

  /**
   * Sets the connection timeout on the underlying {@link ClientHttpRequestFactory}.
   *
   * @param connectTimeout the connection timeout
   * @return a new builder instance.
   */
  public RestTemplateBuilder setConnectTimeout(Duration connectTimeout) {
    return new RestTemplateBuilder(requestFactoryCustomizer.connectTimeout(connectTimeout),
            detectRequestFactory, rootUri, messageConverters, interceptors, requestFactory,
            uriTemplateHandler, errorHandler, basicAuthentication, defaultHeaders,
            customizers, requestCustomizers);
  }

  /**
   * Sets the read timeout on the underlying {@link ClientHttpRequestFactory}.
   *
   * @param readTimeout the read timeout
   * @return a new builder instance.
   */
  public RestTemplateBuilder setReadTimeout(Duration readTimeout) {
    return new RestTemplateBuilder(requestFactoryCustomizer.readTimeout(readTimeout),
            detectRequestFactory, rootUri, messageConverters, interceptors, requestFactory,
            uriTemplateHandler, errorHandler, basicAuthentication, defaultHeaders,
            customizers, requestCustomizers);
  }

  /**
   * Sets if the underlying {@link ClientHttpRequestFactory} should buffer the
   * {@linkplain ClientHttpRequest#getBody() request body} internally.
   *
   * @param bufferRequestBody value of the bufferRequestBody parameter
   * @return a new builder instance.
   * @see SimpleClientHttpRequestFactory#setBufferRequestBody(boolean)
   * @see HttpComponentsClientHttpRequestFactory#setBufferRequestBody(boolean)
   */
  public RestTemplateBuilder setBufferRequestBody(boolean bufferRequestBody) {
    return new RestTemplateBuilder(requestFactoryCustomizer.bufferRequestBody(bufferRequestBody),
            detectRequestFactory, rootUri, messageConverters, interceptors, requestFactory,
            uriTemplateHandler, errorHandler, basicAuthentication, defaultHeaders,
            customizers, requestCustomizers);
  }

  /**
   * Set the {@link RestTemplateCustomizer RestTemplateCustomizers} that should be
   * applied to the {@link RestTemplate}. Customizers are applied in the order that they
   * were added after builder configuration has been applied. Setting this value will
   * replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(RestTemplateCustomizer...)
   */
  public RestTemplateBuilder customizers(RestTemplateCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    return customizers(Arrays.asList(customizers));
  }

  /**
   * Set the {@link RestTemplateCustomizer RestTemplateCustomizers} that should be
   * applied to the {@link RestTemplate}. Customizers are applied in the order that they
   * were added after builder configuration has been applied. Setting this value will
   * replace any previously configured customizers.
   *
   * @param customizers the customizers to set
   * @return a new builder instance
   * @see #additionalCustomizers(RestTemplateCustomizer...)
   */
  public RestTemplateBuilder customizers(Collection<? extends RestTemplateCustomizer> customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, defaultHeaders, copiedSetOf(customizers),
            requestCustomizers);
  }

  /**
   * Add {@link RestTemplateCustomizer RestTemplateCustomizers} that should be applied
   * to the {@link RestTemplate}. Customizers are applied in the order that they were
   * added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(RestTemplateCustomizer...)
   */
  public RestTemplateBuilder additionalCustomizers(RestTemplateCustomizer... customizers) {
    Assert.notNull(customizers, "Customizers must not be null");
    return additionalCustomizers(Arrays.asList(customizers));
  }

  /**
   * Add {@link RestTemplateCustomizer RestTemplateCustomizers} that should be applied
   * to the {@link RestTemplate}. Customizers are applied in the order that they were
   * added after builder configuration has been applied.
   *
   * @param customizers the customizers to add
   * @return a new builder instance
   * @see #customizers(RestTemplateCustomizer...)
   */
  public RestTemplateBuilder additionalCustomizers(Collection<? extends RestTemplateCustomizer> customizers) {
    Assert.notNull(customizers, "RestTemplateCustomizers must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, defaultHeaders, append(this.customizers, customizers),
            requestCustomizers);
  }

  /**
   * Set the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
   * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
   * order that they were added. Setting this value will replace any previously
   * configured request customizers.
   *
   * @param requestCustomizers the request customizers to set
   * @return a new builder instance
   * @see #additionalRequestCustomizers(RestTemplateRequestCustomizer...)
   */
  public RestTemplateBuilder requestCustomizers(RestTemplateRequestCustomizer<?>... requestCustomizers) {
    Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
    return requestCustomizers(Arrays.asList(requestCustomizers));
  }

  /**
   * Set the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
   * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
   * order that they were added. Setting this value will replace any previously
   * configured request customizers.
   *
   * @param requestCustomizers the request customizers to set
   * @return a new builder instance
   * @see #additionalRequestCustomizers(RestTemplateRequestCustomizer...)
   */
  public RestTemplateBuilder requestCustomizers(
          Collection<? extends RestTemplateRequestCustomizer<?>> requestCustomizers) {
    Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
    return new RestTemplateBuilder(requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory, uriTemplateHandler,
            errorHandler, basicAuthentication, defaultHeaders, customizers,
            copiedSetOf(requestCustomizers));
  }

  /**
   * Add the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
   * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
   * order that they were added.
   *
   * @param requestCustomizers the request customizers to add
   * @return a new builder instance
   * @see #requestCustomizers(RestTemplateRequestCustomizer...)
   */
  public RestTemplateBuilder additionalRequestCustomizers(RestTemplateRequestCustomizer<?>... requestCustomizers) {
    Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
    return additionalRequestCustomizers(Arrays.asList(requestCustomizers));
  }

  /**
   * Add the {@link RestTemplateRequestCustomizer RestTemplateRequestCustomizers} that
   * should be applied to the {@link ClientHttpRequest}. Customizers are applied in the
   * order that they were added.
   *
   * @param requestCustomizers the request customizers to add
   * @return a new builder instance
   * @see #requestCustomizers(Collection)
   */
  public RestTemplateBuilder additionalRequestCustomizers(
          Collection<? extends RestTemplateRequestCustomizer<?>> requestCustomizers) {
    Assert.notNull(requestCustomizers, "RequestCustomizers must not be null");
    return new RestTemplateBuilder(
            requestFactoryCustomizer, detectRequestFactory, rootUri,
            messageConverters, interceptors, requestFactory,
            uriTemplateHandler, errorHandler, basicAuthentication,
            defaultHeaders, customizers, append(this.requestCustomizers, requestCustomizers));
  }

  /**
   * Build a new {@link RestTemplate} instance and configure it using this builder.
   *
   * @return a configured {@link RestTemplate} instance.
   * @see #build(Class)
   * @see #configure(RestTemplate)
   */
  public RestTemplate build() {
    return configure(new RestTemplate());
  }

  /**
   * Build a new {@link RestTemplate} instance of the specified type and configure it
   * using this builder.
   *
   * @param <T> the type of rest template
   * @param restTemplateClass the template type to create
   * @return a configured {@link RestTemplate} instance.
   * @see RestTemplateBuilder#build()
   * @see #configure(RestTemplate)
   */
  public <T extends RestTemplate> T build(Class<T> restTemplateClass) {
    return configure(BeanUtils.newInstance(restTemplateClass));
  }

  /**
   * Configure the provided {@link RestTemplate} instance using this builder.
   *
   * @param <T> the type of rest template
   * @param restTemplate the {@link RestTemplate} to configure
   * @return the rest template instance
   * @see RestTemplateBuilder#build()
   * @see RestTemplateBuilder#build(Class)
   */
  public <T extends RestTemplate> T configure(T restTemplate) {
    ClientHttpRequestFactory requestFactory = buildRequestFactory();
    if (requestFactory != null) {
      restTemplate.setRequestFactory(requestFactory);
    }
    addClientHttpRequestInitializer(restTemplate);
    if (!CollectionUtils.isEmpty(messageConverters)) {
      restTemplate.setMessageConverters(new ArrayList<>(messageConverters));
    }
    if (uriTemplateHandler != null) {
      restTemplate.setUriTemplateHandler(uriTemplateHandler);
    }
    if (errorHandler != null) {
      restTemplate.setErrorHandler(errorHandler);
    }
    if (rootUri != null) {
      RootUriTemplateHandler.addTo(restTemplate, rootUri);
    }
    restTemplate.getInterceptors().addAll(interceptors);
    if (!CollectionUtils.isEmpty(customizers)) {
      for (RestTemplateCustomizer customizer : customizers) {
        customizer.customize(restTemplate);
      }
    }
    return restTemplate;
  }

  /**
   * Build a new {@link ClientHttpRequestFactory} instance using the settings of this
   * builder.
   *
   * @return a {@link ClientHttpRequestFactory} or {@code null}
   */
  @Nullable
  public ClientHttpRequestFactory buildRequestFactory() {
    ClientHttpRequestFactory requestFactory = null;
    if (this.requestFactory != null) {
      requestFactory = this.requestFactory.get();
    }
    else if (this.detectRequestFactory) {
      requestFactory = new ClientHttpRequestFactorySupplier().get();
    }
    if (requestFactory != null) {
      if (this.requestFactoryCustomizer != null) {
        this.requestFactoryCustomizer.accept(requestFactory);
      }
    }
    return requestFactory;
  }

  private void addClientHttpRequestInitializer(RestTemplate restTemplate) {
    if (this.basicAuthentication == null && this.defaultHeaders.isEmpty() && this.requestCustomizers.isEmpty()) {
      return;
    }
    restTemplate.getHttpRequestInitializers().add(
            new RestTemplateBuilderClientHttpRequestInitializer(
                    this.basicAuthentication, this.defaultHeaders, this.requestCustomizers));
  }

  @SuppressWarnings("unchecked")
  private <T> Set<T> copiedSetOf(T... items) {
    return copiedSetOf(Arrays.asList(items));
  }

  private <T> Set<T> copiedSetOf(Collection<? extends T> collection) {
    return Collections.unmodifiableSet(new LinkedHashSet<>(collection));
  }

  private static <T> List<T> copiedListOf(T[] items) {
    return List.of(Arrays.copyOf(items, items.length));
  }

  private static <T> Set<T> append(Collection<? extends T> collection, Collection<? extends T> additions) {
    LinkedHashSet<T> result = new LinkedHashSet<>(
            collection != null ? collection : Collections.emptySet());
    if (additions != null) {
      result.addAll(additions);
    }
    return Collections.unmodifiableSet(result);
  }

  private static <K, V> Map<K, List<V>> append(Map<K, List<V>> map, K key, V[] values) {
    LinkedHashMap<K, List<V>> result = new LinkedHashMap<>(map != null ? map : Collections.emptyMap());
    if (values != null) {
      result.put(key, copiedListOf(values));
    }
    return Collections.unmodifiableMap(result);
  }

  /**
   * Internal customizer used to apply {@link ClientHttpRequestFactory} settings.
   */
  private static class RequestFactoryCustomizer implements Consumer<ClientHttpRequestFactory> {
    @Nullable
    private final Duration readTimeout;

    @Nullable
    private final Duration connectTimeout;

    @Nullable
    private final Boolean bufferRequestBody;

    RequestFactoryCustomizer() {
      this(null, null, null);
    }

    private RequestFactoryCustomizer(
            @Nullable Duration connectTimeout, @Nullable Duration readTimeout, @Nullable Boolean bufferRequestBody) {
      this.readTimeout = readTimeout;
      this.connectTimeout = connectTimeout;
      this.bufferRequestBody = bufferRequestBody;
    }

    RequestFactoryCustomizer connectTimeout(Duration connectTimeout) {
      return new RequestFactoryCustomizer(connectTimeout, readTimeout, bufferRequestBody);
    }

    RequestFactoryCustomizer readTimeout(Duration readTimeout) {
      return new RequestFactoryCustomizer(connectTimeout, readTimeout, bufferRequestBody);
    }

    RequestFactoryCustomizer bufferRequestBody(boolean bufferRequestBody) {
      return new RequestFactoryCustomizer(connectTimeout, readTimeout, bufferRequestBody);
    }

    @Override
    public void accept(ClientHttpRequestFactory requestFactory) {
      ClientHttpRequestFactory unwrappedRequestFactory = unwrapRequestFactoryIfNecessary(requestFactory);
      if (connectTimeout != null) {
        setConnectTimeout(unwrappedRequestFactory, connectTimeout);
      }
      if (readTimeout != null) {
        setReadTimeout(unwrappedRequestFactory, readTimeout);
      }
      if (bufferRequestBody != null) {
        setBufferRequestBody(unwrappedRequestFactory);
      }
    }

    private ClientHttpRequestFactory unwrapRequestFactoryIfNecessary(ClientHttpRequestFactory requestFactory) {
      if (requestFactory instanceof ClientHttpRequestFactoryWrapper) {
        Field field = ReflectionUtils.findField(ClientHttpRequestFactoryWrapper.class, "requestFactory");
        ReflectionUtils.makeAccessible(field);
        ClientHttpRequestFactory unwrappedRequestFactory = requestFactory;
        while (unwrappedRequestFactory instanceof ClientHttpRequestFactoryWrapper) {
          unwrappedRequestFactory = (ClientHttpRequestFactory) ReflectionUtils.getField(
                  field, unwrappedRequestFactory);
        }
        return unwrappedRequestFactory;
      }
      return requestFactory;
    }

    private void setConnectTimeout(ClientHttpRequestFactory factory, Duration connectTimeout) {
      Method method = findMethod(factory, "setConnectTimeout", int.class);
      int timeout = Math.toIntExact(connectTimeout.toMillis());
      invoke(factory, method, timeout);
    }

    private void setReadTimeout(ClientHttpRequestFactory factory, Duration readTimeout) {
      Method method = findMethod(factory, "setReadTimeout", int.class);
      int timeout = Math.toIntExact(readTimeout.toMillis());
      invoke(factory, method, timeout);
    }

    private void setBufferRequestBody(ClientHttpRequestFactory factory) {
      Method method = findMethod(factory, "setBufferRequestBody", boolean.class);
      invoke(factory, method, bufferRequestBody);
    }

    private Method findMethod(ClientHttpRequestFactory requestFactory, String methodName, Class<?>... parameters) {
      Method method = ReflectionUtils.findMethod(requestFactory.getClass(), methodName, parameters);
      if (method != null) {
        return method;
      }
      throw new IllegalStateException("Request factory " + requestFactory.getClass()
              + " does not have a suitable " + methodName + " method");
    }

    private void invoke(ClientHttpRequestFactory requestFactory, Method method, Object... parameters) {
      ReflectionUtils.invokeMethod(method, requestFactory, parameters);
    }

  }

}
