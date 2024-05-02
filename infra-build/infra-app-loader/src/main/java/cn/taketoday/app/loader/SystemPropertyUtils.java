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

package cn.taketoday.app.loader;

import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * Internal helper class adapted from Framework for resolving placeholders in
 * texts.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
final class SystemPropertyUtils {

  private static final String PLACEHOLDER_PREFIX = "${";

  private static final String PLACEHOLDER_SUFFIX = "}";

  private static final String VALUE_SEPARATOR = ":";

  private static final String SIMPLE_PREFIX = PLACEHOLDER_PREFIX.substring(1);

  private SystemPropertyUtils() { }

  @Nullable
  static String resolvePlaceholders(Properties properties, @Nullable String text) {
    return (text != null) ? parseStringValue(properties, text, text, new HashSet<>()) : null;
  }

  private static String parseStringValue(Properties properties, String value, String current,
          Set<String> visitedPlaceholders) {
    StringBuilder result = new StringBuilder(current);
    int startIndex = current.indexOf(PLACEHOLDER_PREFIX);
    while (startIndex != -1) {
      int endIndex = findPlaceholderEndIndex(result, startIndex);
      if (endIndex == -1) {
        startIndex = -1;
        continue;
      }
      String placeholder = result.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
      String originalPlaceholder = placeholder;
      if (!visitedPlaceholders.add(originalPlaceholder)) {
        throw new IllegalArgumentException(
                "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
      }
      placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
      String propertyValue = resolvePlaceholder(properties, value, placeholder);
      if (propertyValue == null) {
        int separatorIndex = placeholder.indexOf(VALUE_SEPARATOR);
        if (separatorIndex != -1) {
          String actualPlaceholder = placeholder.substring(0, separatorIndex);
          String defaultValue = placeholder.substring(separatorIndex + VALUE_SEPARATOR.length());
          propertyValue = resolvePlaceholder(properties, value, actualPlaceholder);
          propertyValue = (propertyValue != null) ? propertyValue : defaultValue;
        }
      }
      if (propertyValue != null) {
        propertyValue = parseStringValue(properties, value, propertyValue, visitedPlaceholders);
        result.replace(startIndex, endIndex + PLACEHOLDER_SUFFIX.length(), propertyValue);
        startIndex = result.indexOf(PLACEHOLDER_PREFIX, startIndex + propertyValue.length());
      }
      else {
        startIndex = result.indexOf(PLACEHOLDER_PREFIX, endIndex + PLACEHOLDER_SUFFIX.length());
      }
      visitedPlaceholders.remove(originalPlaceholder);
    }
    return result.toString();
  }

  @Nullable
  private static String resolvePlaceholder(@Nullable Properties properties, String text, String placeholderName) {
    String propertyValue = getProperty(placeholderName, null, text);
    if (propertyValue != null) {
      return propertyValue;
    }
    return (properties != null) ? properties.getProperty(placeholderName) : null;
  }

  @Nullable
  static String getProperty(String key) {
    return getProperty(key, null, "");
  }

  @Nullable
  private static String getProperty(String key, @Nullable String defaultValue, String text) {
    try {
      String value = System.getProperty(key);
      value = (value != null) ? value : System.getenv(key);
      value = (value != null) ? value : System.getenv(key.replace('.', '_'));
      value = (value != null) ? value : System.getenv(key.toUpperCase(Locale.ENGLISH).replace('.', '_'));
      return (value != null) ? value : defaultValue;
    }
    catch (Throwable ex) {
      System.err.printf("Could not resolve key '%s' in '%s' as system property or in environment: %s%n", key, text, ex);
      return defaultValue;
    }
  }

  private static int findPlaceholderEndIndex(CharSequence buf, int startIndex) {
    int index = startIndex + PLACEHOLDER_PREFIX.length();
    int withinNestedPlaceholder = 0;
    while (index < buf.length()) {
      if (substringMatch(buf, index, PLACEHOLDER_SUFFIX)) {
        if (withinNestedPlaceholder > 0) {
          withinNestedPlaceholder--;
          index = index + PLACEHOLDER_SUFFIX.length();
        }
        else {
          return index;
        }
      }
      else if (substringMatch(buf, index, SIMPLE_PREFIX)) {
        withinNestedPlaceholder++;
        index = index + SIMPLE_PREFIX.length();
      }
      else {
        index++;
      }
    }
    return -1;
  }

  private static boolean substringMatch(CharSequence str, int index, CharSequence substring) {
    for (int j = 0; j < substring.length(); j++) {
      int i = index + j;
      if (i >= str.length() || str.charAt(i) != substring.charAt(j)) {
        return false;
      }
    }
    return true;
  }

}
