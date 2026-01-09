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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see PropertyPlaceholderHandler#PLACEHOLDER_PREFIX
 * @see PropertyPlaceholderHandler#PLACEHOLDER_SUFFIX
 * @see System#getProperty(String)
 * @since 4.0 2021/9/28 22:39
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
        System.err.printf("Could not resolve placeholder '%s' in [%s] as system property: %s%n", placeholderName, this.text, ex);
        return null;
      }
    }
  }

}
