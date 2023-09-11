/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URL;

import cn.taketoday.expression.spel.support.StandardTypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for type comparison.
 *
 * @author Andy Clement
 */
class StandardTypeLocatorTests {
  final StandardTypeLocator locator = new StandardTypeLocator();

  @ParameterizedTest(name = "[{index}] {0} --> {1}")
  @CsvSource(delimiterString = "-->", textBlock = """
          Boolean           --> java.lang.Boolean
          Character         --> java.lang.Character
          Number            --> java.lang.Number
          Integer           --> java.lang.Integer
          String            --> java.lang.String

          java.lang.Boolean --> java.lang.Boolean
          java.lang.Integer --> java.lang.Integer
          java.lang.String  --> java.lang.String
          """)
  void defaultImports(String typeName, Class<?> type) {
    assertThat(locator.findType(typeName)).isEqualTo(type);
  }

  @Test
  void importClass() {
    assertThat(locator.findType("Integer")).isEqualTo(Integer.class);
    assertThat(locator.findType("String")).isEqualTo(String.class);

    locator.importClass(A.class);

    assertThat(locator.findType("A")).isEqualTo(A.class);

  }

  @Test
  void registerAlias() {
    StandardTypeLocator locator = new StandardTypeLocator();
    assertThat(locator.findType("Integer")).isEqualTo(Integer.class);
    assertThat(locator.findType("String")).isEqualTo(String.class);

    locator.importClass(A.class);
    locator.registerAlias("A", "a");
    assertThat(locator.findType("a")).isEqualTo(A.class);

    locator.registerAlias(B.class, "b");

    assertThat(locator.findType("b")).isEqualTo(B.class);
  }

  @Test
  void importPrefixes() {
    assertThat(locator.getImportPrefixes()).containsExactly("java.lang");
  }

  @Test
  void typeNotFound() {
    assertThatExceptionOfType(SpelEvaluationException.class)
            .isThrownBy(() -> locator.findType("URL"))
            .extracting(SpelEvaluationException::getMessageCode)
            .isEqualTo(SpelMessage.TYPE_NOT_FOUND);
  }

  @Test
  void registerImport() {
    locator.registerImport("java.net");
    assertThat(locator.findType("URL")).isEqualTo(URL.class);
  }

  static class A { }

  static class B { }
}
