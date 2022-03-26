/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.expression.spel;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.spel.support.StandardTypeLocator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for type comparison.
 *
 * @author Andy Clement
 */
class StandardTypeLocatorTests {

  @Test
  void testImports() throws EvaluationException {
    StandardTypeLocator locator = new StandardTypeLocator();
    assertThat(locator.findType("java.lang.Integer")).isEqualTo(Integer.class);
    assertThat(locator.findType("java.lang.String")).isEqualTo(String.class);

    List<String> prefixes = locator.getImportPrefixes();
    assertThat(prefixes.size()).isEqualTo(1);
    assertThat(prefixes.contains("java.lang")).isTrue();
    assertThat(prefixes.contains("java.util")).isFalse();

    assertThat(locator.findType("Boolean")).isEqualTo(Boolean.class);
    // currently does not know about java.util by default
    // assertEquals(java.util.List.class,locator.findType("List"));

    assertThatExceptionOfType(SpelEvaluationException.class).isThrownBy(() ->
                    locator.findType("URL"))
            .satisfies(ex -> assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.TYPE_NOT_FOUND));
    locator.registerImport("java.net");
    assertThat(locator.findType("URL")).isEqualTo(java.net.URL.class);
  }

  @Test
  void importClass() {
    StandardTypeLocator locator = new StandardTypeLocator();
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

  static class A { }

  static class B { }
}
