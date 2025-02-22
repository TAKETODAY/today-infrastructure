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

package infra.web.client.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.SocketConfig;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.net.ssl.SSLException;

import infra.core.ssl.SslBundle;
import infra.core.ssl.SslManagerBundle;
import infra.core.ssl.SslOptions;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestFactoryWrapper;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.ReactorClientHttpRequestFactory;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.ClassUtils;
import infra.util.PropertyMapper;
import infra.util.ReflectionUtils;
import infra.util.function.ThrowingConsumer;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.tcp.SslProvider;

/**
 * Utility class that can be used to create {@link ClientHttpRequestFactory} instances
 * configured using given {@link ClientHttpRequestFactorySettings}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/1 22:40
 */
public abstract class ClientHttpRequestFactories {

  static final String APACHE_HTTP_CLIENT_CLASS = "org.apache.hc.client5.http.impl.classic.HttpClients";

  private static final boolean APACHE_HTTP_CLIENT_PRESENT = ClassUtils.isPresent(APACHE_HTTP_CLIENT_CLASS);

  static final String REACTOR_CLIENT_CLASS = "reactor.netty.http.client.HttpClient";

  private static final boolean REACTOR_CLIENT_PRESENT = ClassUtils.isPresent(REACTOR_CLIENT_CLASS);

