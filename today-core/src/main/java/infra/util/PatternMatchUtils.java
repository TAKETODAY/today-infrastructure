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

package infra.util;

import org.jspecify.annotations.Nullable;

/**
 * Utility methods for simple pattern matching, in particular for Spring's typical
 * {@code xxx*}, {@code *xxx}, {@code *xxx*}, and {@code xxx*yyy} pattern styles.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2.0
 */
public abstract class PatternMatchUtils {

  /**
   * Match a String against the given pattern, supporting direct equality as
   * well as the following simple pattern styles: {@code xxx*}, {@code *xxx},
   * {@code *xxx*}, and {@code xxx*yyy} (with an arbitrary number of pattern parts).
   * <p>Returns {@code false} if the supplied String or pattern is {@code null}.
   *
   * @param pattern the pattern to match against
   * @param str the String to match
   * @return whether the String matches the given pattern
   */
  public static boolean simpleMatch(@Nullable String pattern, @Nullable String str) {
    return simpleMatch(pattern, str, false);
  }

  /**
   * Variant of {@link #simpleMatch(String, String)} that ignores upper/lower case.
   */
  public static boolean simpleMatchIgnoreCase(@Nullable String pattern, @Nullable String str) {
    return simpleMatch(pattern, str, true);
  }

  private static boolean simpleMatch(@Nullable String pattern, @Nullable String str, boolean ignoreCase) {
    if (pattern == null || str == null) {
      return false;
    }

    int firstIndex = pattern.indexOf('*');
    if (firstIndex == -1) {
      return (ignoreCase ? pattern.equalsIgnoreCase(str) : pattern.equals(str));
    }

    if (firstIndex == 0) {
      if (pattern.length() == 1) {
        return true;
      }
      int nextIndex = pattern.indexOf('*', 1);
      if (nextIndex == -1) {
        String part = pattern.substring(1);
        return (ignoreCase ? StringUtils.endsWithIgnoreCase(str, part) : str.endsWith(part));
      }
      String part = pattern.substring(1, nextIndex);
      if (part.isEmpty()) {
        return simpleMatch(pattern.substring(nextIndex), str, ignoreCase);
      }
      int partIndex = indexOf(str, part, 0, ignoreCase);
      while (partIndex != -1) {
        if (simpleMatch(pattern.substring(nextIndex), str.substring(partIndex + part.length()), ignoreCase)) {
          return true;
        }
        partIndex = indexOf(str, part, partIndex + 1, ignoreCase);
      }
      return false;
    }

    return str.length() >= firstIndex
            && checkStartsWith(pattern, str, firstIndex, ignoreCase)
            && simpleMatch(pattern.substring(firstIndex), str.substring(firstIndex), ignoreCase);
  }

  private static boolean checkStartsWith(String pattern, String str, int index, boolean ignoreCase) {
    String part = str.substring(0, index);
    return (ignoreCase ? StringUtils.startsWithIgnoreCase(pattern, part) : pattern.startsWith(part));
  }

  private static int indexOf(String str, String otherStr, int startIndex, boolean ignoreCase) {
    if (!ignoreCase) {
      return str.indexOf(otherStr, startIndex);
    }
    for (int i = startIndex; i <= (str.length() - otherStr.length()); i++) {
      if (str.regionMatches(true, i, otherStr, 0, otherStr.length())) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Match a String against the given patterns, supporting direct equality as
   * well as the following simple pattern styles: {@code xxx*}, {@code *xxx},
   * {@code *xxx*}, and {@code xxx*yyy} (with an arbitrary number of pattern parts).
   * <p>Returns {@code false} if the supplied String is {@code null} or if the
   * supplied patterns array is {@code null} or empty.
   *
   * @param patterns the patterns to match against
   * @param str the String to match
   * @return whether the String matches any of the given patterns
   */
  public static boolean simpleMatch(String @Nullable [] patterns, @Nullable String str) {
    if (patterns != null) {
      for (String pattern : patterns) {
        if (simpleMatch(pattern, str)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Variant of {@link #simpleMatch(String[], String)}  that ignores upper/lower case.
   */
  public static boolean simpleMatchIgnoreCase(String @Nullable [] patterns, @Nullable String str) {
    if (patterns != null) {
      for (String pattern : patterns) {
        if (simpleMatch(pattern, str, true)) {
          return true;
        }
      }
    }
    return false;
  }

}
