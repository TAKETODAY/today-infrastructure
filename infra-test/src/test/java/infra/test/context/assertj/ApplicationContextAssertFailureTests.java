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

package infra.test.context.assertj;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import infra.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ApplicationContextAssertFailureTests {

  @Test
  void everyBeanAssertionExplainsStartupFailure() {
    try (var context = new GenericApplicationContext()) {
      var assertions = new ApplicationContextAssert<>(context, new IllegalStateException("startup marker"));
      List<Consumer<ApplicationContextAssert<GenericApplicationContext>>> checks = List.of(
              a -> a.hasBean("sample"),
              a -> a.hasSingleBean(String.class),
              a -> a.doesNotHaveBean(String.class),
              a -> a.doesNotHaveBean("sample"),
              a -> a.getBeanNames(String.class),
              a -> a.getBean(String.class),
              a -> a.getBean("sample"),
              a -> a.getBean("sample", String.class),
              a -> a.getBeans(String.class),
              ApplicationContextAssert::hasNotFailed);
      for (var check : checks) {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> check.accept(assertions))
                .withMessageContaining("context failed to start", "startup marker");
      }
    }
  }

  @Test
  void missingWrongAndAmbiguousBeansReportTheirActualState() {
    try (var context = new GenericApplicationContext()) {
      context.registerBean("first", String.class, () -> "first", definition -> definition.setPrimary(true));
      context.registerBean("second", String.class, () -> "second", definition -> definition.setPrimary(true));
      context.refresh();
      var assertions = new ApplicationContextAssert<>(context, null);
      assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.hasBean("missing"))
              .withMessageContaining("found no such bean");
      assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.hasSingleBean(Integer.class))
              .withMessageContaining("found no beans");
      assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.hasSingleBean(String.class))
              .withMessageContaining("first", "second");
      assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.doesNotHaveBean(String.class));
      assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.doesNotHaveBean("first"));
      assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.getBean(String.class))
              .withMessageContaining("first", "second");
      assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.getBean("first", Integer.class))
              .withMessageContaining("first", "java.lang.Integer");
      assertThatExceptionOfType(AssertionError.class).isThrownBy(assertions::hasFailed)
              .withMessageContaining("context started successfully");
      assertions.doesNotHaveBean("missing");
      assertions.getBean("first", null).isEqualTo("first");
    }
  }

}
