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

package infra.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import infra.context.support.GenericApplicationContext;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.MapPropertySource;
import infra.core.env.SystemEnvironmentPropertySource;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TestPropertyValues}.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 */
@ResourceLock("SYSTEM_PROPERTIES")
class TestPropertyValuesTests {

  @Test
  void nullableInputsReturnEmptyOrUnchangedValues() {
    TestPropertyValues empty = TestPropertyValues.empty();
    assertThat(TestPropertyValues.of((Iterable<String>) null)).isSameAs(empty);
    assertThat(TestPropertyValues.of((Stream<String>) null)).isSameAs(empty);
    assertThat(TestPropertyValues.of((Map<String, String>) null)).isSameAs(empty);
    assertThat(TestPropertyValues.of(null, TestPropertyValues.Pair::parse)).isSameAs(empty);
    TestPropertyValues values = TestPropertyValues.of("a=b");
    assertThat(values.and((Iterable<String>) null)).isSameAs(values);
    assertThat(values.and((Stream<String>) null)).isSameAs(values);
    assertThat(values.and((Map<String, String>) null)).isSameAs(values);
    assertThat(values.and(null, TestPropertyValues.Pair::parse)).isSameAs(values);
  }

  @Test
  void factoriesAndChainingCreateIndependentSnapshots() {
    TestPropertyValues original = TestPropertyValues.of(List.of("a=original"));
    TestPropertyValues extended = original.and(List.of("b:two"))
            .and(Stream.of("c=three")).and(Map.of("d", "four"))
            .and(Stream.of("skip", "e"), name -> name.equals("skip") ? null : TestPropertyValues.Pair.of(name, "five"));
    MockEnvironment environment = new MockEnvironment();
    extended.applyTo(environment);
    assertThat(environment.getProperty("a")).isEqualTo("original");
    assertThat(environment.getProperty("b")).isEqualTo("two");
    assertThat(environment.getProperty("c")).isEqualTo("three");
    assertThat(environment.getProperty("d")).isEqualTo("four");
    assertThat(environment.getProperty("e")).isEqualTo("five");
    assertThat(environment.getProperty("skip")).isNull();
    MockEnvironment separate = new MockEnvironment();
    original.applyTo(separate);
    assertThat(separate.getProperty("b")).isNull();
    TestPropertyValues.of(Map.of("a", "override")).applyTo(environment);
    assertThat(environment.getProperty("a")).isEqualTo("override");
    assertThat(environment.getProperty("e")).isEqualTo("five");
  }

  @Test
  void appliesToContextAndReplacesSourcesOfDifferentTypes() {
    try (GenericApplicationContext context = new GenericApplicationContext()) {
      TestPropertyValues.of("a=one").applyTo(context);
      assertThat(context.getEnvironment().getProperty("a")).isEqualTo("one");
    }
    MockEnvironment environment = new MockEnvironment();
    TestPropertyValues.of("A_B=one").applyTo(environment, TestPropertyValues.Type.SYSTEM_ENVIRONMENT);
    assertThat(environment.getProperty("a.b")).isEqualTo("one");
    assertThat(environment.getPropertySources().get("test-systemEnvironment"))
            .isExactlyInstanceOf(SystemEnvironmentPropertySource.class);
    TestPropertyValues.of("a=one").applyTo(environment, TestPropertyValues.Type.MAP, "custom");
    assertThat(environment.getPropertySources().get("custom")).isExactlyInstanceOf(MapPropertySource.class);
    TestPropertyValues.of("A_B=two").applyTo(environment, TestPropertyValues.Type.SYSTEM_ENVIRONMENT, "custom");
    assertThat(environment.getPropertySources().get("custom")).isExactlyInstanceOf(SystemEnvironmentPropertySource.class);
    assertThat(environment.getProperty("a.b")).isEqualTo("two");
  }

  @Test
  void rejectsMissingApplyArguments() {
    TestPropertyValues values = TestPropertyValues.empty();
    MockEnvironment environment = new MockEnvironment();
    assertThatIllegalArgumentException().isThrownBy(() -> values.applyTo((ConfigurableEnvironment) null,
            TestPropertyValues.Type.MAP, "test")).withMessage("Environment is required");
    assertThatIllegalArgumentException().isThrownBy(() -> values.applyTo(environment, null, "test"))
            .withMessage("Property source type is required");
    assertThatIllegalArgumentException().isThrownBy(() -> values.applyTo(environment, TestPropertyValues.Type.MAP, null))
            .withMessage("Property source name is required");
  }

  @Test
  void parsesSeparatorsWhitespaceAndEmptyValues() {
    Map<String, Object> properties = new LinkedHashMap<>();
    for (String pair : List.of(" name = value ", "colon : a=b", "equals=a:b", "bare", "empty=")) {
      TestPropertyValues.Pair.parse(pair).addTo(properties);
    }
    assertThat(properties).containsEntry("name", "value").containsEntry("colon", "a=b")
            .containsEntry("equals", "a:b").containsEntry("bare", "").containsEntry("empty", "");
    assertThatIllegalArgumentException().isThrownBy(() -> TestPropertyValues.Pair.of("", "value"));
  }

  @Test
  void restoresSystemPropertiesAfterRunnableAndCheckedFailure() {
    String key = "infra.test.coverage.property";
    String saved = System.getProperty(key);
    try {
      System.setProperty(key, "previous");
      TestPropertyValues.of(key + "=").applyToSystemProperties((Runnable) () ->
              assertThat(System.getProperty(key)).isNull());
      assertThat(System.getProperty(key)).isEqualTo("previous");
      System.clearProperty(key);
      IOException failure = new IOException("action failed");
      assertThatExceptionOfType(IOException.class).isThrownBy(() -> TestPropertyValues.of(key + "=temporary")
              .applyToSystemProperties((Callable<String>) () -> {
                assertThat(System.getProperty(key)).isEqualTo("temporary");
                throw failure;
              })).isSameAs(failure);
      assertThat(System.getProperty(key)).isNull();
    }
    finally {
      if (saved != null) {
        System.setProperty(key, saved);
      }
      else {
        System.clearProperty(key);
      }
    }
  }

  @Test
  void applyToSystemPropertiesShouldRestoreEmptySystemProperty() {
    System.setProperty("foo", "");
    try {
      TestPropertyValues.of("foo=bar").applyToSystemProperties(() -> {
        assertThat(System.getProperty("foo")).isEqualTo("bar");
        return null;
      });
      assertThat(System.getProperty("foo")).isEmpty();
    }
    finally {
      System.clearProperty("foo");
    }
  }

}
