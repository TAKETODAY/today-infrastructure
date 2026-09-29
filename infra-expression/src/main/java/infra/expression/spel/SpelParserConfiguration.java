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
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SpelExpressionParser#SpelExpressionParser(SpelParserConfiguration)
 * @since 4.0
 */
public class SpelParserConfiguration {

  /** Create a configuration using the builder defaults. */
  public static SpelParserConfiguration withDefaults() {
    return builder().build();
  }

  /** Create a builder for selectively overriding parser defaults. */
  public static Builder builder() {
    return new Builder();
  }

  /** Default maximum size to which a collection or array can automatically grow. */
  public static final int DEFAULT_MAX_AUTO_GROW_SIZE = 256;

  /**
   * System property to configure the default compiler mode for SpEL expression parsers: {@value}.
   *
   * @see #getCompilerMode()
   */
  public static final String EXPRESSION_COMPILER_MODE_PROPERTY_NAME = "spel.compiler.mode";

  /**
   * System property to configure the maximum length for SpEL expressions: {@value}.
   * <p>Can also be configured via the {@link InfraStrategies} mechanism.
   *
   * @see SpelParserConfiguration#getMaximumExpressionLength()
   */
  public static final String MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME = "spel.default.max-length";

  /** Default maximum number of bits in a BigDecimal or BigInteger power result. */
  public static final int DEFAULT_MAX_BIG_POWER_BITS = 1_000_000;

  /** Default maximum structural nesting depth of a SpEL expression. */
  public static final int DEFAULT_MAX_EXPRESSION_NESTING_DEPTH = 1_000;

  /** Property configuring the default maximum bit length of big-number power results. */
  public static final String EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME = "spel.default.max-big-power-bits";

  private static final SpelCompilerMode defaultCompilerMode;

  /**
   * Default maximum length permitted for a SpEL expression.
   */
  public static final int DEFAULT_MAX_EXPRESSION_LENGTH;

