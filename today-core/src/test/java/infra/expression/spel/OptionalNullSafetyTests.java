/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.expression.spel;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import infra.expression.Expression;
import infra.expression.spel.standard.SpelExpressionParser;
import infra.expression.spel.support.StandardEvaluationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests which verify support for using {@link Optional} with the null-safe and
 * Elvis operators in SpEL expressions.
 *
 * @author Sam Brannen
 * @since 5.0
 */
class OptionalNullSafetyTests {

  private final SpelExpressionParser parser = new SpelExpressionParser();

  private final StandardEvaluationContext context = new StandardEvaluationContext();

  @BeforeEach
  void setUpContext() {
    context.setVariable("service", new Service());
  }

  /**
   * Tests for the status quo when using {@link Optional} in SpEL expressions,
   * before explicit null-safe support was added in 7.0.
   */
  @Nested
  class LegacyOptionalTests {

    @Test
    void accessPropertyOnNullOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName(null).empty");

      assertThatExceptionOfType(SpelEvaluationException.class)
              .isThrownBy(() -> expr.getValue(context))
              .satisfies(ex -> {
                assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE_ON_NULL);
                assertThat(ex).hasMessageContaining("Property or field 'empty' cannot be found on null");
              });
    }

    @Test
    void accessPropertyOnNullOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName(null)?.empty");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void invokeMethodOnNullOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName(null)?.salutation('Master')");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void accessIndexOnNullOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor(null)?.[1]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void projectionOnNullOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor(null)?.![#this.length]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void selectAllOnNullOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor(null)?.?[#this.length > 5]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void selectFirstOnNullOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor(null)?.^[#this.length > 5]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void selectLastOnNullOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor(null)?.$[#this.length > 5]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void elvisOperatorOnNullOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName(null) ?: 'unknown'");

      assertThat(expr.getValue(context)).isEqualTo("unknown");
    }

    @Test
    void accessNonexistentPropertyOnEmptyOptional() {
      assertPropertyNotReadable("#service.findJediByName('').name");
    }

    @Test
    void accessNonexistentPropertyOnNonEmptyOptional() {
      assertPropertyNotReadable("#service.findJediByName('Yoda').name");
    }

    @Test
    void accessOptionalPropertyOnEmptyOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName('').present");

      assertThat(expr.getValue(context, Boolean.class)).isFalse();
    }

    @Test
    void accessOptionalPropertyOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('')?.present");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context, Boolean.class)).isFalse();
      assertThat(expr.getValue(context, Boolean.class)).isFalse();
    }

    @Test
    void accessOptionalPropertyOnNonEmptyOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName('Yoda').present");

      assertThat(expr.getValue(context, Boolean.class)).isTrue();
    }

    @Test
    void accessOptionalPropertyOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('Yoda')?.present");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context, Boolean.class)).isTrue();
      assertThat(expr.getValue(context, Boolean.class)).isTrue();
    }

    @Test
    void invokeOptionalMethodOnEmptyOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName('').orElse('Luke')");

      assertThat(expr.getValue(context)).isEqualTo("Luke");
    }

    @Test
    void invokeOptionalMethodOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('')?.orElse('Luke')");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isEqualTo("Luke");
      assertThat(expr.getValue(context)).isEqualTo("Luke");
    }

    @Test
    void invokeOptionalMethodOnNonEmptyOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName('Yoda').orElse('Luke')");

      assertThat(expr.getValue(context)).isEqualTo(new Jedi("Yoda"));
    }

    @Test
    void invokeOptionalMethodOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('Yoda')?.orElse('Luke')");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isEqualTo(new Jedi("Yoda"));
      assertThat(expr.getValue(context)).isEqualTo(new Jedi("Yoda"));
    }

    private void assertPropertyNotReadable(String expression) {
      Expression expr = parser.parseExpression(expression);

      assertThatExceptionOfType(SpelEvaluationException.class)
              .isThrownBy(() -> expr.getValue(context))
              .satisfies(ex -> {
                assertThat(ex.getMessageCode()).isEqualTo(SpelMessage.PROPERTY_OR_FIELD_NOT_READABLE);
                assertThat(ex).hasMessageContaining("Property or field 'name' cannot be found on object of type 'java.util.Optional'");
              });
    }

  }

  @Nested
  class NullSafeTests {

    @Test
    void accessPropertyOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('')?.name");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isNull();
      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void accessPropertyOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('Yoda')?.name");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isEqualTo("Yoda");
      assertThat(expr.getValue(context)).isEqualTo("Yoda");
    }

    @Test
    void invokeMethodOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('')?.salutation('Master')");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isNull();
      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void invokeMethodOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findJediByName('Yoda')?.salutation('Master')");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isEqualTo("Master Yoda");
      assertThat(expr.getValue(context)).isEqualTo("Master Yoda");
    }

    @Test
    void accessIndexOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('')?.[1]");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isNull();
      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    void accessIndexOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.[1]");

      // Invoke multiple times to ensure there are no caching issues.
      assertThat(expr.getValue(context)).isEqualTo("lemon");
      assertThat(expr.getValue(context)).isEqualTo("lemon");
    }

    @Test
    void projectionOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('')?.![#this.length]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void projectionOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.![#this.length]");

      assertThat(expr.getValue(context, List.class)).containsExactly(6, 5, 5, 9);
    }

    @Test
    void selectAllOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('')?.?[#this.length > 5]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectAllOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.?[#this.length > 5]");

      assertThat(expr.getValue(context, List.class)).containsExactly("banana", "pineapple");
    }

    @Test
    void selectFirstOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('')?.^[#this.length > 5]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectFirstOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.^[#this.length > 5]");

      assertThat(expr.getValue(context, List.class)).containsExactly("banana");
    }

    @Test
    void selectLastOnEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('')?.$[#this.length > 5]");

      assertThat(expr.getValue(context)).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectLastOnNonEmptyOptionalViaNullSafeOperator() {
      Expression expr = parser.parseExpression("#service.findFruitsByColor('yellow')?.$[#this.length > 5]");

      assertThat(expr.getValue(context, List.class)).containsExactly("pineapple");
    }

  }

  @Nested
  class ElvisTests {

    @Test
    void elvisOperatorOnEmptyOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName('') ?: 'unknown'");

      assertThat(expr.getValue(context)).isEqualTo("unknown");
    }

    @Test
    void elvisOperatorOnNonEmptyOptional() {
      Expression expr = parser.parseExpression("#service.findJediByName('Yoda') ?: 'unknown'");

      assertThat(expr.getValue(context)).isEqualTo(new Jedi("Yoda"));
    }

  }

  record Jedi(String name) {

    public String salutation(String salutation) {
      return salutation + " " + this.name;
    }
  }

  static class Service {

    public Optional<Jedi> findJediByName(@Nullable String name) {
      if (name == null) {
        return null;
      }
      if (name.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(new Jedi(name));
    }

    public Optional<List<String>> findFruitsByColor(@Nullable String color) {
      if (color == null) {
        return null;
      }
      if (color.isEmpty()) {
        return Optional.empty();
      }
      return Optional.of(List.of("banana", "lemon", "mango", "pineapple"));
    }

  }

}
