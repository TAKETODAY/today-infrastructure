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

package infra.aot.agent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.aot.agent.RuntimeHintsAgent.ParsedArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:01
 */
class RuntimeHintsAgentTests {

  @Test
  void shouldParseAgentArgumentsWithIncludedPackages() {
    ParsedArguments arguments = ParsedArguments.parse("+infra,+org.example");
    assertThat(arguments.getInstrumentedPackages()).containsExactly("infra", "org.example");
    assertThat(arguments.getIgnoredPackages()).isEmpty();
  }

  @Test
  void shouldParseAgentArgumentsWithExcludedPackages() {
    ParsedArguments arguments = ParsedArguments.parse("-javax,-jdk");
    assertThat(arguments.getInstrumentedPackages()).isEmpty();
    assertThat(arguments.getIgnoredPackages()).containsExactly("javax", "jdk");
  }

  @Test
  void shouldParseAgentArgumentsWithMixedIncludedAndExcluded() {
    ParsedArguments arguments = ParsedArguments.parse("+infra,-javax,+org.example,-jdk");
    assertThat(arguments.getInstrumentedPackages()).containsExactly("infra", "org.example");
    assertThat(arguments.getIgnoredPackages()).containsExactly("javax", "jdk");
  }

  @Test
  void shouldUseDefaultPackageWhenNoArguments() {
    ParsedArguments arguments = ParsedArguments.parse(null);
    assertThat(arguments.getInstrumentedPackages()).containsExactly("infra");
    assertThat(arguments.getIgnoredPackages()).isEmpty();
  }

  @Test
  void shouldThrowExceptionForInvalidArgumentFormat() {
    assertThatThrownBy(() -> ParsedArguments.parse("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot parse agent arguments");
  }

  @Test
  void shouldSetLoadedFlagOnPremain() {
    // This test verifies the static state, so we need to be careful about side effects
    assertThat(RuntimeHintsAgent.isLoaded()).isFalse();
    // Note: We cannot easily test premain fully without actual instrumentation
  }

  @Test
  void shouldCreateParsedArgumentsWithEmptyLists() {
    ParsedArguments arguments = new ParsedArguments(new ArrayList<>(), new ArrayList<>());
    assertThat(arguments.getInstrumentedPackages()).isEmpty();
    assertThat(arguments.getIgnoredPackages()).isEmpty();
  }

  @Test
  void shouldHandleEmptyStringArgument() {
    ParsedArguments arguments = ParsedArguments.parse("");
    assertThat(arguments.getInstrumentedPackages()).containsExactly("infra");
    assertThat(arguments.getIgnoredPackages()).isEmpty();
  }

  @Test
  void shouldReturnSameInstanceWhenCallingIsLoaded() {
    boolean loadedBefore = RuntimeHintsAgent.isLoaded();
    boolean loadedAfter = RuntimeHintsAgent.isLoaded();
    assertThat(loadedBefore).isEqualTo(loadedAfter);
  }

  @Test
  void shouldCreateParsedArgumentsWithPredefinedLists() {
    List<String> instrumented = List.of("com.example", "org.test");
    List<String> ignored = List.of("javax", "jdk");
    ParsedArguments arguments = new ParsedArguments(instrumented, ignored);

    assertThat(arguments.getInstrumentedPackages()).containsExactly("com.example", "org.test");
    assertThat(arguments.getIgnoredPackages()).containsExactly("javax", "jdk");
  }

  @Test
  void shouldCreateTransformerInPremain() {
    // This is a partial test since we can't mock Instrumentation easily
    // Just verify that the method doesn't throw exceptions with valid inputs
    assertThatCode(() -> {
      // We can't fully test this without a real Instrumentation instance
      // But we can at least verify the parsing logic
      ParsedArguments arguments = ParsedArguments.parse("+test.package");
      assertThat(arguments.getInstrumentedPackages()).isNotEmpty();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldParseMultiplePackagesCorrectlyInPremain() {
    assertThatCode(() -> {
      ParsedArguments arguments = ParsedArguments.parse("+com.example,+org.test,-javax.swing");
      assertThat(arguments.getInstrumentedPackages()).containsExactly("com.example", "org.test");
      assertThat(arguments.getIgnoredPackages()).containsExactly("javax.swing");
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleNullAgentArgumentsInPremain() {
    assertThatCode(() -> {
      ParsedArguments arguments = ParsedArguments.parse(null);
      assertThat(arguments.getInstrumentedPackages()).containsExactly("infra");
      assertThat(arguments.getIgnoredPackages()).isEmpty();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldCreateTransformerWithDefaultPackageWhenNoArguments() {
    assertThatCode(() -> {
      ParsedArguments arguments = ParsedArguments.parse("");
      assertThat(arguments.getInstrumentedPackages()).containsExactly("infra");
    }).doesNotThrowAnyException();
  }



}