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
import infra.util.StringUtils;

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
   * System property to configure the default compiler mode for SpEL expression parsers: {@value}.
   * <p>Prefer {@link Builder#compilerMode(SpelCompilerMode)} when setting the
   * mode for an individual parser. Can also be configured via {@link InfraStrategies}.
   *
   * @see #getCompilerMode()
   */
  public static final String EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "infra.spel.compiler.mode";

  /**
   * System property to configure the maximum length for SpEL expressions: {@value}.
   * <p>Can also be configured via the {@link InfraStrategies} mechanism.
   * Prefer {@link Builder#maximumExpressionLength(int)} for individual parsers.
   *
   * @see SpelParserConfiguration#getMaximumExpressionLength()
   */
  public static final String MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME = "infra.spel.default.max-length";

  /**
   * Default maximum number of operations permitted during SpEL expression evaluation: {@value}.
   *
   * @see #EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME
   */
  public static final int DEFAULT_MAX_OPERATIONS = 10_000;

  /**
   * Default maximum number of bits in a {@link java.math.BigDecimal} or
   * {@link java.math.BigInteger} power result: {@value}.
   * <p>Approximately equivalent to a decimal number with 300,000 digits.
   *
   * @see #EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME
   */
  public static final int DEFAULT_MAX_BIG_POWER_BITS = 1_000_000;

  /**
   * Default maximum size to which a collection or array can automatically grow: {@value}.
   * <p>Aligned with the default auto-grow limit used for data binding
   * support (see {@code DataBinder.DEFAULT_AUTO_GROW_COLLECTION_LIMIT}), for
   * consistency between SpEL and data binding.
   *
   * @see Builder#maximumAutoGrowSize(int)
   */
  public static final int DEFAULT_MAX_AUTO_GROW_SIZE = 256;

  /**
   * Default maximum nesting depth permitted within a SpEL expression: {@value}.
   * <p>This limit guards against deeply nested constructs (for example, nested
   * inline lists or maps, parenthesized expressions, ternary or Elvis expressions,
   * or chained unary operators) that could otherwise drive SpEL's recursive-descent
   * parser to exhaust the current thread's call stack.
   * <p>The default is intentionally conservative so that the limit is reached
   * before the call stack is exhausted on a thread with a typical 1 MB stack,
   * even before the parser has been JIT-compiled.
   * <p><strong>NOTE</strong>: This limit improves diagnostics for the common case
   * by converting what would otherwise be an opaque {@link StackOverflowError}
   * into a descriptive {@link SpelParseException}, but it is <em>not</em> a
   * guaranteed defense against {@code StackOverflowError} under every possible
   * JVM thread stack size configuration. The amount of stack space consumed per
   * level of nesting depends on the JVM, its current JIT compilation state, and
   * the platform; consequently, this default may not suffice on threads configured
   * with a substantially reduced stack size (for example, via a reduced {@code -Xss}
   * setting, as is sometimes done in high-concurrency deployments to support large
   * thread pools). Applications and frameworks that evaluate SpEL expressions from
   * an untrusted source should not rely on this limit alone; see the
   * <a href="https://docs.spring.io/spring-framework/reference/core/expressions/evaluation.html#expressions-evaluation-context-security"
   * >Security Considerations</a> section of the Spring Framework reference
   * documentation for further guidance on evaluating untrusted SpEL expressions.
   *
   * @see Builder#maximumNestingDepth(int)
   */
  public static final int DEFAULT_MAX_EXPRESSION_NESTING_DEPTH = 200;

  /**
   * Property configuring the default maximum number of operations permitted
   * during SpEL expression evaluation: {@value}.
   * <p>Can also be configured via {@link InfraStrategies}. Prefer
   * {@link Builder#maximumOperations(int)} for individual parsers.
   *
   * @see #DEFAULT_MAX_OPERATIONS
   */
  public static final String EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME = "infra.spel.default.max-operations";

  /**
   * Property configuring the default maximum bit length of big-number power
   * results: {@value}.
   * <p>Can also be configured via {@link InfraStrategies}. Prefer
   * {@link Builder#maximumBigPowerBits(int)} for individual parsers.
   *
   * @see #DEFAULT_MAX_BIG_POWER_BITS
   */
  public static final String EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME = "infra.spel.default.max-big-power-bits";

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

  private final int maximumOperations;

  private final int maximumBigPowerBits;

  private final int maximumNestingDepth;

  /**
   * Internal constructor with explicit limits on operations, big-number powers
   * and expression nesting.
   *
   * @param compilerMode compiler mode; must not be {@code null}
   * @param compilerClassLoader class loader for compilation
   * @param autoGrowNullReferences whether to grow null references
   * @param autoGrowCollections whether to grow collections
   * @param maximumAutoGrowSize maximum auto-grow size
   * @param maximumExpressionLength maximum expression length
   * @param maximumOperations maximum number of operations; must be positive
   * @param maximumBigPowerBits maximum bits in a big-number power result
   * @param maximumNestingDepth maximum structural nesting depth; must be positive
   */
  SpelParserConfiguration(SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
          boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize,
          int maximumExpressionLength, int maximumOperations, int maximumBigPowerBits, int maximumNestingDepth) {
    this.compilerMode = compilerMode;
    this.compilerClassLoader = compilerClassLoader;
    this.autoGrowNullReferences = autoGrowNullReferences;
    this.autoGrowCollections = autoGrowCollections;
    this.maximumAutoGrowSize = maximumAutoGrowSize;
    this.maximumExpressionLength = maximumExpressionLength;
    this.maximumOperations = maximumOperations;
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
   * Return the maximum number of operations permitted during SpEL expression
   * evaluation.
   *
   * @see #DEFAULT_MAX_OPERATIONS
   * @since 5.0
   */
  public int getMaximumOperations() {
    return this.maximumOperations;
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

  private static int retrieveMaxOperations() {
    return retrieveConfig(EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME, DEFAULT_MAX_OPERATIONS);
  }

  private static int retrieveMaxBigPowerBits() {
    return retrieveConfig(EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME, DEFAULT_MAX_BIG_POWER_BITS);
  }

  private static int retrieveConfig(String propertyName, int defaultValue) {
    String value = InfraStrategies.getProperty(propertyName);
    if (StringUtils.isBlank(value)) {
      return defaultValue;
    }
    try {
      int val = Integer.parseInt(value.trim());
      if (val <= 0) {
        throw new IllegalArgumentException("Value [%d] for property [%s] must be positive".formatted(val, propertyName));
      }
      return val;
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Failed to parse value for property [%s]: %s"
              .formatted(propertyName, ex.getMessage()), ex);
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

    private @Nullable Integer maximumOperations;

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
     * Set the maximum number of operations permitted during SpEL expression
     * evaluation.
     * <p>Defaults to the value of
     * {@value SpelParserConfiguration#EXPRESSION_MAX_OPERATIONS_PROPERTY_NAME},
     * or {@link SpelParserConfiguration#DEFAULT_MAX_OPERATIONS} when unset.
     *
     * @param maximumOperations the maximum operation count; must be positive
     * @return this builder
     */
    public Builder maximumOperations(int maximumOperations) {
      Assert.isTrue(maximumOperations > 0, "'maximumOperations' must be a positive number");
      this.maximumOperations = maximumOperations;
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
      int maximumOperations = this.maximumOperations != null ? this.maximumOperations : retrieveMaxOperations();
      int maximumBigPowerBits = this.maximumBigPowerBits != null ? this.maximumBigPowerBits : retrieveMaxBigPowerBits();
      return new SpelParserConfiguration(this.compilerMode, this.compilerClassLoader,
              this.autoGrowNullReferences, this.autoGrowCollections, this.maximumAutoGrowSize,
              this.maximumExpressionLength, maximumOperations, maximumBigPowerBits, this.maximumNestingDepth);
    }
  }

}
