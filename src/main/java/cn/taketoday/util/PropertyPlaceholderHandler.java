/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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
 * @author TODAY 2021/9/28 22:26
 * @since 4.0
 */
public class PropertyPlaceholderHandler {
  private static final Logger log = LoggerFactory.getLogger(PropertyPlaceholderHandler.class);
  public static final String PLACE_HOLDER_SUFFIX = "}";
  public static final String PLACE_HOLDER_PREFIX = "#{";
  /** Value separator for system property placeholders: ":". */
  public static final String VALUE_SEPARATOR = ":";

  private static final HashMap<String, String> wellKnownSimplePrefixes = new HashMap<>(4);

  static {
    wellKnownSimplePrefixes.put("}", "{");
    wellKnownSimplePrefixes.put("]", "[");
    wellKnownSimplePrefixes.put(")", "(");
  }

  public static final PropertyPlaceholderHandler strict = new PropertyPlaceholderHandler(
          PLACE_HOLDER_PREFIX, PLACE_HOLDER_SUFFIX, VALUE_SEPARATOR, false);

  public static final PropertyPlaceholderHandler defaults = new PropertyPlaceholderHandler(
          PLACE_HOLDER_PREFIX, PLACE_HOLDER_SUFFIX, VALUE_SEPARATOR, true);

  private final String simplePrefix;
  private final String placeholderPrefix;
  private final String placeholderSuffix;

  @Nullable
  private final String valueSeparator;

  private final boolean ignoreUnresolvablePlaceholders;

  /**
   * Creates a new {@code PropertyPlaceholderHandler} that uses the supplied prefix and suffix.
   * Unresolvable placeholders are ignored.
   *
   * @param placeholderPrefix
   *         the prefix that denotes the start of a placeholder
   * @param placeholderSuffix
   *         the suffix that denotes the end of a placeholder
   */
  public PropertyPlaceholderHandler(String placeholderPrefix, String placeholderSuffix) {
    this(placeholderPrefix, placeholderSuffix, null, true);
  }

  /**
   * Creates a new {@code PropertyPlaceholderHandler} that uses the supplied prefix and suffix.
   *
   * @param placeholderPrefix
   *         the prefix that denotes the start of a placeholder
   * @param placeholderSuffix
   *         the suffix that denotes the end of a placeholder
   * @param valueSeparator
   *         the separating character between the placeholder variable
   *         and the associated default value, if any
   * @param ignoreUnresolvablePlaceholders
   *         indicates whether unresolvable placeholders should
   *         be ignored ({@code true}) or cause an exception ({@code false})
   */
  public PropertyPlaceholderHandler(
          String placeholderPrefix, String placeholderSuffix,
          @Nullable String valueSeparator, boolean ignoreUnresolvablePlaceholders) {

    Assert.notNull(placeholderPrefix, "'placeholderPrefix' must not be null");
    Assert.notNull(placeholderSuffix, "'placeholderSuffix' must not be null");
    this.placeholderPrefix = placeholderPrefix;
    this.placeholderSuffix = placeholderSuffix;
    String simplePrefixForSuffix = wellKnownSimplePrefixes.get(placeholderSuffix);
    if (simplePrefixForSuffix != null && placeholderPrefix.endsWith(simplePrefixForSuffix)) {
      this.simplePrefix = simplePrefixForSuffix;
    }
    else {
      this.simplePrefix = placeholderPrefix;
    }
    this.valueSeparator = valueSeparator;
    this.ignoreUnresolvablePlaceholders = ignoreUnresolvablePlaceholders;
  }

  /**
   * Replaces all placeholders of format {@code ${name}} with the corresponding
   * property from the supplied {@link Properties}.
   *
   * @param value
   *         the value containing the placeholders to be replaced
   * @param properties
   *         the {@code Properties} to use for replacement
   *
   * @return the supplied value with placeholders replaced inline
   */
  public String replacePlaceholders(String value, final Properties properties) {
    Assert.notNull(properties, "'properties' must not be null");
    return replacePlaceholders(value, properties::getProperty);
  }

