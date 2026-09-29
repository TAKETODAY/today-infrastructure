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

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.expression.spel.standard.SpelExpressionParser;
import infra.util.Assert;
import infra.util.InfraStrategies;

/**
 * Configuration object for the SpEL expression parser.
 * <p>Prefer {@link #builder()} to configure only the settings that differ from
 * their defaults, or {@link #withDefaults()} when none need overriding.
 * The existing constructors are deprecated in favor of the builder.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 * @see #withDefaults()
 * @see #builder()
 * @since 4.0
 */
public class SpelParserConfiguration {

  /**
   * Default maximum size to which a collection or array can automatically grow: {@value}.
   * <p>Aligned with the default limit used for data binding, for consistent
   * auto-growth behavior.
   *
   * @see Builder#maximumAutoGrowSize(int)
   */
  public static final int DEFAULT_MAX_AUTO_GROW_SIZE = 256;

  /**
   * System property to configure the default compiler mode for SpEL expression parsers: {@value}.
   * <p>Prefer {@link Builder#compilerMode(SpelCompilerMode)} when setting the
   * mode for an individual parser. Can also be configured via {@link InfraStrategies}.
   *
   * @see #getCompilerMode()
   */
  public static final String EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "spel.compiler.mode";

  /**
   * System property to configure the maximum length for SpEL expressions: {@value}.
   * <p>Can also be configured via the {@link InfraStrategies} mechanism.
   * Prefer {@link Builder#maximumExpressionLength(int)} for individual parsers.
   *
   * @see SpelParserConfiguration#getMaximumExpressionLength()
   */
  public static final String MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME = "spel.default.max-length";

  /**
   * Default maximum number of bits in a {@link java.math.BigDecimal} or
   * {@link java.math.BigInteger} power result: {@value}.
   * <p>Approximately equivalent to a decimal number with 300,000 digits.
   *
   * @see #EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
   */
  public static final int DEFAULT_MAX_BIG_POWER_BITS = 1_000_000;

  /**
   * Default maximum structural nesting depth of a SpEL expression: {@value}.
   * <p>Deeply nested inline lists, maps, parentheses, conditional expressions,
   * and chained unary operators can exhaust a recursive-descent parser's stack.
   * This limit typically turns that failure into a {@link SpelParseException},
   * but does not guarantee protection from {@link StackOverflowError} for every
   * JVM thread stack size and execution environment. Do not rely on it alone
   * when evaluating expressions from untrusted sources.
   *
   * @see Builder#maximumNestingDepth(int)
   */
  public static final int DEFAULT_MAX_EXPRESSION_NESTING_DEPTH = 1_000;

  /**
   * Property configuring the default maximum bit length of big-number power
   * results: {@value}.
   * <p>Can also be configured via {@link InfraStrategies}. Prefer
   * {@link Builder#maximumBigPowerBits(int)} for individual parsers.
   *
   * @see #DEFAULT_MAX_BIG_POWER_BITS
   */
  public static final String EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME = "spel.default.max-big-power-bits";

  private static final SpelCompilerMode defaultCompilerMode;

  /**
   * Default maximum length permitted for a SpEL expression.
   *
   * @see #MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME
   */
  public static final int DEFAULT_MAX_EXPRESSION_LENGTH;

  static {
    String compilerMode = InfraStrategies.getProperty(EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
    defaultCompilerMode = compilerMode != null ? SpelCompilerMode.valueOf(compilerMode.toUpperCase(Locale.ROOT)) : SpelCompilerMode.OFF;
    DEFAULT_MAX_EXPRESSION_LENGTH = InfraStrategies.getInt(MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME, 10_000);
  }

  private final SpelCompilerMode compilerMode;

  private final @Nullable ClassLoader compilerClassLoader;

  private final boolean autoGrowNullReferences;

  private final boolean autoGrowCollections;

  private final int maximumAutoGrowSize;

  private final int maximumExpressionLength;

  private final int maximumBigPowerBits;

  private final int maximumNestingDepth;

  /**
   * Internal constructor with explicit limits on big-number powers and expression nesting.
   *
   * @param compilerMode compiler mode; must not be {@code null}
   * @param compilerClassLoader class loader for compilation
   * @param autoGrowNullReferences whether to grow null references
   * @param autoGrowCollections whether to grow collections
   * @param maximumAutoGrowSize maximum auto-grow size
   * @param maximumExpressionLength maximum expression length
   * @param maximumBigPowerBits maximum bits in a big-number power result
   * @param maximumNestingDepth maximum structural nesting depth; must be positive
   */
  SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
          boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize,
          int maximumExpressionLength, int maximumBigPowerBits, int maximumNestingDepth) {
    this.compilerMode = compilerMode;
    this.compilerClassLoader = compilerClassLoader;
    this.autoGrowNullReferences = autoGrowNullReferences;
    this.autoGrowCollections = autoGrowCollections;
    this.maximumAutoGrowSize = maximumAutoGrowSize;
    this.maximumExpressionLength = maximumExpressionLength;
    this.maximumBigPowerBits = maximumBigPowerBits;
    this.maximumNestingDepth = maximumNestingDepth;
  }

