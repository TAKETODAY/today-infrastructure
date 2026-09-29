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

package infra.expression.spel.standard;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import infra.core.Ordered;
import infra.expression.EvaluationContext;
import infra.expression.Expression;
import infra.expression.spel.SpelCompilerMode;
import infra.expression.spel.SpelParserConfiguration;
import infra.expression.spel.support.SimpleEvaluationContext;
import infra.expression.spel.support.StandardEvaluationContext;

import static infra.expression.spel.standard.SpelExpressionTestUtils.assertIsCompiled;
import static infra.expression.spel.standard.SpelExpressionTestUtils.assertIsNotCompiled;
import static infra.expression.spel.standard.SpelExpressionTestUtils.getInterpretedCount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;

/**
 * Tests for the {@link SpelCompiler}.
 *
 * @author Sam Brannen
 * @author Andy Clement
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
    assertIsCompiled(expression);
  }

  @Test
  void simpleEvaluationContextBlocksCompilationByDefault() {
    SpelExpressionParser parser = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));
    Expression expression = parser.parseExpression("order");
    SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    assertThat(context.isCompilationSupported()).isFalse();

    OrderedComponent component = new OrderedComponent();
    IntStream.rangeClosed(1, 5).forEach(i -> assertThat(expression.getValue(context, component)).isEqualTo(42));
    assertIsNotCompiled(expression);
  }

  @Test
  void simpleEvaluationContextAllowsCompilationWhenSupported() {
    SpelExpressionParser parser = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));
    Expression expression = parser.parseExpression("order");
    SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
            .withCompilationSupported().build();
    assertThat(context.isCompilationSupported()).isTrue();

    OrderedComponent component = new OrderedComponent();
    IntStream.rangeClosed(1, 2).forEach(i -> assertThat(expression.getValue(context, component)).isEqualTo(42));
    assertIsCompiled(expression);
  }

  @Test
  void simpleEvaluationContextIgnoresPrecompiledExpressionByDefault() {
    SpelExpressionParser parser = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null));
    Expression expression = parser.parseExpression("order");
    EvaluationContext standardContext = new StandardEvaluationContext();
    assertThat(standardContext.isCompilationSupported()).isTrue();
    OrderedComponent component = new OrderedComponent();
    IntStream.rangeClosed(1, 2).forEach(i ->
            assertThat(expression.getValue(standardContext, component)).isEqualTo(42));
    assertIsCompiled(expression);

    EvaluationContext simpleContext = SimpleEvaluationContext.forReadOnlyDataBinding().build();
    assertThat(simpleContext.isCompilationSupported()).isFalse();
    int count = getInterpretedCount(expression);
    assertThat(expression.getValue(simpleContext, component)).isEqualTo(42);
    assertThat(getInterpretedCount(expression)).isEqualTo(count + 1);
    assertIsCompiled(expression);
  }

  @Test
  void simpleEvaluationContextSetAsDefaultBlocksCompilationForImplicitContextVariants() {
    SpelExpression expression = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null)).parseRaw("order");
    OrderedComponent component = new OrderedComponent();
    SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
            .withRootObject(component).build();
    assertThat(context.isCompilationSupported()).isFalse();
    expression.setEvaluationContext(context);

    for (int i = 0; i < 5; i++) {
      assertThat(expression.getValue()).isEqualTo(42);
      assertIsNotCompiled(expression);
      assertThat(expression.getValue(Integer.class)).isEqualTo(42);
      assertIsNotCompiled(expression);
      assertThat(expression.getValue(component)).isEqualTo(42);
      assertIsNotCompiled(expression);
      assertThat(expression.getValue(component, Integer.class)).isEqualTo(42);
      assertIsNotCompiled(expression);
    }
  }

  @Test
  void simpleEvaluationContextSetAsDefaultIgnoresPrecompiledExpressionForImplicitContextVariants() {
    SpelExpression expression = new SpelExpressionParser(
            new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null)).parseRaw("order");
    StandardEvaluationContext standardContext = new StandardEvaluationContext();
    assertThat(standardContext.isCompilationSupported()).isTrue();
    OrderedComponent component = new OrderedComponent();
    IntStream.rangeClosed(1, 2).forEach(i ->
            assertThat(expression.getValue(standardContext, component, Integer.class)).isEqualTo(42));
    assertIsCompiled(expression);

    SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
            .withRootObject(component).build();
    assertThat(context.isCompilationSupported()).isFalse();
    expression.setEvaluationContext(context);

    int count = getInterpretedCount(expression);
    assertThat(expression.getValue()).isEqualTo(42);
    assertThat(getInterpretedCount(expression)).isEqualTo(++count);
    assertThat(expression.getValue(Integer.class)).isEqualTo(42);
    assertThat(getInterpretedCount(expression)).isEqualTo(++count);
    assertThat(expression.getValue(component)).isEqualTo(42);
    assertThat(getInterpretedCount(expression)).isEqualTo(++count);
    assertThat(expression.getValue(component, Integer.class)).isEqualTo(42);
    assertThat(getInterpretedCount(expression)).isEqualTo(++count);
    assertIsCompiled(expression);
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
    assertThat(infra.expression.spel.standard.SpelCompiler.compile(expression)).isFalse();
    assertThat(expression.getValue(context)).isEqualTo(false);
    assertThat(SpelCompiler.compile(expression)).isTrue();
    assertIsCompiled(expression);
    assertThat(expression.getValue(context)).isEqualTo(false);

    context.setVariable("user", new User());
    expression = parser.parseExpression("#root.isEditable(#user)");
    assertThat(SpelCompiler.compile(expression)).isFalse();
    assertThat(expression.getValue(context)).asInstanceOf(BOOLEAN).isTrue();
    assertThat(SpelCompiler.compile(expression)).isTrue();
    assertIsCompiled(expression);
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
    private String someField = "";

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
