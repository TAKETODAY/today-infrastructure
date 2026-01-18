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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;

import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.ReflectionHintsPredicates;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/2 13:44
 */
class ClientHttpRequestFactoryRuntimeHintsTests {

  @Test
  void shouldRegisterHints() {
    RuntimeHints hints = new RuntimeHints();
    new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    Field requestFactoryField = ReflectionUtils.findField(ClientHttpRequestFactoryWrapper.class,
            "requestFactory");
    assertThat(requestFactoryField).isNotNull();
    assertThat(reflection.onFieldAccess(requestFactoryField)).accepts(hints);
  }

  @Test
  void shouldRegisterHttpComponentHints() {
    RuntimeHints hints = new RuntimeHints();
    new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    assertThat(reflection
            .onMethodInvocation(method(HttpComponentsClientHttpRequestFactory.class, "setReadTimeout", int.class)))
            .accepts(hints);
    assertThat(reflection
            .onMethodInvocation(method(HttpComponentsClientHttpRequestFactory.class, "setReadTimeout", Duration.class)))
            .accepts(hints);
  }

  @Test
  void shouldRegisterReactorHints() {
    RuntimeHints hints = new RuntimeHints();
    new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    assertThat(reflection
            .onMethodInvocation(method(ReactorClientHttpRequestFactory.class, "setConnectTimeout", int.class)))
            .accepts(hints);
    assertThat(reflection
            .onMethodInvocation(method(ReactorClientHttpRequestFactory.class, "setConnectTimeout", Duration.class)))
            .accepts(hints);
    assertThat(reflection
            .onMethodInvocation(method(ReactorClientHttpRequestFactory.class, "setReadTimeout", long.class)))
            .accepts(hints);
    assertThat(reflection
            .onMethodInvocation(method(ReactorClientHttpRequestFactory.class, "setReadTimeout", Duration.class)))
            .accepts(hints);
  }

  @Test
  void shouldRegisterJdkHttpHints() {
    RuntimeHints hints = new RuntimeHints();
    new ClientHttpRequestFactoryRuntimeHints().registerHints(hints, getClass().getClassLoader());
    ReflectionHintsPredicates reflection = RuntimeHintsPredicates.reflection();
    assertThat(
            reflection.onMethodInvocation(method(JdkClientHttpRequestFactory.class, "setReadTimeout", int.class)))
            .accepts(hints);
    assertThat(reflection
            .onMethodInvocation(method(JdkClientHttpRequestFactory.class, "setReadTimeout", Duration.class)))
            .accepts(hints);
  }

  private static Method method(Class<?> target, String name, Class<?>... parameterTypes) {
    Method method = ReflectionUtils.findMethod(target, name, parameterTypes);
    assertThat(method).isNotNull();
    return method;
  }

}