  /**
   * Return a {@link ClientHttpRequestFactory} implementation with the given
   * {@code settings} applied. The first of the following implementations whose
   * dependencies {@link ClassUtils#isPresent are available} is returned:
   * <ol>
   * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
   * </ol>
   *
   * @param settings the settings to apply
   * @return a new {@link ClientHttpRequestFactory}
   */
  public static ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
    Assert.notNull(settings, "Settings is required");
    if (APACHE_HTTP_CLIENT_PRESENT) {
      return HttpComponents.get(settings);
    }
    if (REACTOR_CLIENT_PRESENT) {
      return Reactor.get(settings);
    }
    return Jdk.get(settings);
  }

  /**
   * Return a new {@link ClientHttpRequestFactory} of the given
   * {@code requestFactoryType}, applying {@link ClientHttpRequestFactorySettings} using
   * reflection if necessary. The following implementations are supported without the
   * use of reflection:
   * <ul>
   * <li>{@link HttpComponentsClientHttpRequestFactory}</li>
   * <li>{@link JdkClientHttpRequestFactory}</li>
   * </ul>
   * A {@code requestFactoryType} of {@link ClientHttpRequestFactory} is equivalent to
   * calling {@link #get(ClientHttpRequestFactorySettings)}.
   *
   * @param <T> the {@link ClientHttpRequestFactory} type
   * @param requestFactoryType the {@link ClientHttpRequestFactory} type
   * @param settings the settings to apply
   * @return a new {@link ClientHttpRequestFactory} instance
   */
  @SuppressWarnings("unchecked")
  public static <T extends ClientHttpRequestFactory> T get(Class<T> requestFactoryType, ClientHttpRequestFactorySettings settings) {
    Assert.notNull(settings, "Settings is required");
    if (requestFactoryType == ClientHttpRequestFactory.class) {
      return (T) get(settings);
    }
    if (requestFactoryType == HttpComponentsClientHttpRequestFactory.class) {
      return (T) HttpComponents.get(settings);
    }
    if (requestFactoryType == JdkClientHttpRequestFactory.class) {
      return (T) Jdk.get(settings);
    }
    return get(() -> createRequestFactory(requestFactoryType), settings);
  }

  /**
   * Return a new {@link ClientHttpRequestFactory} from the given supplier, applying
   * {@link ClientHttpRequestFactorySettings} using reflection.
   *
   * @param <T> the {@link ClientHttpRequestFactory} type
   * @param requestFactorySupplier the {@link ClientHttpRequestFactory} supplier
   * @param settings the settings to apply
   * @return a new {@link ClientHttpRequestFactory} instance
   */
  public static <T extends ClientHttpRequestFactory> T get(
          Supplier<T> requestFactorySupplier, ClientHttpRequestFactorySettings settings) {
    return Reflective.get(requestFactorySupplier, settings);
  }

  private static <T extends ClientHttpRequestFactory> T createRequestFactory(Class<T> requestFactory) {
    try {
      Constructor<T> constructor = requestFactory.getDeclaredConstructor();
      ReflectionUtils.makeAccessible(constructor);
      return constructor.newInstance();
    }
    catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Support for {@link HttpComponentsClientHttpRequestFactory}.
   */
  static class HttpComponents {

    static HttpComponentsClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
      var requestFactory = createRequestFactory(settings.readTimeout(), settings.sslBundle());
      if (settings.connectTimeout() != null) {
        requestFactory.setConnectTimeout((int) settings.connectTimeout().toMillis());
      }
      return requestFactory;
    }

    private static HttpComponentsClientHttpRequestFactory createRequestFactory(
            @Nullable Duration readTimeout, @Nullable SslBundle sslBundle) {
      return new HttpComponentsClientHttpRequestFactory(createHttpClient(readTimeout, sslBundle));
    }

    private static HttpClient createHttpClient(@Nullable Duration readTimeout, @Nullable SslBundle sslBundle) {
      var connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();
      if (readTimeout != null) {
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout((int) readTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .build();
        connectionManagerBuilder.setDefaultSocketConfig(socketConfig);
      }
      if (sslBundle != null) {
        SslOptions options = sslBundle.getOptions();
        var socketFactory = new SSLConnectionSocketFactory(sslBundle.createSslContext(),
                options.getEnabledProtocols(), options.getCiphers(), new DefaultHostnameVerifier());
        connectionManagerBuilder.setSSLSocketFactory(socketFactory);
      }

      var connectionManager = connectionManagerBuilder.useSystemProperties().build();
      return HttpClientBuilder.create()
              .useSystemProperties()
              .setConnectionManager(connectionManager).build();
    }

  }

  /**
   * Support for {@link JdkClientHttpRequestFactory}.
   */
  static class Jdk {

    static JdkClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
      java.net.http.HttpClient httpClient = createHttpClient(settings.connectTimeout(), settings.sslBundle());
      JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(settings::readTimeout).to(requestFactory::setReadTimeout);
      return requestFactory;
    }

    private static java.net.http.HttpClient createHttpClient(
            @Nullable Duration connectTimeout, @Nullable SslBundle sslBundle) {
      java.net.http.HttpClient.Builder builder = java.net.http.HttpClient.newBuilder();
      if (connectTimeout != null) {
        builder.connectTimeout(connectTimeout);
      }
      if (sslBundle != null) {
        builder.sslContext(sslBundle.createSslContext());
      }
      return builder.build();
    }

  }

  /**
   * Support for {@link ReactorClientHttpRequestFactory}.
   */
  static class Reactor {

    static ReactorClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
      ReactorClientHttpRequestFactory requestFactory = createRequestFactory(settings.sslBundle());
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
      map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
      return requestFactory;
    }

    private static ReactorClientHttpRequestFactory createRequestFactory(@Nullable SslBundle sslBundle) {
      if (sslBundle != null) {
        var httpClient = reactor.netty.http.client.HttpClient.create()
                .secure((ThrowingConsumer.of((spec) -> configureSsl(spec, sslBundle))));
        return new ReactorClientHttpRequestFactory(httpClient);
      }
      return new ReactorClientHttpRequestFactory();
    }

    private static void configureSsl(SslProvider.SslContextSpec spec, SslBundle sslBundle) throws SSLException {
      SslOptions options = sslBundle.getOptions();
      SslManagerBundle managers = sslBundle.getManagers();
      SslContextBuilder builder = SslContextBuilder.forClient()
              .keyManager(managers.getKeyManagerFactory())
              .trustManager(managers.getTrustManagerFactory())
              .ciphers(SslOptions.asSet(options.getCiphers()))
              .protocols(options.getEnabledProtocols());
      spec.sslContext(builder.build());
    }

  }

  /**
   * Support for reflective configuration of an unknown {@link ClientHttpRequestFactory}
   * implementation.
   */
  static class Reflective {

    static <T extends ClientHttpRequestFactory> T get(Supplier<T> supplier, ClientHttpRequestFactorySettings settings) {
      T requestFactory = supplier.get();
      configure(requestFactory, settings);
      return requestFactory;
    }

    private static void configure(ClientHttpRequestFactory requestFactory, ClientHttpRequestFactorySettings settings) {
      ClientHttpRequestFactory unwrapped = unwrapIfNecessary(requestFactory);
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(settings::readTimeout).to(readTimeout -> setReadTimeout(unwrapped, readTimeout));
      map.from(settings::connectTimeout).to(connectTimeout -> setConnectTimeout(unwrapped, connectTimeout));
      map.from(settings::bufferRequestBody).to(bufferRequestBody -> setBufferRequestBody(unwrapped, bufferRequestBody));
    }

    private static ClientHttpRequestFactory unwrapIfNecessary(ClientHttpRequestFactory requestFactory) {
      if (requestFactory instanceof ClientHttpRequestFactoryWrapper) {
        while (requestFactory instanceof ClientHttpRequestFactoryWrapper wrapper) {
          requestFactory = wrapper.getRequestFactory();
        }
      }
      return requestFactory;
    }

    private static void setConnectTimeout(ClientHttpRequestFactory factory, Duration connectTimeout) {
      Method method = findMethod(factory, "setConnectTimeout", int.class);
      int timeout = Math.toIntExact(connectTimeout.toMillis());
      invoke(factory, method, timeout);
    }

    private static void setReadTimeout(ClientHttpRequestFactory factory, Duration readTimeout) {
      Method method = findMethod(factory, "setReadTimeout", int.class);
      int timeout = Math.toIntExact(readTimeout.toMillis());
      invoke(factory, method, timeout);
    }

    private static void setBufferRequestBody(ClientHttpRequestFactory factory, boolean bufferRequestBody) {
      Method method = findMethod(factory, "setBufferRequestBody", boolean.class);
      invoke(factory, method, bufferRequestBody);
    }

    private static Method findMethod(ClientHttpRequestFactory requestFactory,
            String methodName, Class<?>... parameters) {
      Method method = ReflectionUtils.findMethod(requestFactory.getClass(), methodName, parameters);
      Assert.state(method != null, () -> "Request factory %s does not have a suitable %s method"
              .formatted(requestFactory.getClass().getName(), methodName));
      Assert.state(!method.isAnnotationPresent(Deprecated.class),
              () -> "Request factory %s has the %s method marked as deprecated"
                      .formatted(requestFactory.getClass().getName(), methodName));
      return method;
    }

    private static void invoke(ClientHttpRequestFactory requestFactory, Method method, Object... parameters) {
      ReflectionUtils.invokeMethod(method, requestFactory, parameters);
    }

  }

}
