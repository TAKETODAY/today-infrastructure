/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Helper class for resolving placeholders in texts. Usually applied to file paths.
 * <p>
 * A text may contain {@code $ ...} placeholders, to be resolved as system properties:
 * e.g. {@code $ user.dir}. Default values can be supplied using the ":" separator between
 * key and value.
 * <p>
 * Adapted from Infra.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see System#getProperty(String)
 * @since 4.0
 */
public abstract class SystemPropertyUtils {

  /**
   * Prefix for system property placeholders: "${".
   */
  public static final String PLACEHOLDER_PREFIX = "${";

  /**
   * Suffix for system property placeholders: "}".
   */
  public static final String PLACEHOLDER_SUFFIX = "}";

  /**
   * Value separator for system property placeholders: ":".
   */
  public static final String VALUE_SEPARATOR = ":";

  private static final String SIMPLE_PREFIX = PLACEHOLDER_PREFIX.substring(1);

  /**
   * Resolve ${...} placeholders in the given text, replacing them with corresponding
   * system property values.
   *
   * @param text the String to resolve
   * @return the resolved String
   * @throws IllegalArgumentException if there is an unresolvable placeholder
   * @see #PLACEHOLDER_PREFIX
   * @see #PLACEHOLDER_SUFFIX
   */
  public static String resolvePlaceholders(String text) {
    if (text == null) {
      return text;
    }
    return parseStringValue(null, text, text, new HashSet<>());
  }

  /**
   * Resolve ${...} placeholders in the given text, replacing them with corresponding
   * system property values.
   *
   * @param properties a properties instance to use in addition to System
   * @param text the String to resolve
   * @return the resolved String
   * @throws IllegalArgumentException if there is an unresolvable placeholder
   * @see #PLACEHOLDER_PREFIX
   * @see #PLACEHOLDER_SUFFIX
   */
  public static String resolvePlaceholders(Properties properties, String text) {
    if (text == null) {
      return text;
    }
    return parseStringValue(properties, text, text, new HashSet<>());
  }

  private static String parseStringValue(Properties properties, String value, String current,
          Set<String> visitedPlaceholders) {

    StringBuilder buf = new StringBuilder(current);

    int startIndex = current.indexOf(PLACEHOLDER_PREFIX);
    while (startIndex != -1) {
      int endIndex = findPlaceholderEndIndex(buf, startIndex);
      if (endIndex != -1) {
        String placeholder = buf.substring(startIndex + PLACEHOLDER_PREFIX.length(), endIndex);
        String originalPlaceholder = placeholder;
        if (!visitedPlaceholders.add(originalPlaceholder)) {
          throw new IllegalArgumentException(
                  "Circular placeholder reference '" + originalPlaceholder + "' in property definitions");
        }
        // Recursive invocation, parsing placeholders contained in the
        // placeholder
        // key.
        placeholder = parseStringValue(properties, value, placeholder, visitedPlaceholders);
        // Now obtain the value for the fully resolved key...
        String propVal = resolvePlaceholder(properties, value, placeholder);
        if (propVal == null) {
          int separatorIndex = placeholder.indexOf(VALUE_SEPARATOR);
          if (separatorIndex != -1) {
            String actualPlaceholder = placeholder.substring(0, separatorIndex);
            String defaultValue = placeholder.substring(separatorIndex + VALUE_SEPARATOR.length());
            propVal = resolvePlaceholder(properties, value, actualPlaceholder);
            if (propVal == null) {
              propVal = defaultValue;
            }
          }
        }
        if (propVal != null) {
          // Recursive invocation, parsing placeholders contained in the
          // previously resolved placeholder value.
          propVal = parseStringValue(properties, value, propVal, visitedPlaceholders);
          buf.replace(startIndex, endIndex + PLACEHOLDER_SUFFIX.length(), propVal);
          startIndex = buf.indexOf(PLACEHOLDER_PREFIX, startIndex + propVal.length());
        }
        else {
          // Proceed with unprocessed value.
          startIndex = buf.indexOf(PLACEHOLDER_PREFIX, endIndex + PLACEHOLDER_SUFFIX.length());
        }
        visitedPlaceholders.remove(originalPlaceholder);
      }
      else {
        startIndex = -1;
      }
    }

    return buf.toString();
  }

  private static String resolvePlaceholder(Properties properties, String text, String placeholderName) {
    String propVal = getProperty(placeholderName, null, text);
    if (propVal != null) {
      return propVal;
    }
    return (properties != null) ? properties.getProperty(placeholderName) : null;
  }

  public static String getProperty(String key) {
    return getProperty(key, null, "");
  }

  public static String getProperty(String key, String defaultValue) {
    return getProperty(key, defaultValue, "");
  }

  /**
   * Search the System properties and environment variables for a value with the
   * provided key. Environment variables in {@code UPPER_CASE} style are allowed where
   * System properties would normally be {@code lower.case}.
   *
   * @param key the key to resolve
   * @param defaultValue the default value
   * @param text optional extra context for an error message if the key resolution fails
   * (e.g. if System properties are not accessible)
   * @return a static property value or null of not found
   */
  public static String getProperty(String key, String defaultValue, String text) {
    try {
      String propVal = System.getProperty(key);
      if (propVal == null) {
        // Fall back to searching the system environment.
        propVal = System.getenv(key);
      }
      if (propVal == null) {
        // Try with underscores.
        String name = key.replace('.', '_');
        propVal = System.getenv(name);
      }
      if (propVal == null) {
        // Try uppercase with underscores as well.
        String name = key.toUpperCase(Locale.ENGLISH).replace('.', '_');
        propVal = System.getenv(name);
      }
      if (propVal != null) {
        return propVal;
      }
    }
    catch (Throwable ex) {
      System.err.println("Could not resolve key '" + key + "' in '" + text
              + "' as system property or in environment: " + ex);
    }
    return defaultValue;
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
