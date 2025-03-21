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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.ReflectionHintsPredicates;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.http.client.ClientHttpRequestFactoryWrapper;
import infra.http.client.HttpComponentsClientHttpRequestFactory;
import infra.http.client.JdkClientHttpRequestFactory;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/7/3 22:48
 */
class ClientHttpRequestFactoriesRuntimeHintsTests {

  @Test
  void shouldRegisterHints() {
    RuntimeHints hints = new RuntimeHints();
    new ClientHttpRequestFactoriesRuntimeHints().registerHints(hints, getClass().getClassLoader());
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    Field requestFactoryField = ReflectionUtils.findField(ClientHttpRequestFactoryWrapper.class,
            "requestFactory");
    assertThat(requestFactoryField).isNotNull();
    assertThat(reflection.onFieldAccess(requestFactoryField)).accepts(hints);
  }

  @Test
  void shouldRegisterHttpComponentHints() {
    RuntimeHints hints = new RuntimeHints();
    new ClientHttpRequestFactoriesRuntimeHints().registerHints(hints, getClass().getClassLoader());
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    assertThat(reflection
            .onMethodInvocation(method(HttpComponentsClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
            .accepts(hints);
  }

  @Test
  void shouldRegisterSimpleHttpHints() {
    RuntimeHints hints = new RuntimeHints();
    new ClientHttpRequestFactoriesRuntimeHints().registerHints(hints, getClass().getClassLoader());
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    assertThat(reflection.onMethodInvocation(method(JdkClientHttpRequestFactory.class, "setReadTimeout", Duration.class)))
            .accepts(hints);
  }

  private static Method method(Class<?> target, String name, Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(target, name, parameterTypes);
    assertThat(method).isNotNull();
    return method;
  }

}