  /**
   * Return the compiler mode for parsers using this configuration object.
   */
  public SpelCompilerMode getCompilerMode() {
    return this.compilerMode;
  }

  /**
   * Return the ClassLoader to use as the basis for expression compilation.
   */
  public @Nullable ClassLoader getCompilerClassLoader() {
    return this.compilerClassLoader;
  }

  /**
   * Return {@code true} if {@code null} references should be automatically grown.
   */
  public boolean isAutoGrowNullReferences() {
    return this.autoGrowNullReferences;
  }

  /**
   * Return {@code true} if collections should be automatically grown.
   */
  public boolean isAutoGrowCollections() {
    return this.autoGrowCollections;
  }

  /**
   * Return the maximum size that a collection can auto grow.
   *
   * @see #DEFAULT_MAX_AUTO_GROW_SIZE
   */
  public int getMaximumAutoGrowSize() {
    return this.maximumAutoGrowSize;
  }

  /**
   * Return the maximum number of characters that a SpEL expression can contain.
   */
  public int getMaximumExpressionLength() {
    return this.maximumExpressionLength;
  }

  /**
   * Return the maximum number of bits in a {@link java.math.BigDecimal} or
   * {@link java.math.BigInteger} power result.
   *
   * @see #DEFAULT_MAX_BIG_POWER_BITS
   */
  public int getMaximumBigPowerBits() {
    return this.maximumBigPowerBits;
  }

  /**
   * Return the maximum structural nesting depth permitted within an expression.
   *
   * @see #DEFAULT_MAX_EXPRESSION_NESTING_DEPTH
   */
  public int getMaximumNestingDepth() {
    return this.maximumNestingDepth;
  }

