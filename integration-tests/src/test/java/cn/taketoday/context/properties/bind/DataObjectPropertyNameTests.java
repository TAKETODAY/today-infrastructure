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

package cn.taketoday.context.properties.bind;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataObjectPropertyName}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
class DataObjectPropertyNameTests {

  @Test
  void toDashedCaseConvertsValue() {
    assertThat(DataObjectPropertyName.toDashedForm("Foo")).isEqualTo("foo");
    assertThat(DataObjectPropertyName.toDashedForm("foo")).isEqualTo("foo");
    assertThat(DataObjectPropertyName.toDashedForm("fooBar")).isEqualTo("foo-bar");
    assertThat(DataObjectPropertyName.toDashedForm("foo_bar")).isEqualTo("foo-bar");
    assertThat(DataObjectPropertyName.toDashedForm("_foo_bar")).isEqualTo("-foo-bar");
    assertThat(DataObjectPropertyName.toDashedForm("foo_Bar")).isEqualTo("foo-bar");
  }

  @Test
  void toDashedFormWhenContainsIndexedAddsNoDashToIndex() {
    assertThat(DataObjectPropertyName.toDashedForm("test[fooBar]")).isEqualTo("test[fooBar]");
    assertThat(DataObjectPropertyName.toDashedForm("testAgain[fooBar]")).isEqualTo("test-again[fooBar]");
  }

}
