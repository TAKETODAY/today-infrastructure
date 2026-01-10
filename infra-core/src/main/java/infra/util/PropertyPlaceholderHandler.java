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

package infra.util;

import org.jspecify.annotations.Nullable;

import java.util.Properties;

import infra.lang.Assert;

/**
 * Utility class for working with Strings that have placeholder values in them.
 * A placeholder takes the form {@code ${name}}. Using {@code PropertyPlaceholderHandler}
 * these placeholders can be substituted for user-supplied values.
 *
 * <p>Values for substitution can be supplied using a {@link Properties} instance or
 * using a {@link PlaceholderResolver}.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/9/28 22:26
 */
public class PropertyPlaceholderHandler {

  /** Suffix for property placeholders: "}". */
  public static final String PLACEHOLDER_SUFFIX = "}";

  /** Prefix for property placeholders: "${". */
  public static final String PLACEHOLDER_PREFIX = "${";

  /** Value separator for property placeholders: ":". */
  public static final String VALUE_SEPARATOR = ":";

  /** Default escape character: {@code '\'}. */
  public static final char ESCAPE_CHARACTER = '\\';

  public static final PropertyPlaceholderHandler strict = new PropertyPlaceholderHandler(
          PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, ESCAPE_CHARACTER, false);

  public static final PropertyPlaceholderHandler nonStrict = new PropertyPlaceholderHandler(
          PLACEHOLDER_PREFIX, PLACEHOLDER_SUFFIX, VALUE_SEPARATOR, ESCAPE_CHARACTER, true);

  private final PlaceholderParser parser;

  /**
   * Creates a new {@code PropertyPlaceholderHandler} that uses the supplied prefix and suffix.
   * Unresolvable placeholders are ignored.
   *
   * @param placeholderPrefix the prefix that denotes the start of a placeholder
   * @param placeholderSuffix the suffix that denotes the end of a placeholder
   */
  public PropertyPlaceholderHandler(String placeholderPrefix, String placeholderSuffix) {
    this(placeholderPrefix, placeholderSuffix, null, null, true);
  }

  /**
   * Create a new {@code PropertyPlaceholderHelper} that uses the supplied prefix and suffix.
   *
   * @param placeholderPrefix the prefix that denotes the start of a placeholder
   * @param placeholderSuffix the suffix that denotes the end of a placeholder
   * @param valueSeparator the separating character between the placeholder variable
   * and the associated default value, if any
   * @param escapeCharacter the escape character to use to ignore placeholder prefix
   * or value separator, if any
   * @param ignoreUnresolvablePlaceholders indicates whether unresolvable placeholders should
   * be ignored ({@code true}) or cause an exception ({@code false})
   */
  public PropertyPlaceholderHandler(String placeholderPrefix, String placeholderSuffix,
          @Nullable String valueSeparator, @Nullable Character escapeCharacter, boolean ignoreUnresolvablePlaceholders) {
    Assert.notNull(placeholderPrefix, "'placeholderPrefix' is required");
    Assert.notNull(placeholderSuffix, "'placeholderSuffix' is required");
    this.parser = new PlaceholderParser(placeholderPrefix, placeholderSuffix,
            valueSeparator, escapeCharacter, ignoreUnresolvablePlaceholders);
  }

  /**
   * Replaces all placeholders of format {@code ${name}} with the corresponding
   * property from the supplied {@link Properties}.
   *
   * @param value the value containing the placeholders to be replaced
   * @param properties the {@code Properties} to use for replacement
   * @return the supplied value with placeholders replaced inline
   */
  public String replacePlaceholders(String value, final Properties properties) {
    Assert.notNull(properties, "'properties' is required");
    return replacePlaceholders(value, properties::getProperty);
  }

  /**
   * Replaces all placeholders of format {@code ${name}} with the value returned
   * from the supplied {@link PlaceholderResolver}.
   *
   * @param value the value containing the placeholders to be replaced
   * @param placeholderResolver the {@code PlaceholderResolver} to use for replacement
   * @return the supplied value with placeholders replaced inline
   */
  public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
    Assert.notNull(value, "'value' is required");
    return this.parser.replacePlaceholders(value, placeholderResolver);
  }

  // static

  public static PropertyPlaceholderHandler shared(boolean ignoreUnresolvablePlaceholders) {
    return ignoreUnresolvablePlaceholders ? nonStrict : strict;
  }

}