  private static int retrieveMaxBigPowerBits() {
    String value = InfraStrategies.getProperty(EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME);
    if (value == null || value.isBlank()) {
      return DEFAULT_MAX_BIG_POWER_BITS;
    }
    try {
      int maxBits = Integer.parseInt(value.trim());
      if (maxBits <= 0) {
        throw new IllegalArgumentException("Value [%d] for property [%s] must be positive".formatted(maxBits, EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME));
      }
      return maxBits;
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Failed to parse value for property [%s]: %s"
              .formatted(EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME, ex.getMessage()), ex);
    }
  }

  /**
   * Create a configuration with the same defaults as {@link #builder()}.
   * <p>Equivalent to {@code SpelParserConfiguration.builder().build()}.
   *
   * @return a configuration with default settings
   * @see #builder()
   * @since 5.0
   */
  public static SpelParserConfiguration withDefaults() {
    return builder().build();
  }

  /**
   * Create a {@link Builder} to override only the settings that differ from
   * their defaults.
   *
   * @return a new configuration builder
   * @see #withDefaults()
   * @since 5.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Fluent configuration of a SpEL parser without specifying unrelated limits.
   * <p>Only override the properties that differ from their defaults. The
   * compiler mode and big-power limit honor the configured
   * {@link InfraStrategies} properties unless overridden here; collection
   * auto-growth defaults to {@value #DEFAULT_MAX_AUTO_GROW_SIZE}.
   *
   * @see SpelParserConfiguration#builder()
   * @since 5.0
   */
  public static final class Builder {

    private SpelCompilerMode compilerMode = defaultCompilerMode;

    private @Nullable ClassLoader compilerClassLoader;

    private boolean autoGrowNullReferences;

    private boolean autoGrowCollections;

    private int maximumAutoGrowSize = DEFAULT_MAX_AUTO_GROW_SIZE;

    private int maximumExpressionLength = DEFAULT_MAX_EXPRESSION_LENGTH;

    private @Nullable Integer maximumBigPowerBits;

    private int maximumNestingDepth = DEFAULT_MAX_EXPRESSION_NESTING_DEPTH;

    private Builder() {
    }

    /**
     * Set the compiler mode for parsers using this configuration.
     * <p>Defaults to the value of
     * {@value SpelParserConfiguration#EXPRESSION_COMPILER_MODE_PROPERTY_NAME},
     * or {@link SpelCompilerMode#OFF} when that property is not set.
     *
     * @param compilerMode the compiler mode; must not be {@code null}
     * @return this builder
     */
    public Builder compilerMode(SpelCompilerMode compilerMode) {
      Assert.notNull(compilerMode, "'compilerMode' is required");
      this.compilerMode = compilerMode;
      return this;
    }

    /**
     * Set the class loader used as the basis for expression compilation.
     * <p>Defaults to {@code null}, indicating the default class loader.
     *
     * @param compilerClassLoader the class loader to use, or {@code null}
     * @return this builder
     */
    public Builder compilerClassLoader(@Nullable ClassLoader compilerClassLoader) {
      this.compilerClassLoader = compilerClassLoader;
      return this;
    }

    /**
     * Enable automatic growth of null references encountered in property paths.
     * <p>Disabled by default.
     *
     * @return this builder
     */
    public Builder autoGrowNullReferences() {
      this.autoGrowNullReferences = true;
      return this;
    }

    /**
     * Enable automatic growth of collections and arrays encountered in
     * property paths. Disabled by default.
     *
     * @return this builder
     * @see #maximumAutoGrowSize(int)
     */
    public Builder autoGrowCollections() {
      this.autoGrowCollections = true;
      return this;
    }

    /**
     * Set the maximum size to which collections and arrays may automatically grow.
     * <p>Defaults to {@value SpelParserConfiguration#DEFAULT_MAX_AUTO_GROW_SIZE}.
     * Zero effectively prevents growth beyond the current size.
     *
     * @param maximumAutoGrowSize the maximum size; must not be negative
     * @return this builder
     * @see #autoGrowCollections()
     */
    public Builder maximumAutoGrowSize(int maximumAutoGrowSize) {
      Assert.isTrue(maximumAutoGrowSize >= 0, "'maximumAutoGrowSize' must not be negative");
      this.maximumAutoGrowSize = maximumAutoGrowSize;
      return this;
    }

    /**
     * Set the maximum length permitted for a SpEL expression.
     * <p>Defaults to {@link SpelParserConfiguration#DEFAULT_MAX_EXPRESSION_LENGTH}.
     *
     * @param maximumExpressionLength the maximum length; must be positive
     * @return this builder
     */
    public Builder maximumExpressionLength(int maximumExpressionLength) {
      Assert.isTrue(maximumExpressionLength > 0, "'maximumExpressionLength' must be a positive number");
      this.maximumExpressionLength = maximumExpressionLength;
      return this;
    }

    /**
     * Set the maximum number of bits in a {@link java.math.BigDecimal} or
     * {@link java.math.BigInteger} power result.
     * <p>Defaults to the value of
     * {@value SpelParserConfiguration#EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME},
     * or {@link SpelParserConfiguration#DEFAULT_MAX_BIG_POWER_BITS} when unset.
     * Use {@link Integer#MAX_VALUE} for effectively no limit.
     *
     * @param maximumBigPowerBits the maximum bit count; must be positive
     * @return this builder
     */
    public Builder maximumBigPowerBits(int maximumBigPowerBits) {
      Assert.isTrue(maximumBigPowerBits > 0, "'maximumBigPowerBits' must be a positive number");
      this.maximumBigPowerBits = maximumBigPowerBits;
      return this;
    }

    /**
     * Set the maximum nesting depth permitted within a SpEL expression.
     * <p>Defaults to
     * {@link SpelParserConfiguration#DEFAULT_MAX_EXPRESSION_NESTING_DEPTH}.
     *
     * @param maximumNestingDepth the maximum depth; must be positive
     * @return this builder
     */
    public Builder maximumNestingDepth(int maximumNestingDepth) {
      Assert.isTrue(maximumNestingDepth > 0, "'maximumNestingDepth' must be a positive number");
      this.maximumNestingDepth = maximumNestingDepth;
      return this;
    }

    /**
     * Build the configured parser configuration.
     *
     * @return a configuration with the specified overrides
     */
    public SpelParserConfiguration build() {
      int maximumBigPowerBits = this.maximumBigPowerBits != null ? this.maximumBigPowerBits : retrieveMaxBigPowerBits();
      return new SpelParserConfiguration(this.compilerMode, this.compilerClassLoader,
              this.autoGrowNullReferences, this.autoGrowCollections, this.maximumAutoGrowSize,
              this.maximumExpressionLength, maximumBigPowerBits, this.maximumNestingDepth);
    }
  }

}
