/*
 * Copyright 2017 - 2026 the TODAY authors.
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