  static {
    String compilerMode = InfraStrategies.getProperty(EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
    defaultCompilerMode = compilerMode != null ? SpelCompilerMode.valueOf(compilerMode.toUpperCase(Locale.ROOT)) : SpelCompilerMode.OFF;
    DEFAULT_MAX_EXPRESSION_LENGTH = InfraStrategies.getInt(MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME, 10_000);
  }

  private final SpelCompilerMode compilerMode;

  @Nullable
  private final ClassLoader compilerClassLoader;

  private final boolean autoGrowNullReferences;

  private final boolean autoGrowCollections;

  private final int maximumAutoGrowSize;

  private final int maximumExpressionLength;

  private final int maximumBigPowerBits;

  private final int maximumNestingDepth;

  /**
   * Create a new {@code SpelParserConfiguration} instance with default settings.
   * @deprecated in favor of {@link #withDefaults()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration() {
    this(null, null, false, false, Integer.MAX_VALUE);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param compilerMode the compiler mode for the parser
   * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
   * @deprecated in favor of {@link #builder()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
    this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param autoGrowNullReferences if null references should automatically grow
   * @param autoGrowCollections if collections should automatically grow
   * @see #SpelParserConfiguration(boolean, boolean, int)
   * @deprecated in favor of {@link #builder()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
    this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param autoGrowNullReferences if null references should automatically grow
   * @param autoGrowCollections if collections should automatically grow
   * @param maximumAutoGrowSize the maximum size that the collection can auto grow;
   * zero disables growth, and negative values are not allowed
   * @deprecated in favor of {@link #builder()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {
    this(null, null, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param compilerMode the compiler mode that parsers using this configuration object should use
   * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
   * @param autoGrowNullReferences if null references should automatically grow
   * @param autoGrowCollections if collections should automatically grow
   * @param maximumAutoGrowSize the maximum size that the collection can auto grow
   * @deprecated in favor of {@link #builder()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
          boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize) {

    this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections,
            maximumAutoGrowSize, DEFAULT_MAX_EXPRESSION_LENGTH);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param compilerMode the compiler mode that parsers using this configuration object should use
   * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
   * @param autoGrowNullReferences if null references should automatically grow
   * @param autoGrowCollections if collections should automatically grow
   * @param maximumAutoGrowSize the maximum size that a collection can auto grow
   * @param maximumExpressionLength the maximum length of a SpEL expression;
   * must be a positive number
   * @deprecated in favor of {@link #builder()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
          boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize, int maximumExpressionLength) {

    this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections,
            maximumAutoGrowSize, maximumExpressionLength, retrieveMaxBigPowerBits());
  }

  /**
   * Create a parser configuration with an explicit limit on big-number power results.
   *
   * @param compilerMode compiler mode, or {@code null} for the default
   * @param compilerClassLoader class loader for compilation
   * @param autoGrowNullReferences whether to grow null references
   * @param autoGrowCollections whether to grow collections
   * @param maximumAutoGrowSize maximum auto-grow size
   * @param maximumExpressionLength maximum expression length
   * @param maximumBigPowerBits maximum bits in a BigDecimal or BigInteger power result
   * @deprecated in favor of {@link #builder()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
          boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize,
          int maximumExpressionLength, int maximumBigPowerBits) {

    this(compilerMode, compilerClassLoader, autoGrowNullReferences, autoGrowCollections, maximumAutoGrowSize,
            maximumExpressionLength, maximumBigPowerBits, DEFAULT_MAX_EXPRESSION_NESTING_DEPTH);
  }

  /**
   * Create a parser configuration with explicit limits on big-number powers and expression nesting.
   *
   * @param compilerMode compiler mode, or {@code null} for the default
   * @param compilerClassLoader class loader for compilation
   * @param autoGrowNullReferences whether to grow null references
   * @param autoGrowCollections whether to grow collections
   * @param maximumAutoGrowSize maximum auto-grow size
   * @param maximumExpressionLength maximum expression length
   * @param maximumBigPowerBits maximum bits in a big-number power result
   * @param maximumNestingDepth maximum structural nesting depth; must be positive
   * @deprecated in favor of {@link #builder()}
   */
  @Deprecated(since = "5.0")
  public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
          boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize,
          int maximumExpressionLength, int maximumBigPowerBits, int maximumNestingDepth) {

    Assert.isTrue(maximumBigPowerBits > 0, "'maximumBigPowerBits' must be a positive number");
    Assert.isTrue(maximumNestingDepth > 0, "'maximumNestingDepth' must be a positive number");
    Assert.isTrue(maximumAutoGrowSize >= 0, "'maximumAutoGrowSize' must not be negative");
    this.compilerMode = (compilerMode != null ? compilerMode : defaultCompilerMode);
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
  @Nullable
  public ClassLoader getCompilerClassLoader() {
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

  /** Return the maximum number of bits in a big-number power result. */
  public int getMaximumBigPowerBits() {
    return this.maximumBigPowerBits;
  }

  /** Return the maximum structural nesting depth permitted within an expression. */
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
      Assert.isTrue(maxBits > 0, () -> "Value [" + maxBits + "] for property [" +
              EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME + "] must be positive");
      return maxBits;
    }
    catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Failed to parse value for property [" +
              EXPRESSION_MAX_BIG_POWER_BITS_PROPERTY_NAME + "]: " + ex.getMessage(), ex);
    }
  }

  /** Fluent configuration of a SpEL parser without specifying unrelated limits. */
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

    /** Set the compiler mode. */
    public Builder compilerMode(SpelCompilerMode compilerMode) {
      Assert.notNull(compilerMode, "'compilerMode' must not be null");
      this.compilerMode = compilerMode;
      return this;
    }

    /** Set the class loader used as the basis for expression compilation. */
    public Builder compilerClassLoader(@Nullable ClassLoader compilerClassLoader) {
      this.compilerClassLoader = compilerClassLoader;
      return this;
    }

    /** Enable automatic growth of null references. */
    public Builder autoGrowNullReferences() {
      this.autoGrowNullReferences = true;
      return this;
    }

    /** Enable automatic growth of collections. */
    public Builder autoGrowCollections() {
      this.autoGrowCollections = true;
      return this;
    }

    /** Set the maximum size to which collections may grow. */
    public Builder maximumAutoGrowSize(int maximumAutoGrowSize) {
      Assert.isTrue(maximumAutoGrowSize >= 0, "'maximumAutoGrowSize' must not be negative");
      this.maximumAutoGrowSize = maximumAutoGrowSize;
      return this;
    }

    /** Set the maximum expression length. */
    public Builder maximumExpressionLength(int maximumExpressionLength) {
      Assert.isTrue(maximumExpressionLength > 0, "'maximumExpressionLength' must be a positive number");
      this.maximumExpressionLength = maximumExpressionLength;
      return this;
    }

    /** Set the maximum number of bits in a big-number power result. */
    public Builder maximumBigPowerBits(int maximumBigPowerBits) {
      Assert.isTrue(maximumBigPowerBits > 0, "'maximumBigPowerBits' must be a positive number");
      this.maximumBigPowerBits = maximumBigPowerBits;
      return this;
    }

    /** Set the maximum expression nesting depth. */
    public Builder maximumNestingDepth(int maximumNestingDepth) {
      Assert.isTrue(maximumNestingDepth > 0, "'maximumNestingDepth' must be a positive number");
      this.maximumNestingDepth = maximumNestingDepth;
      return this;
    }

    /** Build the configured parser configuration. */
    @SuppressWarnings("deprecation")
    public SpelParserConfiguration build() {
      int maximumBigPowerBits = this.maximumBigPowerBits != null ? this.maximumBigPowerBits : retrieveMaxBigPowerBits();
      return new SpelParserConfiguration(this.compilerMode, this.compilerClassLoader,
              this.autoGrowNullReferences, this.autoGrowCollections, this.maximumAutoGrowSize,
              this.maximumExpressionLength, maximumBigPowerBits, this.maximumNestingDepth);
    }
  }

}
