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

package infra.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
class SystemPropertyUtilsTests {

  @Test
  void replaceFromSystemProperty() {
    System.setProperty("test.prop", "bar");
    try {
      String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}");
      assertThat(resolved).isEqualTo("bar");
    }
    finally {
      System.getProperties().remove("test.prop");
    }
  }

  @Test
  void replaceFromSystemPropertyWithDefault() {
    System.setProperty("test.prop", "bar");
    try {
      String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:foo}");
      assertThat(resolved).isEqualTo("bar");
    }
    finally {
      System.getProperties().remove("test.prop");
    }
  }

  @Test
  void replaceFromSystemPropertyWithExpressionDefault() {
    System.setProperty("test.prop", "bar");
    try {
      String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:#{foo.bar}}");
      assertThat(resolved).isEqualTo("bar");
    }
    finally {
      System.getProperties().remove("test.prop");
    }
  }

  @Test
  void replaceFromSystemPropertyWithExpressionContainingDefault() {
    System.setProperty("test.prop", "bar");
    try {
      String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:Y#{foo.bar}X}");
      assertThat(resolved).isEqualTo("bar");
    }
    finally {
      System.getProperties().remove("test.prop");
    }
  }

  @Test
  void replaceWithDefault() {
    String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:foo}");
    assertThat(resolved).isEqualTo("foo");
  }

  @Test
  void replaceWithExpressionDefault() {
    String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:#{foo.bar}}");
    assertThat(resolved).isEqualTo("#{foo.bar}");
  }

  @Test
  void replaceWithExpressionContainingDefault() {
    String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:Y#{foo.bar}X}");
    assertThat(resolved).isEqualTo("Y#{foo.bar}X");
  }

  @Test
  void replaceWithNoDefault() {
    assertThatExceptionOfType(PlaceholderResolutionException.class)
            .isThrownBy(() -> SystemPropertyUtils.resolvePlaceholders("${test.prop}"));
  }

  @Test
  void replaceWithNoDefaultIgnored() {
    String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}", true);
    assertThat(resolved).isEqualTo("${test.prop}");
  }

  @Test
  void replaceWithEmptyDefault() {
    String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop:}");
    assertThat(resolved).isEqualTo("");
  }

  @Test
  void recursiveFromSystemProperty() {
    System.setProperty("test.prop", "foo=${bar}");
    System.setProperty("bar", "baz");
    try {
      String resolved = SystemPropertyUtils.resolvePlaceholders("${test.prop}");
      assertThat(resolved).isEqualTo("foo=baz");
    }
    finally {
      System.getProperties().remove("test.prop");
      System.getProperties().remove("bar");
    }
  }

  @Test
  void replaceFromEnv() {
    Map<String, String> env = System.getenv();
    if (env.containsKey("PATH")) {
      String text = "${PATH}";
      assertThat(SystemPropertyUtils.resolvePlaceholders(text)).isEqualTo(env.get("PATH"));
    }
  }

  @Test
  void resolvePlaceholdersWithEmptyString() {
    String result = SystemPropertyUtils.resolvePlaceholders("");
    assertThat(result).isEqualTo("");
  }

  @Test
  void resolvePlaceholdersWithNoPlaceholders() {
    String result = SystemPropertyUtils.resolvePlaceholders("no placeholders here");
    assertThat(result).isEqualTo("no placeholders here");
  }

  @Test
  void resolvePlaceholdersWithMultiplePlaceholders() {
    System.setProperty("prop1", "value1");
    System.setProperty("prop2", "value2");
    try {
      String result = SystemPropertyUtils.resolvePlaceholders("First: ${prop1}, Second: ${prop2}");
      assertThat(result).isEqualTo("First: value1, Second: value2");
    }
    finally {
      System.getProperties().remove("prop1");
      System.getProperties().remove("prop2");
    }
  }

  @Test
  void resolvePlaceholdersWithMixedPlaceholdersAndDefaults() {
    System.setProperty("prop1", "value1");
    try {
      String result = SystemPropertyUtils.resolvePlaceholders("First: ${prop1}, Second: ${prop2:default2}");
      assertThat(result).isEqualTo("First: value1, Second: default2");
    }
    finally {
      System.getProperties().remove("prop1");
    }
  }

  @Test
  void resolvePlaceholdersWithNestedPlaceholdersInDefault() {
    System.setProperty("prop1", "value1");
    try {
      String result = SystemPropertyUtils.resolvePlaceholders("${prop1:${prop2:default}}");
      assertThat(result).isEqualTo("value1");
    }
    finally {
      System.getProperties().remove("prop1");
    }
  }

  @Test
  void resolvePlaceholdersWithUnresolvablePlaceholderAndIgnoreFlag() {
    String result = SystemPropertyUtils.resolvePlaceholders("${unknown.prop}", true);
    assertThat(result).isEqualTo("${unknown.prop}");
  }

  @Test
  void resolvePlaceholdersWithMultipleUnresolvablePlaceholdersAndIgnoreFlag() {
    String result = SystemPropertyUtils.resolvePlaceholders("${unknown.prop1} and ${unknown.prop2}", true);
    assertThat(result).isEqualTo("${unknown.prop1} and ${unknown.prop2}");
  }

  @Test
  void resolvePlaceholdersWithSpecialCharactersInPropertyName() {
    System.setProperty("prop.with.dots", "value");
    System.setProperty("prop-with-dashes", "value2");
    try {
      String result = SystemPropertyUtils.resolvePlaceholders("${prop.with.dots} and ${prop-with-dashes}");
      assertThat(result).isEqualTo("value and value2");
    }
    finally {
      System.getProperties().remove("prop.with.dots");
      System.getProperties().remove("prop-with-dashes");
    }
  }

  @Test
  void resolvePlaceholdersWithRecursiveResolutionAndDefaults() {
    System.setProperty("prop1", "${prop2:default}");
    System.setProperty("prop2", "value2");
    try {
      String result = SystemPropertyUtils.resolvePlaceholders("${prop1}");
      assertThat(result).isEqualTo("value2");
    }
    finally {
      System.getProperties().remove("prop1");
      System.getProperties().remove("prop2");
    }
  }

  @Test
  void resolvePlaceholdersWithCircularReference() {
    System.setProperty("prop1", "${prop2}");
    System.setProperty("prop2", "${prop1}");
    try {
      assertThatExceptionOfType(PlaceholderResolutionException.class)
              .isThrownBy(() -> SystemPropertyUtils.resolvePlaceholders("${prop1}"));
    }
    finally {
      System.getProperties().remove("prop1");
      System.getProperties().remove("prop2");
    }
  }

  @Test
  void resolvePlaceholdersWithEnvVariableFallback() {
    Map<String, String> env = System.getenv();
    if (env.containsKey("USER") || env.containsKey("USERNAME")) {
      String envVar = env.containsKey("USER") ? "USER" : "USERNAME";
      String result = SystemPropertyUtils.resolvePlaceholders("${nonexistent.prop:${" + envVar + "}}");
      assertThat(result).isEqualTo(env.get(envVar));
    }
  }

  @Test
  void resolvePlaceholdersWithEscapedDollarSign() {
    String result = SystemPropertyUtils.resolvePlaceholders("\\${not.a.placeholder}");
    assertThat(result).isEqualTo("${not.a.placeholder}");
  }

  @Test
  void resolvePlaceholdersWithIgnoreUnresolvableAndComplexText() {
    String result = SystemPropertyUtils.resolvePlaceholders("Before ${prop1} middle ${prop2:default} after", true);
    assertThat(result).isEqualTo("Before ${prop1} middle default after");
  }

}
