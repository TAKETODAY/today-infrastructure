/*
 * Copyright 2026-present the original author or authors.
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

package infra.shell.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author David Pilar
 */
class CommandArgumentTests {

  @Test
  void testBuilderDefaultsToNonVariadic() {
    CommandArgument argument = CommandArgument.with()
            .index(1)
            .description("a description")
            .defaultValue("a default")
            .value("a value")
            .type(String.class)
            .build();

    assertThat(argument.index()).isEqualTo(1);
    assertThat(argument.description()).isEqualTo("a description");
    assertThat(argument.defaultValue()).isEqualTo("a default");
    assertThat(argument.value()).isEqualTo("a value");
    assertThat(argument.type()).isEqualTo(String.class);
    assertThat(argument.variadic()).isFalse();
  }

  @Test
  void testBuilderWithVariadic() {
    CommandArgument argument = CommandArgument.with().index(0).type(Integer.class).variadic(true).build();

    assertThat(argument.variadic()).isTrue();
  }

}
