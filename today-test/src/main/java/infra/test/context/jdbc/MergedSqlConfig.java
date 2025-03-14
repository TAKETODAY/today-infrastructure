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

package infra.test.context.jdbc;

import java.lang.reflect.Array;
import java.util.Arrays;

import infra.core.annotation.AnnotationAttributes;
import infra.core.annotation.AnnotationUtils;
import infra.core.style.ToStringBuilder;
import infra.jdbc.datasource.init.ScriptUtils;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.test.context.TestContextAnnotationUtils;

/**
 * {@code MergedSqlConfig} encapsulates the <em>merged</em> {@link SqlConfig @SqlConfig}
 * attributes declared locally via {@link Sql#config} and globally as a class-level annotation.
 *
 * <p>Explicit local configuration attributes override global configuration attributes.
 *
 * @author Sam Brannen
 * @see SqlConfig
 * @since 4.0
 */
class MergedSqlConfig {

  private static final String COMMENT_PREFIX = "commentPrefix";

  private static final String COMMENT_PREFIXES = "commentPrefixes";

  private final String dataSource;

  private final String transactionManager;

  private final SqlConfig.TransactionMode transactionMode;

  private final String encoding;

  private final String separator;

  private final String[] commentPrefixes;

  private final String blockCommentStartDelimiter;

  private final String blockCommentEndDelimiter;

  private final SqlConfig.ErrorMode errorMode;

