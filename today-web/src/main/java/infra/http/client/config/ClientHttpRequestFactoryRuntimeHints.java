/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import infra.aot.hint.ExecutableMode;
import infra.aot.hint.ReflectionHints;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.RuntimeHintsRegistrar;
import infra.aot.hint.TypeReference;
import infra.http.client.ClientHttpRequestFactory;
import infra.http.client.ClientHttpRequestFactoryWrapper;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.http.client.ReactorClientHttpRequestFactory;
import infra.lang.Assert;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

/**
 * {@link RuntimeHintsRegistrar} for {@link ClientHttpRequestFactory} implementations.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ClientHttpRequestFactoryRuntimeHints implements RuntimeHintsRegistrar {

  @Override
  public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
    if (ClassUtils.isPresent("infra.http.client.ClientHttpRequestFactory", classLoader)) {
      registerHints(hints.reflection(), classLoader);
    }
  }

  private void registerHints(ReflectionHints hints, @Nullable ClassLoader classLoader) {
    hints.registerField(findField(ClientHttpRequestFactoryWrapper.class, "requestFactory"));

    registerClientHttpRequestFactoryHints(hints, classLoader,
            HttpComponentsClientHttpRequestFactoryBuilder.Classes.HTTP_CLIENTS,
            () -> registerReflectionHints(hints, HttpComponentsClientHttpRequestFactory.class));

    registerClientHttpRequestFactoryHints(hints, classLoader,
            ReactorClientHttpRequestFactoryBuilder.Classes.HTTP_CLIENT,
            () -> registerReflectionHints(hints, ReactorClientHttpRequestFactory.class, long.class));

    registerClientHttpRequestFactoryHints(hints, classLoader,
            JdkClientHttpRequestFactoryBuilder.Classes.HTTP_CLIENT,
            () -> registerReflectionHints(hints, JdkClientHttpRequestFactory.class));
  }

  private void registerClientHttpRequestFactoryHints(ReflectionHints hints, @Nullable ClassLoader classLoader,
          String className, Runnable action) {
    hints.registerTypeIfPresent(classLoader, className, (typeHint) -> {
      typeHint.onReachableType(TypeReference.of(className));
      action.run();
    });
  }

  private void registerReflectionHints(ReflectionHints hints,
          Class<? extends ClientHttpRequestFactory> requestFactoryType) {
    registerReflectionHints(hints, requestFactoryType, int.class);
  }

  private void registerReflectionHints(ReflectionHints hints,
          Class<? extends ClientHttpRequestFactory> requestFactoryType, Class<?> readTimeoutType) {
    registerMethod(hints, requestFactoryType, "setConnectTimeout", int.class);
    registerMethod(hints, requestFactoryType, "setConnectTimeout", Duration.class);
    registerMethod(hints, requestFactoryType, "setReadTimeout", readTimeoutType);
    registerMethod(hints, requestFactoryType, "setReadTimeout", Duration.class);
  }

  private void registerMethod(ReflectionHints hints, Class<? extends ClientHttpRequestFactory> requestFactoryType,
          String methodName, Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(requestFactoryType, methodName, parameterTypes);
    if (method != null) {
      hints.registerMethod(method, ExecutableMode.INVOKE);
    }
  }

  private Field findField(Class<?> type, String name) {
    Field field = ReflectionUtils.findField(type, name);
    Assert.state(field != null, () -> "Unable to find field '%s' on %s".formatted(type.getName(), name));
    return field;
  }

}
