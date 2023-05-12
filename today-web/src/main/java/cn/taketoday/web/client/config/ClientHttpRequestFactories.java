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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Supplier;

import cn.taketoday.http.client.ClientHttpRequestFactory;
import cn.taketoday.http.client.ClientHttpRequestFactoryWrapper;
import cn.taketoday.http.client.HttpComponentsClientHttpRequestFactory;
import cn.taketoday.http.client.OkHttp3ClientHttpRequestFactory;
import cn.taketoday.http.client.SimpleClientHttpRequestFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.PropertyMapper;
import cn.taketoday.util.ReflectionUtils;

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
    if (ClassUtils.isPresent("org.apache.http.client.HttpClient", classLoader)) {
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
      var requestFactory = new HttpComponentsClientHttpRequestFactory();

      if (settings.readTimeout() != null) {
        requestFactory.setReadTimeout((int) settings.readTimeout().toMillis());
      }
      if (settings.connectTimeout() != null) {
        requestFactory.setConnectTimeout((int) settings.connectTimeout().toMillis());
      }

      if (settings.bufferRequestBody() != null) {
        requestFactory.setBufferRequestBody(settings.bufferRequestBody());
      }
      return requestFactory;
    }

  }

  /**
   * Support for {@link OkHttp3ClientHttpRequestFactory}.
   */
  static class OkHttp {

    static OkHttp3ClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
      Assert.state(settings.bufferRequestBody() == null,
              () -> "OkHttp3ClientHttpRequestFactory does not support request body buffering");
      OkHttp3ClientHttpRequestFactory requestFactory = new OkHttp3ClientHttpRequestFactory();
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
      map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
      return requestFactory;
    }

  }

  /**
   * Support for {@link SimpleClientHttpRequestFactory}.
   */
  static class Simple {

    static SimpleClientHttpRequestFactory get(ClientHttpRequestFactorySettings settings) {
      SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
      PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
      map.from(settings::readTimeout).asInt(Duration::toMillis).to(requestFactory::setReadTimeout);
      map.from(settings::connectTimeout).asInt(Duration::toMillis).to(requestFactory::setConnectTimeout);
      map.from(settings::bufferRequestBody).to(requestFactory::setBufferRequestBody);
      return requestFactory;
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
      map.from(settings::connectTimeout).to((connectTimeout) -> setConnectTimeout(unwrapped, connectTimeout));
      map.from(settings::readTimeout).to((readTimeout) -> setReadTimeout(unwrapped, readTimeout));
      map.from(settings::bufferRequestBody)
              .to((bufferRequestBody) -> setBufferRequestBody(unwrapped, bufferRequestBody));
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
