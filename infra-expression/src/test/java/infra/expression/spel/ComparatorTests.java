/*
 * Copyright 2002-present the original author or authors.
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

package infra.expression.spel;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import infra.expression.EvaluationException;
import infra.expression.Expression;
import infra.expression.ExpressionParser;
import infra.expression.TypeComparator;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;
import infra.expression.spel.support.StandardTypeComparator;

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
