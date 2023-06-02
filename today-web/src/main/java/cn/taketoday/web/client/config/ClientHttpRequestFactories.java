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

package cn.taketoday.web.client.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.SocketConfig;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cn.taketoday.core.ssl.SslBundle;
import cn.taketoday.core.ssl.SslOptions;
import cn.taketoday.http.HttpMethod;
import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestFactoryWrapper;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.PropertyMapper;
import cn.taketoday.util.ReflectionUtils;
import okhttp3.OkHttpClient;

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

  /**
   * Return a new {@link ClientHttpRequestFactory} instance using the most appropriate
   * implementation.
   *
   * @param settings the settings to apply
   * @return a new {@link ClientHttpRequestFactory}
   */
  public static ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
    Assert.notNull(settings, "Settings is required");
    ClassLoader classLoader = ClientHttpRequestFactories.class.getClassLoader();
    if (ClassUtils.isPresent("org.apache.hc.client5.http.impl.classic.HttpClients", classLoader)) {
      return HttpComponents.get(settings);
    }
    if (ClassUtils.isPresent("okhttp3.OkHttpClient", classLoader)) {
      return OkHttp.get(settings);
    }
    return Simple.get(settings);
  }

  /**
   * Return a new {@link ClientHttpRequestFactory} of the given type, applying
   * {@link ClientHttpRequestFactorySettings} using reflection if necessary.
   *
   * @param <T> the {@link ClientHttpRequestFactory} type
   * @param requestFactoryType the {@link ClientHttpRequestFactory} type
   * @param settings the settings to apply
   * @return a new {@link ClientHttpRequestFactory} instance
   */
  @SuppressWarnings("unchecked")
  public static <T extends ClientHttpRequestFactory> T get(Class<T> requestFactoryType,
          ClientHttpRequestFactorySettings settings) {
    Assert.notNull(settings, "Settings must not be null");
    if (requestFactoryType == ClientHttpRequestFactory.class) {
      return (T) get(settings);
    }
    if (requestFactoryType == HttpComponentsClientHttpRequestFactory.class) {
      return (T) HttpComponents.get(settings);
    }
    if (requestFactoryType == OkHttp3ClientHttpRequestFactory.class) {
      return (T) OkHttp.get(settings);
    }
    if (requestFactoryType == SimpleClientHttpRequestFactory.class) {
      return (T) Simple.get(settings);
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
        var socketFactory = new SSLConnectionSocketFactory(
                sslBundle.createSslContext(),
                options.getEnabledProtocols(),
                options.getCiphers(),
                new DefaultHostnameVerifier()
        );
        connectionManagerBuilder.setSSLSocketFactory(socketFactory);
      }
      var connectionManager = connectionManagerBuilder.build();
      return HttpClientBuilder.create()
              .setConnectionManager(connectionManager)
              .build();
    }

  }

  /**
   * Support for {@link OkHttp3ClientHttpRequestFactory}.
   */
  static class OkHttp {

    static OkHttp3ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
      Assert.state(settings.bufferRequestBody() == null,
              () -> "OkHttp3ClientHttpRequestFactory does not support request body buffering");
      OkHttp3ClientHttpRequestFactory requestFactory = createRequestFactory(settings.sslBundle());

      if (settings.readTimeout() != null) {
        requestFactory.setReadTimeout((int) settings.readTimeout().toMillis());
      }
      if (settings.connectTimeout() != null) {
        requestFactory.setConnectTimeout((int) settings.connectTimeout().toMillis());
      }

      return requestFactory;
    }

    private static OkHttp3ClientHttpRequestFactory createRequestFactory(@Nullable SslBundle sslBundle) {
      if (sslBundle != null) {
        Assert.state(!sslBundle.getOptions().isSpecified(), "SSL Options cannot be specified with OkHttp");
        SSLSocketFactory socketFactory = sslBundle.createSslContext().getSocketFactory();
        TrustManager[] trustManagers = sslBundle.getManagers().getTrustManagers();
        Assert.state(trustManagers.length == 1,
                "Trust material must be provided in the SSL bundle for OkHttp3ClientHttpRequestFactory");
        OkHttpClient client = new OkHttpClient.Builder()
                .sslSocketFactory(socketFactory, (X509TrustManager) trustManagers[0])
                .build();
        return new OkHttp3ClientHttpRequestFactory(client);
      }
      return new OkHttp3ClientHttpRequestFactory();
    }

  }

  /**
   * Support for {@link SimpleClientHttpRequestFactory}.
   */
  static class Simple {

    static SimpleClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
      SslBundle sslBundle = settings.sslBundle();
      SimpleClientHttpRequestFactory requestFactory =
              sslBundle != null
              ? new SimpleClientHttpsRequestFactory(sslBundle)
              : new SimpleClientHttpRequestFactory();
      Assert.state(sslBundle == null || !sslBundle.getOptions().isSpecified(),
              "SSL Options cannot be specified with Java connections");
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
      map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
      return requestFactory;
    }

    /**
     * {@link SimpleClientHttpsRequestFactory} to configure SSL from an
     * {@link SslBundle}.
     */
    private static class SimpleClientHttpsRequestFactory extends SimpleClientHttpRequestFactory {

      @Nullable
      private final SslBundle sslBundle;

      SimpleClientHttpsRequestFactory(@Nullable SslBundle sslBundle) {
        this.sslBundle = sslBundle;
      }

      @Override
      protected void prepareConnection(HttpURLConnection connection, HttpMethod httpMethod) throws IOException {
        if (sslBundle != null && connection instanceof HttpsURLConnection secureConnection) {
          SSLSocketFactory socketFactory = this.sslBundle.createSslContext().getSocketFactory();
          secureConnection.setSSLSocketFactory(socketFactory);
        }
      }

    }

  }

  /**
   * Support for reflective configuration of an unknown {@link ClientHttpRequestFactory}
   * implementation.
   */
  static class Reflective {

    static <T extends ClientHttpRequestFactory> T get(Supplier<T> requestFactorySupplier,
            ClientHttpRequestFactorySettings settings) {
      T requestFactory = requestFactorySupplier.get();
      configure(requestFactory, settings);
      return requestFactory;
    }

    private static void configure(ClientHttpRequestFactory requestFactory,
            ClientHttpRequestFactorySettings settings) {
      ClientHttpRequestFactory unwrapped = unwrapIfNecessary(requestFactory);
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(settings::readTimeout).to(readTimeout -> setReadTimeout(unwrapped, readTimeout));
      map.from(settings::connectTimeout).to(connectTimeout -> setConnectTimeout(unwrapped, connectTimeout));
      map.from(settings::bufferRequestBody).to(bufferRequestBody -> setBufferRequestBody(unwrapped, bufferRequestBody));
    }

    private static ClientHttpRequestFactory unwrapIfNecessary(
            ClientHttpRequestFactory requestFactory) {
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
