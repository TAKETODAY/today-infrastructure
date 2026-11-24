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

package infra.test.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ReflectionTestUtilsAdditionalTests {

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "DEBUG", "OFF" })
  void fieldAndMethodAccessRemainConsistentWithDiagnosticsEnabledOrDisabled(LogLevel level) {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = ReflectionTestUtils.class.getName();
    var configuration = logging.getLoggerConfiguration(name);
    LogLevel previous = (configuration != null ? configuration.getConfiguredLevel() : null);
    logging.setLogLevel(name, level);
    try {
      Fluent target = new Fluent();
      ReflectionTestUtils.setField(target, "text", "field");
      assertThat(ReflectionTestUtils.<String>getField(target, "text")).isEqualTo("field");
      ReflectionTestUtils.invokeSetterMethod(target, "value", "setter", String.class);
      assertThat(ReflectionTestUtils.invokeGetterMethod(target, "value")).isEqualTo("setter");
      assertThat(ReflectionTestUtils.<String>invokeMethod(target, "value")).isEqualTo("setter");
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  @Test
  void fluentAccessorsAndMissingMembersHaveUsefulDiagnosticsEvenWithBrokenToString() {
    Fluent target = new Fluent();
    ReflectionTestUtils.invokeSetterMethod(target, "value", "updated", String.class);
    assertThat(ReflectionTestUtils.invokeGetterMethod(target, "value")).isEqualTo("updated");
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.setField(target, "missing", "value"))
            .withMessageContaining("whose toString() method threw");
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.getField(target, "missing"))
            .withMessageContaining("whose toString() method threw");
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.invokeSetterMethod(target, "missing", "value"))
            .withMessageContaining("Could not find setter method");
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.invokeSetterMethod(target, "setMissing", "value"));
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.invokeGetterMethod(target, "missing"))
            .withMessageContaining("Could not find getter method");
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.invokeGetterMethod(target, "getMissing"));
  }

  @Test
  void explicitClassAndStaticMethodsAreSupported() {
    Fluent target = new Fluent();
    ReflectionTestUtils.setField(target, Fluent.class, "text", "changed", String.class);
    assertThat(ReflectionTestUtils.<String>getField(target, Fluent.class, "text")).isEqualTo("changed");
    assertThat(ReflectionTestUtils.<String>invokeMethod(Fluent.class, "staticValue")).isEqualTo("static");
    assertThat(ReflectionTestUtils.<String>invokeMethod(target, Fluent.class, "value")).isEqualTo("changed");
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.invokeMethod((Object) null, (Class<?>) null, "value"));
    assertThatIllegalArgumentException().isThrownBy(() -> ReflectionTestUtils.invokeMethod(target, " "));
    org.assertj.core.api.Assertions.assertThatIllegalStateException()
            .isThrownBy(() -> ReflectionTestUtils.invokeMethod(target, "missing")).withMessageContaining("Method not found");
  }

  static class Fluent {
    private String text;

    private void value(String value) {
      text = value;
    }

    private String value() {
      return text;
    }

    private static String staticValue() {
      return "static";
    }

    @Override
    public String toString() {
      throw new IllegalStateException("broken toString");
    }
  }

}
