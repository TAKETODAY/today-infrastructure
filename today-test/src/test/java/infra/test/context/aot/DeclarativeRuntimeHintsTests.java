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

package infra.test.context.aot;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import infra.aot.generate.InMemoryGeneratedFiles;
import infra.aot.hint.RuntimeHints;
import infra.test.context.aot.samples.hints.DeclarativeRuntimeHintsSpringJupiterTests;

import static infra.aot.hint.predicate.RuntimeHintsPredicates.reflection;
import static infra.aot.hint.predicate.RuntimeHintsPredicates.resource;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for declarative support for registering run-time hints for tests, tested
 * via the {@link TestContextAotGenerator}
 *
 * @author Sam Brannen
 * @since 4.0
 */
class DeclarativeRuntimeHintsTests extends AbstractAotTests {

  private final RuntimeHints runtimeHints = new RuntimeHints();

  private final TestContextAotGenerator generator =
          new TestContextAotGenerator(new InMemoryGeneratedFiles(), this.runtimeHints);

  @Test
  void declarativeRuntimeHints() {
    Class<?> testClass = DeclarativeRuntimeHintsSpringJupiterTests.class;

    this.generator.processAheadOfTime(Stream.of(testClass));

    // @Reflective
    assertReflectionRegistered(testClass);

    // @RegisterReflectionForBinding
    assertReflectionRegistered(DeclarativeRuntimeHintsSpringJupiterTests.SampleClassWithGetter.class);
    assertReflectionRegistered(String.class);
    assertThat(reflection().onMethod(DeclarativeRuntimeHintsSpringJupiterTests.SampleClassWithGetter.class, "getName")).accepts(this.runtimeHints);

    // @ImportRuntimeHints
    assertThat(resource().forResource("org/example/config/enigma.txt")).accepts(this.runtimeHints);
  }

  private void assertReflectionRegistered(Class<?> type) {
    assertThat(reflection().onType(type)).as("Reflection hint for %s", type).accepts(this.runtimeHints);
  }

}
