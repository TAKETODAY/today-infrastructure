/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.runner;

import org.junit.jupiter.api.Test;

import infra.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebApplicationContextRunnerTests {

  @Test
  void defaultRunnerRegistersWebScopesAndCopiesConfiguration() {
    WebApplicationContextRunner original = new WebApplicationContextRunner();
    original.withPropertyValues("test.value=updated").withBean("message", String.class, () -> "hello")
            .run(context -> {
              assertThat(context).hasSingleBean(String.class);
              assertThat(context.getBean(String.class)).isEqualTo("hello");
              assertThat(context.getEnvironment().getProperty("test.value")).isEqualTo("updated");
              assertThat(context.getBeanFactory().getRegisteredScope("request")).isNotNull();
              assertThat(context.getBeanFactory().getRegisteredScope("session")).isNotNull();
            });
    original.run(context -> {
      assertThat(context).doesNotHaveBean(String.class);
      assertThat(context.getEnvironment().getProperty("test.value")).isNull();
    });
  }

  @Test
  void customContextFactoryAndPrepareDoNotRefreshContext() {
    GenericApplicationContext supplied = new GenericApplicationContext();
    new WebApplicationContextRunner(() -> supplied).withPropertyValues("test.value=prepared")
            .prepare(context -> {
              assertThat(context.getSourceApplicationContext()).isSameAs(supplied);
              assertThat(supplied.isActive()).isFalse();
              assertThat(context.getEnvironment().getProperty("test.value")).isEqualTo("prepared");
            });
  }

  @Test
  void consumerFailureClosesContextAndRestoresClassLoader() {
    GenericApplicationContext supplied = new GenericApplicationContext();
    ClassLoader original = Thread.currentThread().getContextClassLoader();
    ClassLoader custom = new ClassLoader(original) { };
    IllegalStateException failure = new IllegalStateException("consumer failed");
    assertThatThrownBy(() -> new WebApplicationContextRunner(() -> supplied).withClassLoader(custom)
            .run(context -> {
              assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(custom);
              throw failure;
            })).isSameAs(failure);
    assertThat(supplied.isActive()).isFalse();
    assertThat(Thread.currentThread().getContextClassLoader()).isSameAs(original);
  }

}
