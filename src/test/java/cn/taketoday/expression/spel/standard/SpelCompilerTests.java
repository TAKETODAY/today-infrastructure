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

package cn.taketoday.expression.spel.standard;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import cn.taketoday.core.Ordered;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.spel.SpelCompilationCoverageTests;
import cn.taketoday.expression.spel.SpelCompilerMode;
import cn.taketoday.expression.spel.SpelParserConfiguration;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

/**
 * Tests for the {@link SpelCompiler}.
 *
 * @author Sam Brannen
 * @author Andy Clement
 * @since 5.1.14
 */
class SpelCompilerTests {

  @Test
    // gh-24357
  void expressionCompilesWhenMethodComesFromPublicInterface() {
    SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
    SpelExpressionParser parser = new SpelExpressionParser(config);

    OrderedComponent component = new OrderedComponent();
    Expression expression = parser.parseExpression("order");

    // Evaluate the expression multiple times to ensure that it gets compiled.
    IntStream.rangeClosed(1, 5).forEach(i -> assertThat(expression.getValue(component)).isEqualTo(42));
  }

  @Test
    // gh-25706
  void defaultMethodInvocation() {
    SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null);
    SpelExpressionParser parser = new SpelExpressionParser(config);

    StandardEvaluationContext context = new StandardEvaluationContext();
    Item item = new Item();
    context.setRootObject(item);

    Expression expression = parser.parseExpression("#root.isEditable2()");
    assertThat(SpelCompiler.compile(expression)).isFalse();
    assertThat(expression.getValue(context)).isEqualTo(false);
    assertThat(SpelCompiler.compile(expression)).isTrue();
    SpelCompilationCoverageTests.assertIsCompiled(expression);
    assertThat(expression.getValue(context)).isEqualTo(false);

    context.setVariable("user", new User());
    expression = parser.parseExpression("#root.isEditable(#user)");
    assertThat(SpelCompiler.compile(expression)).isFalse();
    assertThat(expression.getValue(context)).asInstanceOf(BOOLEAN).isTrue();
    assertThat(SpelCompiler.compile(expression)).isTrue();
    SpelCompilationCoverageTests.assertIsCompiled(expression);
    assertThat(expression.getValue(context)).asInstanceOf(BOOLEAN).isTrue();
  }

  @Test
    // gh-28043
  void changingRegisteredVariableTypeDoesNotResultInFailureInMixedMode() {
    SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.MIXED, null);
    SpelExpressionParser parser = new SpelExpressionParser(config);
    Expression sharedExpression = parser.parseExpression("#bean.value");
    StandardEvaluationContext context = new StandardEvaluationContext();

    Object[] beans = new Object[] { new Bean1(), new Bean2(), new Bean3(), new Bean4() };

    IntStream.rangeClosed(1, 1_000_000).parallel().forEach(count -> {
      context.setVariable("bean", beans[count % 4]);
      assertThat(sharedExpression.getValue(context)).asString().startsWith("1");
    });
  }

  static class OrderedComponent implements Ordered {

    @Override
    public int getOrder() {
      return 42;
    }
  }

  public static class User {

    boolean isAdmin() {
      return true;
    }
  }

  public static class Item implements Editable {

    // some fields
    private final String someField = "";

    // some getters and setters

    @Override
    public boolean hasSomeProperty() {
      return someField != null;
    }
  }

  public interface Editable {

    default boolean isEditable(User user) {
      return user.isAdmin() && hasSomeProperty();
    }

    default boolean isEditable2() {
      return false;
    }

    boolean hasSomeProperty();
  }

  public static class Bean1 {
    public String getValue() {
      return "11";
    }
  }

  public static class Bean2 {
    public Integer getValue() {
      return 111;
    }
  }

  public static class Bean3 {
    public Float getValue() {
      return 1.23f;
    }
  }

  public static class Bean4 {
    public Character getValue() {
      return '1';
    }
  }

}
