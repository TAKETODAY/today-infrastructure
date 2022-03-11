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

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.spel.support.StandardTypeComparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for type comparison
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 */
public class StandardTypeComparatorTests {

  @Test
  void testPrimitives() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();
    // primitive int
    assertThat(comparator.compare(1, 2)).isNegative();
    assertThat(comparator.compare(1, 1)).isZero();
    assertThat(comparator.compare(2, 1)).isPositive();

    assertThat(comparator.compare(1.0d, 2)).isNegative();
    assertThat(comparator.compare(1.0d, 1)).isZero();
    assertThat(comparator.compare(2.0d, 1)).isPositive();

    assertThat(comparator.compare(1.0f, 2)).isNegative();
    assertThat(comparator.compare(1.0f, 1)).isZero();
    assertThat(comparator.compare(2.0f, 1)).isPositive();

    assertThat(comparator.compare(1L, 2)).isNegative();
    assertThat(comparator.compare(1L, 1)).isZero();
    assertThat(comparator.compare(2L, 1)).isPositive();

    assertThat(comparator.compare(1, 2L)).isNegative();
    assertThat(comparator.compare(1, 1L)).isZero();
    assertThat(comparator.compare(2, 1L)).isPositive();

    assertThat(comparator.compare(1L, 2L)).isNegative();
    assertThat(comparator.compare(1L, 1L)).isZero();
    assertThat(comparator.compare(2L, 1L)).isPositive();
  }

  @Test
  void testNonPrimitiveNumbers() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();

    BigDecimal bdOne = new BigDecimal("1");
    BigDecimal bdTwo = new BigDecimal("2");

    assertThat(comparator.compare(bdOne, bdTwo)).isNegative();
    assertThat(comparator.compare(bdOne, new BigDecimal("1"))).isZero();
    assertThat(comparator.compare(bdTwo, bdOne)).isPositive();

    assertThat(comparator.compare(1, bdTwo)).isNegative();
    assertThat(comparator.compare(1, bdOne)).isZero();
    assertThat(comparator.compare(2, bdOne)).isPositive();

    assertThat(comparator.compare(1.0d, bdTwo)).isNegative();
    assertThat(comparator.compare(1.0d, bdOne)).isZero();
    assertThat(comparator.compare(2.0d, bdOne)).isPositive();

    assertThat(comparator.compare(1.0f, bdTwo)).isNegative();
    assertThat(comparator.compare(1.0f, bdOne)).isZero();
    assertThat(comparator.compare(2.0f, bdOne)).isPositive();

    assertThat(comparator.compare(1L, bdTwo)).isNegative();
    assertThat(comparator.compare(1L, bdOne)).isZero();
    assertThat(comparator.compare(2L, bdOne)).isPositive();

  }

  @Test
  void testNulls() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();
    assertThat(comparator.compare(null, "abc")).isNegative();
    assertThat(comparator.compare(null, null)).isZero();
    assertThat(comparator.compare("abc", null)).isPositive();
  }

  @Test
  void testObjects() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();
    assertThat(comparator.compare("a", "a")).isZero();
    assertThat(comparator.compare("a", "b")).isNegative();
    assertThat(comparator.compare("b", "a")).isPositive();
  }

  @Test
  void testCanCompare() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();
    assertThat(comparator.canCompare(null, 1)).isTrue();
    assertThat(comparator.canCompare(1, null)).isTrue();

    assertThat(comparator.canCompare(2, 1)).isTrue();
    assertThat(comparator.canCompare("abc", "def")).isTrue();
    assertThat(comparator.canCompare("abc", 3)).isFalse();
    assertThat(comparator.canCompare(String.class, 3)).isFalse();
  }

  @Test
  public void shouldUseCustomComparator() {
    TypeComparator comparator = new StandardTypeComparator();
    ComparableType t1 = new ComparableType(1);
    ComparableType t2 = new ComparableType(2);

    assertThat(comparator.canCompare(t1, 2)).isFalse();
    assertThat(comparator.canCompare(t1, t2)).isTrue();
    assertThat(comparator.compare(t1, t1)).isZero();
    assertThat(comparator.compare(t1, t2)).isNegative();
    assertThat(comparator.compare(t2, t1)).isPositive();
  }

  static class ComparableType implements Comparable<ComparableType> {

    private final int id;

    public ComparableType(int id) {
      this.id = id;
    }

    @Override
    public int compareTo(@NotNull ComparableType other) {
      return this.id - other.id;
    }

  }

}
