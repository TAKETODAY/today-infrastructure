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

package infra.expression.spel;

import org.jspecify.annotations.Nullable;

import java.util.Locale;

import infra.expression.spel.standard.SpelExpressionParser;
import infra.lang.TodayStrategies;

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
   * <p>Can also be configured via the {@link TodayStrategies} mechanism.
   *
   * @see SpelParserConfiguration#getMaximumExpressionLength()
   */
  public static final String MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME = "spel.default.max-length";

  private static final SpelCompilerMode defaultCompilerMode;

  /**
   * Default maximum length permitted for a SpEL expression.
   */
  public static final int DEFAULT_MAX_EXPRESSION_LENGTH;

  static {
    String compilerMode = TodayStrategies.getProperty(EXPRESSION_COMPILER_MODE_PROPERTY_NAME);
    defaultCompilerMode = compilerMode != null ? SpelCompilerMode.valueOf(compilerMode.toUpperCase(Locale.ROOT)) : SpelCompilerMode.OFF;
    DEFAULT_MAX_EXPRESSION_LENGTH = TodayStrategies.getInt(MAX_SPEL_EXPRESSION_LENGTH_PROPERTY_NAME, 10_000);
  }

  private final SpelCompilerMode compilerMode;

  @Nullable
  private final ClassLoader compilerClassLoader;

  private final boolean autoGrowNullReferences;

  private final boolean autoGrowCollections;

  private final int maximumAutoGrowSize;

  private final int maximumExpressionLength;

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

    this.compilerMode = (compilerMode != null ? compilerMode : defaultCompilerMode);
    this.compilerClassLoader = compilerClassLoader;
    this.autoGrowNullReferences = autoGrowNullReferences;
    this.autoGrowCollections = autoGrowCollections;
    this.maximumAutoGrowSize = maximumAutoGrowSize;
    this.maximumExpressionLength = maximumExpressionLength;
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

}
