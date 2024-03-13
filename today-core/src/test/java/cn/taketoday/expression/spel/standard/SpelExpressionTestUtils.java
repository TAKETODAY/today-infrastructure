/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.expression.spel.standard;

import java.lang.reflect.Field;

import cn.taketoday.expression.Expression;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests utilities for {@link SpelExpression}.
 *
 * @author Stephane Nicoll
 */
public abstract class SpelExpressionTestUtils {

  public static void assertIsCompiled(Expression expression) {
    try {
      Field field = SpelExpression.class.getDeclaredField("compiledAst");
      field.setAccessible(true);
      Object object = field.get(expression);
      assertThat(object).isNotNull();
    }
    catch (Exception ex) {
      throw new AssertionError(ex.getMessage(), ex);
    }
  }

}
