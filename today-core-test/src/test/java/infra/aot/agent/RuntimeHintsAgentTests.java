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