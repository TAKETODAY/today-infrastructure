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

import cn.taketoday.lang.Nullable;

/**
 * Helper class for resolving placeholders in texts. Usually applied to file paths.
 *
 * <p>A text may contain {@code ${...}} placeholders, to be resolved as system properties:
 * e.g. {@code ${user.dir}}. Default values can be supplied using the ":" separator
 * between key and value.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Dave Syer
 * @author TODAY 2021/9/28 22:39
 * @see PropertyPlaceholderHandler#PLACEHOLDER_PREFIX
 * @see PropertyPlaceholderHandler#PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 * @since 4.0
 */
public abstract class SystemPropertyUtils {

  /**
   * Resolve {@code ${...}} placeholders in the given text, replacing them with
   * corresponding system property values.
   *
   * @param text the String to resolve
   * @return the resolved String
   * @throws IllegalArgumentException if there is an unresolvable placeholder
   * @see PropertyPlaceholderHandler#PLACEHOLDER_PREFIX
   * @see PropertyPlaceholderHandler#PLACEHOLDER_SUFFIX
   */
  public static String resolvePlaceholders(String text) {
    return resolvePlaceholders(text, false);
  }

  /**
   * Resolve {@code ${...}} placeholders in the given text, replacing them with
   * corresponding system property values. Unresolvable placeholders with no default
   * value are ignored and passed through unchanged if the flag is set to {@code true}.
   *
   * @param text the String to resolve
   * @param ignoreUnresolvablePlaceholders whether unresolved placeholders are to be ignored
   * @return the resolved String
   * @throws IllegalArgumentException if there is an unresolvable placeholder
   * @see PropertyPlaceholderHandler#PLACEHOLDER_PREFIX
   * @see PropertyPlaceholderHandler#PLACEHOLDER_SUFFIX
   * and the "ignoreUnresolvablePlaceholders" flag is {@code false}
   */
  public static String resolvePlaceholders(String text, boolean ignoreUnresolvablePlaceholders) {
    if (text.isEmpty()) {
      return text;
    }
    PropertyPlaceholderHandler shared = PropertyPlaceholderHandler.shared(ignoreUnresolvablePlaceholders);
    return shared.replacePlaceholders(text, new SystemPropertyPlaceholderResolver(text));
  }

  /**
   * PlaceholderResolver implementation that resolves against system properties
   * and system environment variables.
   */
  private record SystemPropertyPlaceholderResolver(String text) implements PlaceholderResolver {

    @Override
    @Nullable
    public String resolvePlaceholder(String placeholderName) {
      try {
        String propVal = System.getProperty(placeholderName);
        if (propVal == null) {
          // Fall back to searching the system environment.
          propVal = System.getenv(placeholderName);
        }
        return propVal;
      }
      catch (Throwable ex) {
        System.err.println(
                "Could not resolve placeholder '" + placeholderName
                        + "' in [" + this.text + "] as system property: " + ex);
        return null;
      }
    }
  }

}
