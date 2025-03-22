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

package infra.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/22 14:19
 */
class DecoratorTests {

  @Test
  void decorateModifiesDelegate() {
    TestDecorator decorator = value -> value + " decorated";
    String result = decorator.decorate("test");
    assertThat(result).isEqualTo("test decorated");
  }

  @Test
  void decorateWithNullDelegateReturnsNull() {
    TestDecorator decorator = value -> value + " decorated";
    String result = decorator.decorate(null);
    assertThat(result).isEqualTo("null decorated");
  }

  @Test
  void andThenChainsDecorators() {
    TestDecorator first = value -> value + " first";
    TestDecorator second = value -> value + " second";

    String result = first.andThen(second).decorate("test");

    assertThat(result).isEqualTo("test first second");
  }

  @Test
  void andThenWithNullDecoratorThrowsException() {
    TestDecorator decorator = value -> value;
    assertThrows(IllegalArgumentException.class, () -> decorator.andThen(null));
  }

  @Test
  void multipleChainingExecutesInOrder() {
    TestDecorator first = value -> value + " 1";
    TestDecorator second = value -> value + " 2";
    TestDecorator third = value -> value + " 3";

    String result = first
            .andThen(second)
            .andThen(third)
            .decorate("test");

    assertThat(result).isEqualTo("test 1 2 3");
  }

  interface TestDecorator extends Decorator<String> {
  }
}