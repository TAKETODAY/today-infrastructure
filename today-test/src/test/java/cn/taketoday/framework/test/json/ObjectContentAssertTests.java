/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.json;

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ObjectContentAssert}.
 *
 * @author Phillip Webb
 */
class ObjectContentAssertTests {

  private static final ExampleObject SOURCE = new ExampleObject();

  private static final ExampleObject DIFFERENT;

  static {
    DIFFERENT = new ExampleObject();
    DIFFERENT.setAge(123);
  }

  @Test
  void isEqualToWhenObjectsAreEqualShouldPass() {
    assertThat(forObject(SOURCE)).isEqualTo(SOURCE);
  }

  @Test
  void isEqualToWhenObjectsAreDifferentShouldFail() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> assertThat(forObject(SOURCE)).isEqualTo(DIFFERENT));
  }

  @Test
  void asArrayForArrayShouldReturnObjectArrayAssert() {
    ExampleObject[] source = new ExampleObject[] { SOURCE };
    assertThat(forObject(source)).asArray().containsExactly(SOURCE);
  }

  @Test
  void asArrayForNonArrayShouldFail() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forObject(SOURCE)).asArray());
  }

  @Test
  void asMapForMapShouldReturnMapAssert() {
    Map<String, ExampleObject> source = Collections.singletonMap("a", SOURCE);
    assertThat(forObject(source)).asMap().containsEntry("a", SOURCE);
  }

  @Test
  void asMapForNonMapShouldFail() {
    assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertThat(forObject(SOURCE)).asMap());
  }

  private AssertProvider<ObjectContentAssert<Object>> forObject(Object source) {
    return () -> new ObjectContentAssert<>(source);
  }

}
