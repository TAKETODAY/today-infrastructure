/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans;

import cn.taketoday.lang.Nullable;

/**
 * Utility methods for classes that perform bean property access
 * according to the {@link PropertyAccessor} interface.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 17:53
 */
public class PropertyAccessorUtils {

  /**
   * Return the actual property name for the given property path.
   *
   * @param propertyPath the property path to determine the property name
   * for (can include property keys, for example for specifying a map entry)
   * @return the actual property name, without any key elements
   */
  public static String getPropertyName(String propertyPath) {
    int separatorIndex = propertyPath.endsWith(PropertyAccessor.PROPERTY_KEY_SUFFIX)
                         ? propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) : -1;
    return separatorIndex != -1 ? propertyPath.substring(0, separatorIndex) : propertyPath;
  }

  /**
   * Check whether the given property path indicates an indexed or nested property.
   *
   * @param propertyPath the property path to check
   * @return whether the path indicates an indexed or nested property
   */
  public static boolean isNestedOrIndexedProperty(@Nullable String propertyPath) {
    if (propertyPath == null) {
      return false;
    }
    int length = propertyPath.length();
    for (int i = 0; i < length; i++) {
      char ch = propertyPath.charAt(i);
      if (ch == PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR
              || ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine the first nested property separator in the
   * given property path, ignoring dots in keys (like "map[my.key]").
   *
   * @param propertyPath the property path to check
   * @return the index of the nested property separator, or -1 if none
   */
  public static int getFirstNestedPropertySeparatorIndex(String propertyPath) {
    return getNestedPropertySeparatorIndex(propertyPath, false);
  }

  /**
   * Determine the first nested property separator in the
   * given property path, ignoring dots in keys (like "map[my.key]").
   *
   * @param propertyPath the property path to check
   * @return the index of the nested property separator, or -1 if none
   */
  public static int getLastNestedPropertySeparatorIndex(String propertyPath) {
    return getNestedPropertySeparatorIndex(propertyPath, true);
  }

  /**
   * Determine the first (or last) nested property separator in the
   * given property path, ignoring dots in keys (like "map[my.key]").
   *
   * @param propertyPath the property path to check
   * @param last whether to return the last separator rather than the first
   * @return the index of the nested property separator, or -1 if none
   */
  private static int getNestedPropertySeparatorIndex(String propertyPath, boolean last) {
    boolean inKey = false;
    int length = propertyPath.length();
    int i = (last ? length - 1 : 0);
    while (last ? i >= 0 : i < length) {
      switch (propertyPath.charAt(i)) {
        case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR:
        case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR:
          inKey = !inKey;
          break;
        case PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR:
          if (!inKey) {
            return i;
          }
      }
      if (last) {
        i--;
      }
      else {
        i++;
      }
    }
    return -1;
  }

  /**
   * Determine whether the given registered path matches the given property path,
   * either indicating the property itself or an indexed element of the property.
   *
   * @param propertyPath the property path (typically without index)
   * @param registeredPath the registered path (potentially with index)
   * @return whether the paths match
   */
  public static boolean matchesProperty(String registeredPath, String propertyPath) {
    if (!registeredPath.startsWith(propertyPath)) {
      return false;
    }
    if (registeredPath.length() == propertyPath.length()) {
      return true;
    }
    if (registeredPath.charAt(propertyPath.length()) != PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
      return false;
    }
    return registeredPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR, propertyPath.length() + 1)
            == registeredPath.length() - 1;
  }

  /**
   * Determine the canonical name for the given property path.
   * Removes surrounding quotes from map keys:<br>
   * {@code map['key']} &rarr; {@code map[key]}<br>
   * {@code map["key"]} &rarr; {@code map[key]}
   *
   * @param propertyName the bean property path
   * @return the canonical representation of the property path
   */
  public static String canonicalPropertyName(@Nullable String propertyName) {
    if (propertyName == null) {
      return "";
    }

    StringBuilder sb = new StringBuilder(propertyName);
    int searchIndex = 0;
    while (searchIndex != -1) {
      int keyStart = sb.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX, searchIndex);
      searchIndex = -1;
      if (keyStart != -1) {
        int keyEnd = sb.indexOf(
                PropertyAccessor.PROPERTY_KEY_SUFFIX, keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length());
        if (keyEnd != -1) {
          String key = sb.substring(keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length(), keyEnd);
          if ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\""))) {
            sb.delete(keyStart + 1, keyStart + 2);
            sb.delete(keyEnd - 2, keyEnd - 1);
            keyEnd = keyEnd - 2;
          }
          searchIndex = keyEnd + PropertyAccessor.PROPERTY_KEY_SUFFIX.length();
        }
      }
    }
    return sb.toString();
  }

  /**
   * Determine the canonical names for the given property paths.
   *
   * @param propertyNames the bean property paths (as array)
   * @return the canonical representation of the property paths
   * (as array of the same size)
   * @see #canonicalPropertyName(String)
   */
  @Nullable
  public static String[] canonicalPropertyNames(@Nullable String[] propertyNames) {
    if (propertyNames == null) {
      return null;
    }
    String[] result = new String[propertyNames.length];
    for (int i = 0; i < propertyNames.length; i++) {
      result[i] = canonicalPropertyName(propertyNames[i]);
    }
    return result;
  }

}
