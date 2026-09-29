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

  /**
   * Create a new {@code SpelParserConfiguration} instance with default settings.
   */
  public SpelParserConfiguration() {
    this(null, null, false, false, Integer.MAX_VALUE);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param compilerMode the compiler mode for the parser
   * @param compilerClassLoader the ClassLoader to use as the basis for expression compilation
   */
  public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader) {
    this(compilerMode, compilerClassLoader, false, false, Integer.MAX_VALUE);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param autoGrowNullReferences if null references should automatically grow
   * @param autoGrowCollections if collections should automatically grow
   * @see #SpelParserConfiguration(boolean, boolean, int)
   */
  public SpelParserConfiguration(boolean autoGrowNullReferences, boolean autoGrowCollections) {
    this(null, null, autoGrowNullReferences, autoGrowCollections, Integer.MAX_VALUE);
  }

  /**
   * Create a new {@code SpelParserConfiguration} instance.
   *
   * @param autoGrowNullReferences if null references should automatically grow
   * @param autoGrowCollections if collections should automatically grow
   * @param maximumAutoGrowSize the maximum size that the collection can auto grow
   */
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
   */
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
   */
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
   */
  public SpelParserConfiguration(@Nullable SpelCompilerMode compilerMode, @Nullable ClassLoader compilerClassLoader,
          boolean autoGrowNullReferences, boolean autoGrowCollections, int maximumAutoGrowSize,
          int maximumExpressionLength, int maximumBigPowerBits) {

    Assert.isTrue(maximumBigPowerBits > 0, "'maximumBigPowerBits' must be a positive number");
    this.compilerMode = (compilerMode != null ? compilerMode : defaultCompilerMode);
    this.compilerClassLoader = compilerClassLoader;
    this.autoGrowNullReferences = autoGrowNullReferences;
    this.autoGrowCollections = autoGrowCollections;
    this.maximumAutoGrowSize = maximumAutoGrowSize;
    this.maximumExpressionLength = maximumExpressionLength;
    this.maximumBigPowerBits = maximumBigPowerBits;
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

}
