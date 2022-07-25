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

import java.math.BigDecimal;

import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ExpressionParser;
import cn.taketoday.expression.TypeComparator;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.expression.spel.support.StandardTypeComparator;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for type comparison
 *
 * @author Andy Clement
 * @author Giovanni Dall'Oglio Risso
 */
public class ComparatorTests {

  @Test
  void testPrimitives() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();
    // primitive int
    assertThat(comparator.compare(1, 2) < 0).isTrue();
    assertThat(comparator.compare(1, 1) == 0).isTrue();
    assertThat(comparator.compare(2, 1) > 0).isTrue();

    assertThat(comparator.compare(1.0d, 2) < 0).isTrue();
    assertThat(comparator.compare(1.0d, 1) == 0).isTrue();
    assertThat(comparator.compare(2.0d, 1) > 0).isTrue();

    assertThat(comparator.compare(1.0f, 2) < 0).isTrue();
    assertThat(comparator.compare(1.0f, 1) == 0).isTrue();
    assertThat(comparator.compare(2.0f, 1) > 0).isTrue();

    assertThat(comparator.compare(1L, 2) < 0).isTrue();
    assertThat(comparator.compare(1L, 1) == 0).isTrue();
    assertThat(comparator.compare(2L, 1) > 0).isTrue();

    assertThat(comparator.compare(1, 2L) < 0).isTrue();
    assertThat(comparator.compare(1, 1L) == 0).isTrue();
    assertThat(comparator.compare(2, 1L) > 0).isTrue();

    assertThat(comparator.compare(1L, 2L) < 0).isTrue();
    assertThat(comparator.compare(1L, 1L) == 0).isTrue();
    assertThat(comparator.compare(2L, 1L) > 0).isTrue();
  }

  @Test
  void testNonPrimitiveNumbers() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();

    BigDecimal bdOne = new BigDecimal("1");
    BigDecimal bdTwo = new BigDecimal("2");

    assertThat(comparator.compare(bdOne, bdTwo) < 0).isTrue();
    assertThat(comparator.compare(bdOne, new BigDecimal("1")) == 0).isTrue();
    assertThat(comparator.compare(bdTwo, bdOne) > 0).isTrue();

    assertThat(comparator.compare(1, bdTwo) < 0).isTrue();
    assertThat(comparator.compare(1, bdOne) == 0).isTrue();
    assertThat(comparator.compare(2, bdOne) > 0).isTrue();

    assertThat(comparator.compare(1.0d, bdTwo) < 0).isTrue();
    assertThat(comparator.compare(1.0d, bdOne) == 0).isTrue();
    assertThat(comparator.compare(2.0d, bdOne) > 0).isTrue();

    assertThat(comparator.compare(1.0f, bdTwo) < 0).isTrue();
    assertThat(comparator.compare(1.0f, bdOne) == 0).isTrue();
    assertThat(comparator.compare(2.0f, bdOne) > 0).isTrue();

    assertThat(comparator.compare(1L, bdTwo) < 0).isTrue();
    assertThat(comparator.compare(1L, bdOne) == 0).isTrue();
    assertThat(comparator.compare(2L, bdOne) > 0).isTrue();

  }

  @Test
  void testNulls() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();
    assertThat(comparator.compare(null, "abc") < 0).isTrue();
    assertThat(comparator.compare(null, null) == 0).isTrue();
    assertThat(comparator.compare("abc", null) > 0).isTrue();
  }

  @Test
  void testObjects() throws EvaluationException {
    TypeComparator comparator = new StandardTypeComparator();
    assertThat(comparator.compare("a", "a") == 0).isTrue();
    assertThat(comparator.compare("a", "b") < 0).isTrue();
    assertThat(comparator.compare("b", "a") > 0).isTrue();
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
  public void customComparatorWorksWithEquality() {
    final StandardEvaluationContext ctx = new StandardEvaluationContext();
    ctx.setTypeComparator(customComparator);

    ExpressionParser parser = new SpelExpressionParser();
    Expression expr = parser.parseExpression("'1' == 1");

    assertThat(expr.getValue(ctx, Boolean.class)).isTrue();

  }

  // A silly comparator declaring everything to be equal
  private final TypeComparator customComparator = new TypeComparator() {
    @Override
    public boolean canCompare(@Nullable Object firstObject, @Nullable Object secondObject) {
      return true;
    }

    @Override
    public int compare(@Nullable Object firstObject, @Nullable Object secondObject) throws EvaluationException {
      return 0;
    }

  };

}