  /**
   * Replaces all placeholders of format {@code ${name}} with the value returned
   * from the supplied {@link PlaceholderResolver}.
   *
   * @param value
   *         the value containing the placeholders to be replaced
   * @param placeholderResolver
   *         the {@code PlaceholderResolver} to use for replacement
   *
   * @return the supplied value with placeholders replaced inline
   */
  public String replacePlaceholders(String value, PlaceholderResolver placeholderResolver) {
    Assert.notNull(value, "'value' must not be null");
    return parseStringValue(value, placeholderResolver, null);
  }

  protected String parseStringValue(
          String value, PlaceholderResolver placeholderResolver, @Nullable Set<String> visitedPlaceholders) {
//    if (value == null || value.length() <= 3) { // #{} > 3
//      return value;
//    }
    int startIndex = value.indexOf(this.placeholderPrefix);
    if (startIndex == -1) {
      return value;
    }
    boolean traceEnabled = log.isTraceEnabled();
    StringBuilder result = new StringBuilder(value);
    while (startIndex != -1) {
      int endIndex = findPlaceholderEndIndex(result, startIndex);
      if (endIndex != -1) {
        String placeholder = result.substring(startIndex + this.placeholderPrefix.length(), endIndex);
        String originalPlaceholder = placeholder;
        if (visitedPlaceholders == null) {
          visitedPlaceholders = new HashSet<>(4);
        }
        if (!visitedPlaceholders.add(originalPlaceholder)) {
          throw new IllegalArgumentException(
                  "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
        }
        // Recursive invocation, parsing placeholders contained in the placeholder key.
        placeholder = parseStringValue(placeholder, placeholderResolver, visitedPlaceholders);
        // Now obtain the value for the fully resolved key...
        String propVal = placeholderResolver.resolvePlaceholder(placeholder);
        if (propVal == null && this.valueSeparator != null) {
          int separatorIndex = placeholder.indexOf(this.valueSeparator);
          if (separatorIndex != -1) {
            String actualPlaceholder = placeholder.substring(0, separatorIndex);
            String defaultValue = placeholder.substring(separatorIndex + this.valueSeparator.length());
            propVal = placeholderResolver.resolvePlaceholder(actualPlaceholder);
            if (propVal == null) {
              propVal = defaultValue;
            }
          }
        }
        if (propVal != null) {
          // Recursive invocation, parsing placeholders contained in the
          // previously resolved placeholder value.
          propVal = parseStringValue(propVal, placeholderResolver, visitedPlaceholders);
          result.replace(startIndex, endIndex + this.placeholderSuffix.length(), propVal);
          if (traceEnabled) {
            log.trace("Resolved placeholder '{}'", placeholder);
          }
          startIndex = result.indexOf(this.placeholderPrefix, startIndex + propVal.length());
        }
        else if (this.ignoreUnresolvablePlaceholders) {
          // Proceed with unprocessed value.
          startIndex = result.indexOf(this.placeholderPrefix, endIndex + this.placeholderSuffix.length());
        }
        else {
          throw new IllegalArgumentException(
                  "Could not resolve placeholder '" +
                          placeholder + "'" + " in value \"" + value + "\"");
        }
        visitedPlaceholders.remove(originalPlaceholder);
      }
      else {
        startIndex = -1;
      }
    }
    return result.toString();
  }

  private int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
    int index = startIndex + this.placeholderPrefix.length();
    int withinNestedPlaceholder = 0;
    int length = buf.length();
    while (index < length) {
      if (StringUtils.substringMatch(buf, index, this.placeholderSuffix)) {
        if (withinNestedPlaceholder > 0) {
          withinNestedPlaceholder--;
          index = index + this.placeholderSuffix.length();
        }
        else {
          return index;
        }
      }
      else if (StringUtils.substringMatch(buf, index, this.simplePrefix)) {
        withinNestedPlaceholder++;
        index = index + this.simplePrefix.length();
      }
      else {
        index++;
      }
    }
    return -1;
  }

  //---------------------------------------------------------------------
  // static
  //---------------------------------------------------------------------

}