  /**
   * Construct a {@code MergedSqlConfig} instance by merging the configuration
   * from the supplied local (potentially method-level) {@code @SqlConfig} annotation
   * with class-level configuration discovered on the supplied {@code testClass}.
   * <p>Local configuration overrides class-level configuration.
   * <p>If the test class is not annotated with {@code @SqlConfig}, no merging
   * takes place and the local configuration is used "as is".
   */
  MergedSqlConfig(SqlConfig localSqlConfig, Class<?> testClass) {
    Assert.notNull(localSqlConfig, "Local @SqlConfig is required");
    Assert.notNull(testClass, "testClass is required");

    AnnotationAttributes mergedAttributes = mergeAttributes(localSqlConfig, testClass);

    this.dataSource = mergedAttributes.getString("dataSource");
    this.transactionManager = mergedAttributes.getString("transactionManager");
    this.transactionMode = getEnum(mergedAttributes, "transactionMode", SqlConfig.TransactionMode.DEFAULT,
            SqlConfig.TransactionMode.INFERRED);
    this.encoding = mergedAttributes.getString("encoding");
    this.separator = getString(mergedAttributes, "separator", ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
    this.commentPrefixes = getCommentPrefixes(mergedAttributes);
    this.blockCommentStartDelimiter = getString(mergedAttributes, "blockCommentStartDelimiter",
            ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER);
    this.blockCommentEndDelimiter = getString(mergedAttributes, "blockCommentEndDelimiter",
            ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
    this.errorMode = getEnum(mergedAttributes, "errorMode", SqlConfig.ErrorMode.DEFAULT, SqlConfig.ErrorMode.FAIL_ON_ERROR);
  }

  private AnnotationAttributes mergeAttributes(SqlConfig localSqlConfig, Class<?> testClass) {
    AnnotationAttributes localAttributes = AnnotationUtils.getAnnotationAttributes(localSqlConfig, false, false);

    // Enforce comment prefix aliases within the local @SqlConfig.
    enforceCommentPrefixAliases(localAttributes);

    // Get global attributes, if any.
    SqlConfig globalSqlConfig = TestContextAnnotationUtils.findMergedAnnotation(testClass, SqlConfig.class);

    // Use local attributes only?
    if (globalSqlConfig == null) {
      return localAttributes;
    }

    AnnotationAttributes globalAttributes = AnnotationUtils.getAnnotationAttributes(globalSqlConfig, false, false);

    // Enforce comment prefix aliases within the global @SqlConfig.
    enforceCommentPrefixAliases(globalAttributes);

    for (String key : globalAttributes.keySet()) {
      Object value = localAttributes.get(key);
      if (isExplicitValue(value)) {
        // Override global attribute with local attribute.
        globalAttributes.put(key, value);

        // Ensure comment prefix aliases are honored during the merge.
        if (key.equals(COMMENT_PREFIX) && isEmptyArray(localAttributes.get(COMMENT_PREFIXES))) {
          globalAttributes.put(COMMENT_PREFIXES, value);
        }
        else if (key.equals(COMMENT_PREFIXES) && isEmptyString(localAttributes.get(COMMENT_PREFIX))) {
          globalAttributes.put(COMMENT_PREFIX, value);
        }
      }
    }
    return globalAttributes;
  }

  /**
   * Get the bean name of the {@link javax.sql.DataSource}.
   *
   * @see SqlConfig#dataSource()
   */
  String getDataSource() {
    return this.dataSource;
  }

  /**
   * Get the bean name of the {@link infra.transaction.PlatformTransactionManager}.
   *
   * @see SqlConfig#transactionManager()
   */
  String getTransactionManager() {
    return this.transactionManager;
  }

  /**
   * Get the {@link SqlConfig.TransactionMode}.
   *
   * @see SqlConfig#transactionMode()
   */
  SqlConfig.TransactionMode getTransactionMode() {
    return this.transactionMode;
  }

  /**
   * Get the encoding for the SQL scripts, if different from the platform
   * encoding.
   *
   * @see SqlConfig#encoding()
   */
  String getEncoding() {
    return this.encoding;
  }

  /**
   * Get the character string used to separate individual statements within the
   * SQL scripts.
   *
   * @see SqlConfig#separator()
   */
  String getSeparator() {
    return this.separator;
  }

  /**
   * Get the prefixes that identify single-line comments within the SQL scripts.
   *
   * @see SqlConfig#commentPrefixes()
   * @since 4.0
   */
  String[] getCommentPrefixes() {
    return this.commentPrefixes;
  }

  /**
   * Get the start delimiter that identifies block comments within the SQL scripts.
   *
   * @see SqlConfig#blockCommentStartDelimiter()
   */
  String getBlockCommentStartDelimiter() {
    return this.blockCommentStartDelimiter;
  }

  /**
   * Get the end delimiter that identifies block comments within the SQL scripts.
   *
   * @see SqlConfig#blockCommentEndDelimiter()
   */
  String getBlockCommentEndDelimiter() {
    return this.blockCommentEndDelimiter;
  }

  /**
   * Get the {@link SqlConfig.ErrorMode}.
   *
   * @see SqlConfig#errorMode()
   */
  SqlConfig.ErrorMode getErrorMode() {
    return this.errorMode;
  }

  /**
   * Provide a String representation of the merged SQL script configuration.
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
            .append("dataSource", this.dataSource)
            .append("transactionManager", this.transactionManager)
            .append("transactionMode", this.transactionMode)
            .append("encoding", this.encoding)
            .append("separator", this.separator)
            .append("commentPrefixes", this.commentPrefixes)
            .append("blockCommentStartDelimiter", this.blockCommentStartDelimiter)
            .append("blockCommentEndDelimiter", this.blockCommentEndDelimiter)
            .append("errorMode", this.errorMode)
            .toString();
  }

  private static <E extends Enum<?>> E getEnum(AnnotationAttributes attributes, String attributeName,
          E inheritedOrDefaultValue, E defaultValue) {

    E value = attributes.getEnum(attributeName);
    if (value == inheritedOrDefaultValue) {
      value = defaultValue;
    }
    return value;
  }

  private static String getString(AnnotationAttributes attributes, String attributeName, String defaultValue) {
    String value = attributes.getString(attributeName);
    if (value.isEmpty()) {
      value = defaultValue;
    }
    return value;
  }

  private static void enforceCommentPrefixAliases(AnnotationAttributes attributes) {
    String commentPrefix = attributes.getString(COMMENT_PREFIX);
    String[] commentPrefixes = attributes.getStringArray(COMMENT_PREFIXES);

    boolean explicitCommentPrefix = !commentPrefix.isEmpty();
    boolean explicitCommentPrefixes = (commentPrefixes.length != 0);
    Assert.isTrue(!(explicitCommentPrefix && explicitCommentPrefixes),
            "You may declare the 'commentPrefix' or 'commentPrefixes' attribute in @SqlConfig but not both");

    if (explicitCommentPrefix) {
      Assert.hasText(commentPrefix, "@SqlConfig(commentPrefix) must contain text");
      attributes.put(COMMENT_PREFIXES, new String[] { commentPrefix });
    }
    else if (explicitCommentPrefixes) {
      for (String prefix : commentPrefixes) {
        Assert.hasText(prefix, "@SqlConfig(commentPrefixes) must not contain empty prefixes");
      }
      attributes.put(COMMENT_PREFIX, commentPrefixes);
    }
    else {
      // We know commentPrefixes is an empty array, so make sure commentPrefix
      // is set to that as well in order to honor the alias contract.
      attributes.put(COMMENT_PREFIX, commentPrefixes);
    }
  }

  private static String[] getCommentPrefixes(AnnotationAttributes attributes) {
    String[] commentPrefix = attributes.getStringArray(COMMENT_PREFIX);
    String[] commentPrefixes = attributes.getStringArray(COMMENT_PREFIXES);

    Assert.state(Arrays.equals(commentPrefix, commentPrefixes),
            "Failed to properly handle 'commentPrefix' and 'commentPrefixes' aliases");

    return (commentPrefixes.length != 0 ? commentPrefixes : ScriptUtils.DEFAULT_COMMENT_PREFIXES);
  }

  /**
   * Determine if the supplied value is an explicit value (i.e., not a default).
   */
  private static boolean isExplicitValue(@Nullable Object value) {
    return !(isEmptyString(value) ||
            isEmptyArray(value) ||
            value == SqlConfig.TransactionMode.DEFAULT ||
            value == SqlConfig.ErrorMode.DEFAULT);
  }

  private static boolean isEmptyString(@Nullable Object value) {
    return (value instanceof String && ((String) value).isEmpty());
  }

  private static boolean isEmptyArray(@Nullable Object value) {
    return (value != null && value.getClass().isArray() && Array.getLength(value) == 0);
  }

}
