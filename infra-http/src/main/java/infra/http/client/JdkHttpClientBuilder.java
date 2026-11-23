/*
 * Copyright 2012-present the original author or authors.
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

package infra.http.client;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import javax.net.ssl.SSLParameters;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslOptions;
import infra.util.Assert;

/**
 * Builder that can be used to create a JDK {@link HttpClient}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public final class JdkHttpClientBuilder {

  private static final Method builderProxyMethod;

  private static final Method builderCookieHandlerMethod;

  static {
    try {
      builderProxyMethod = HttpClient.Builder.class.getMethod("proxy", ProxySelector.class);
      builderCookieHandlerMethod = HttpClient.Builder.class.getMethod("cookieHandler", CookieHandler.class);
    }
    catch (NoSuchMethodException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private final Consumer<HttpClient.Builder> customizer;

  private final @Nullable ProxySelector proxySelector;

  public JdkHttpClientBuilder() {
    this(Empty.consumer(), null);
  }

  private JdkHttpClientBuilder(Consumer<HttpClient.Builder> customizer, @Nullable ProxySelector proxySelector) {
    this.customizer = customizer;
    this.proxySelector = proxySelector;
  }

  /**
   * Return a new {@link JdkHttpClientBuilder} that uses the given executor with the
   * underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param executor the executor to use
   * @return a new {@link JdkHttpClientBuilder} instance
   */
  public JdkHttpClientBuilder withExecutor(Executor executor) {
    Assert.notNull(executor, "'executor' is required");
    return withCustomizer((httpClient) -> httpClient.executor(executor));
  }

  /**
   * Return a new {@link JdkHttpClientBuilder} that applies additional customization to
   * the underlying {@link java.net.http.HttpClient.Builder}.
   *
   * @param customizer the customizer to apply
   * @return a new {@link JdkHttpClientBuilder} instance
   */
  public JdkHttpClientBuilder withCustomizer(Consumer<HttpClient.Builder> customizer) {
    Assert.notNull(customizer, "'customizer' is required");
    return new JdkHttpClientBuilder(this.customizer.andThen(customizer), this.proxySelector);
  }

  /**
   * Return a new builder with a replacement {@link ProxySelector}.
   *
   * @param proxySelector the new proxy selector
   * @return a new {@link JdkHttpClientBuilder} instance
   * @since 5.0
   */
  public JdkHttpClientBuilder withProxySelector(ProxySelector proxySelector) {
    Assert.notNull(proxySelector, "ProxySelector is required");
    return new JdkHttpClientBuilder(this.customizer, proxySelector);
  }

  /**
   * Build a new {@link HttpClient} instance with the given settings applied.
   *
   * @param settings the settings to apply
   * @return a new {@link HttpClient} instance
   */
  public HttpClient build(@Nullable HttpClientSettings settings) {
    settings = settings != null ? settings : HttpClientSettings.defaults();
    Assert.isTrue(settings.readTimeout() == null, "'settings' must not have a 'readTimeout'");
    HttpClient.Builder builder = getBuilder(settings.inetAddressFilter());

    if (settings.cookieHandling() != null) {
      CookieHandler cookieHandler = asCookieHandler(settings.cookieHandling());
      if (cookieHandler != null || settings.inetAddressFilter() != null) {
        builder.cookieHandler(cookieHandler);
      }
    }

    builder.followRedirects(asHttpClientRedirect(settings.redirects()));

    if (settings.connectTimeout() != null) {
      builder.connectTimeout(settings.connectTimeout());
    }

    if (settings.sslBundle() != null) {
      builder.sslContext(settings.sslBundle().createSslContext());
      builder.sslParameters(asSslParameters(settings.sslBundle()));
    }

    if (this.proxySelector != null) {
      builder.proxy(this.proxySelector);
    }
    this.customizer.accept(builder);
    return builder.build();
  }

  private HttpClient.Builder getBuilder(@Nullable InetAddressFilter filter) {
    HttpClient.Builder delegate = HttpClient.newBuilder();
    if (filter == null) {
      return delegate;
    }
    UrlHostFilter hostFilter = new UrlHostFilter(filter);
    ProxySelector defaultProxySelector = ProxySelector.getDefault();
    delegate.proxy(new JdkFilteredProxySelector(
            defaultProxySelector != null ? defaultProxySelector : ProxySelector.of(null), hostFilter));
    delegate.cookieHandler(new JdkFilteredCookieHandler(CookieHandler.getDefault(), hostFilter));
    return (HttpClient.Builder) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { HttpClient.Builder.class }, (proxy, method, args) -> {
              if (builderProxyMethod.equals(method)) {
                args[0] = new JdkFilteredProxySelector((ProxySelector) args[0], hostFilter);
              }
              else if (builderCookieHandlerMethod.equals(method)) {
                args[0] = new JdkFilteredCookieHandler((CookieHandler) args[0], hostFilter);
              }
              try {
                Object result = method.invoke(delegate, args);
                return result == delegate ? proxy : result;
              }
              catch (InvocationTargetException ex) {
                throw ex.getCause();
              }
            });
  }

  private SSLParameters asSslParameters(SslBundle sslBundle) {
    SslOptions options = sslBundle.getOptions();
    SSLParameters parameters = new SSLParameters();
    parameters.setCipherSuites(options.getCiphers());
    parameters.setProtocols(options.getEnabledProtocols());
    return parameters;
  }

  private @Nullable CookieHandler asCookieHandler(HttpCookieHandling cookieHandling) {
    return switch (cookieHandling) {
      case ENABLE_WHEN_POSSIBLE, ENABLE -> new CookieManager();
      case DISABLE -> null;
    };
  }

  private Redirect asHttpClientRedirect(@Nullable HttpRedirects redirects) {
    if (redirects == null) {
      return Redirect.NORMAL;
    }
    return switch (redirects) {
      case FOLLOW_WHEN_POSSIBLE, FOLLOW -> Redirect.NORMAL;
      case DONT_FOLLOW -> Redirect.NEVER;
    };
  }

}
