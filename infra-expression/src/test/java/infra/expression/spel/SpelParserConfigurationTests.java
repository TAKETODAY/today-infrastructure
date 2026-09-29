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

package infra.expression.spel;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Tests for {@link SpelParserConfiguration}. */
class SpelParserConfigurationTests {

  @Test
  void builderAppliesSameDefaultsAsNoArgConstructor() {
    SpelParserConfiguration expected = new SpelParserConfiguration();
    SpelParserConfiguration actual = SpelParserConfiguration.builder().build();
    assertThat(actual.getCompilerMode()).isEqualTo(expected.getCompilerMode());
    assertThat(actual.getCompilerClassLoader()).isEqualTo(expected.getCompilerClassLoader());
    assertThat(actual.isAutoGrowNullReferences()).isEqualTo(expected.isAutoGrowNullReferences());
    assertThat(actual.isAutoGrowCollections()).isEqualTo(expected.isAutoGrowCollections());
    assertThat(actual.getMaximumAutoGrowSize()).isEqualTo(expected.getMaximumAutoGrowSize());
    assertThat(actual.getMaximumExpressionLength()).isEqualTo(expected.getMaximumExpressionLength());
    assertThat(actual.getMaximumBigPowerBits()).isEqualTo(expected.getMaximumBigPowerBits());
    assertThat(actual.getMaximumNestingDepth()).isEqualTo(expected.getMaximumNestingDepth());
  }

  @Test
  void maximumAutoGrowSizeDefaults() {
    assertThat(new SpelParserConfiguration().getMaximumAutoGrowSize()).isEqualTo(256);
    assertThat(SpelParserConfiguration.builder().build().getMaximumAutoGrowSize()).isEqualTo(256);
  }

  @Test
  void withDefaultsMatchesBuilderDefaults() {
    SpelParserConfiguration expected = SpelParserConfiguration.builder().build();
    SpelParserConfiguration actual = SpelParserConfiguration.withDefaults();
    assertThat(actual.getCompilerMode()).isEqualTo(expected.getCompilerMode());
    assertThat(actual.getCompilerClassLoader()).isEqualTo(expected.getCompilerClassLoader());
    assertThat(actual.isAutoGrowNullReferences()).isEqualTo(expected.isAutoGrowNullReferences());
    assertThat(actual.isAutoGrowCollections()).isEqualTo(expected.isAutoGrowCollections());
    assertThat(actual.getMaximumAutoGrowSize()).isEqualTo(expected.getMaximumAutoGrowSize());
    assertThat(actual.getMaximumExpressionLength()).isEqualTo(expected.getMaximumExpressionLength());
    assertThat(actual.getMaximumBigPowerBits()).isEqualTo(expected.getMaximumBigPowerBits());
    assertThat(actual.getMaximumNestingDepth()).isEqualTo(expected.getMaximumNestingDepth());
  }

  @Test
  void builderAppliesCustomValues() {
    ClassLoader classLoader = getClass().getClassLoader();
    SpelParserConfiguration configuration = SpelParserConfiguration.builder()
            .compilerMode(SpelCompilerMode.IMMEDIATE)
            .compilerClassLoader(classLoader)
            .autoGrowNullReferences()
            .autoGrowCollections()
            .maximumAutoGrowSize(99)
            .maximumExpressionLength(100)
            .maximumBigPowerBits(102)
            .maximumNestingDepth(103)
            .build();
    assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.IMMEDIATE);
    assertThat(configuration.getCompilerClassLoader()).isSameAs(classLoader);
    assertThat(configuration.isAutoGrowNullReferences()).isTrue();
    assertThat(configuration.isAutoGrowCollections()).isTrue();
    assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
    assertThat(configuration.getMaximumExpressionLength()).isEqualTo(100);
    assertThat(configuration.getMaximumBigPowerBits()).isEqualTo(102);
    assertThat(configuration.getMaximumNestingDepth()).isEqualTo(103);
  }

  @Test
  void builderRejectsInvalidValues() {
    SpelParserConfiguration.Builder builder = SpelParserConfiguration.builder();
    assertThatIllegalArgumentException().isThrownBy(() -> builder.compilerMode(null));
    assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumAutoGrowSize(-1));
    assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumExpressionLength(0));
    assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumBigPowerBits(0));
    assertThatIllegalArgumentException().isThrownBy(() -> builder.maximumNestingDepth(0));
  }

  @Nested
  class LegacyConstructorTests {

    @Test
    void noArgConstructorAppliesDefaults() {
      SpelParserConfiguration configuration = new SpelParserConfiguration();
      assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.OFF);
      assertThat(configuration.getCompilerClassLoader()).isNull();
      assertThat(configuration.isAutoGrowNullReferences()).isFalse();
      assertThat(configuration.isAutoGrowCollections()).isFalse();
      assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(256);
      assertThat(configuration.getMaximumExpressionLength()).isEqualTo(SpelParserConfiguration.DEFAULT_MAX_EXPRESSION_LENGTH);
      assertThat(configuration.getMaximumBigPowerBits()).isEqualTo(SpelParserConfiguration.DEFAULT_MAX_BIG_POWER_BITS);
    }

    @Test
    void compilerModeAndClassLoaderConstructor() {
      ClassLoader classLoader = getClass().getClassLoader();
      SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, classLoader);
      assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.IMMEDIATE);
      assertThat(configuration.getCompilerClassLoader()).isSameAs(classLoader);
      assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(256);
    }

    @Test
    void autoGrowFlagsConstructor() {
      SpelParserConfiguration configuration = new SpelParserConfiguration(true, true);
      assertThat(configuration.isAutoGrowNullReferences()).isTrue();
      assertThat(configuration.isAutoGrowCollections()).isTrue();
      assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(256);
    }

    @Test
    void autoGrowFlagsAndMaximumAutoGrowSizeConstructor() {
      SpelParserConfiguration configuration = new SpelParserConfiguration(true, true, 99);
      assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
    }

    @Test
    void fiveArgConstructorAppliesAllValues() {
      SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null, true, true, 99);
      assertThat(configuration.getCompilerMode()).isEqualTo(SpelCompilerMode.IMMEDIATE);
      assertThat(configuration.isAutoGrowNullReferences()).isTrue();
      assertThat(configuration.isAutoGrowCollections()).isTrue();
      assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
    }

    @Test
    void sixArgConstructorAppliesMaximumExpressionLength() {
      SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null, true, true, 99, 100);
      assertThat(configuration.getMaximumExpressionLength()).isEqualTo(100);
    }

    @Test
    void canonicalConstructorAppliesAllValues() {
      SpelParserConfiguration configuration = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, null,
              true, true, 99, 100, 102, 103);
      assertThat(configuration.getMaximumAutoGrowSize()).isEqualTo(99);
      assertThat(configuration.getMaximumExpressionLength()).isEqualTo(100);
      assertThat(configuration.getMaximumBigPowerBits()).isEqualTo(102);
      assertThat(configuration.getMaximumNestingDepth()).isEqualTo(103);
    }

    @Test
    void canonicalConstructorRejectsInvalidValues() {
      assertThatIllegalArgumentException().isThrownBy(() ->
              new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, -1, 1, 1, 1));
      assertThatIllegalArgumentException().isThrownBy(() ->
              new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, 0, 1, 0, 1));
      assertThatIllegalArgumentException().isThrownBy(() ->
              new SpelParserConfiguration(SpelCompilerMode.OFF, null, false, false, 0, 1, 1, 0));
    }
  }
}
