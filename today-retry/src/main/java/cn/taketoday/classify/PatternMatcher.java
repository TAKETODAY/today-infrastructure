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
package cn.taketoday.classify;

import java.util.ArrayList;
import java.util.Map;

import cn.taketoday.lang.Assert;

/**
 * @param <S> the type of the thing to match a pattern on
 * @author Dave Syer
 * @author Dan Garrette
 */
public class PatternMatcher<S> {

  private final Map<String, S> map;
  private final ArrayList<String> sorted;

  /**
   * Initialize a new {@link PatternMatcher} with a map of patterns to values
   *
   * @param map a map from String patterns to values
   */
  public PatternMatcher(Map<String, S> map) {
    super();
    this.map = map;
    // Sort keys to start with the most specific
    this.sorted = new ArrayList<>(map.keySet());
    this.sorted.sort((o1, o2) -> {
      String s1 = o1; // .replace('?', '{');
      String s2 = o2; // .replace('*', '}');
      return s2.compareTo(s1);
    });
  }

  /**
   * Lifted from AntPathMatcher in Spring Core. Tests whether or not a string matches
   * against a pattern. The pattern may contain two special characters:<br>
   * '*' means zero or more characters<br>
   * '?' means one and only one character
   *
   * @param pattern pattern to match against. Must not be <code>null</code>.
   * @param str string which must be matched against the pattern. Must not be
   * <code>null</code>.
   * @return <code>true</code> if the string matches against the pattern, or
   * <code>false</code> otherwise.
   */
  public static boolean match(String pattern, String str) {
    char[] patArr = pattern.toCharArray();
    char[] strArr = str.toCharArray();
    int patIdxStart = 0;
    int patIdxEnd = patArr.length - 1;
    int strIdxStart = 0;
    int strIdxEnd = strArr.length - 1;
    char ch;

    boolean containsStar = pattern.contains("*");

    if (!containsStar) {
      // No '*'s, so we make a shortcut
      if (patIdxEnd != strIdxEnd) {
        return false; // Pattern and string do not have the same size
      }
      for (int i = 0; i <= patIdxEnd; i++) {
        ch = patArr[i];
        if (ch != '?') {
          if (ch != strArr[i]) {
            return false;// Character mismatch
          }
        }
      }
      return true; // String matches against pattern
    }

    if (patIdxEnd == 0) {
      return true; // Pattern contains only '*', which matches anything
    }

    // Process characters before first star
    while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
      if (ch != '?') {
        if (ch != strArr[strIdxStart]) {
          return false;// Character mismatch
        }
      }
      patIdxStart++;
      strIdxStart++;
    }
    if (strIdxStart > strIdxEnd) {
      // All characters in the string are used. Check if only '*'s are
      // left in the pattern. If so, we succeeded. Otherwise failure.
      for (int i = patIdxStart; i <= patIdxEnd; i++) {
        if (patArr[i] != '*') {
          return false;
        }
      }
      return true;
    }

    // Process characters after last star
    while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
      if (ch != '?') {
        if (ch != strArr[strIdxEnd]) {
          return false;// Character mismatch
        }
      }
      patIdxEnd--;
      strIdxEnd--;
    }
    if (strIdxStart > strIdxEnd) {
      // All characters in the string are used. Check if only '*'s are
      // left in the pattern. If so, we succeeded. Otherwise failure.
      for (int i = patIdxStart; i <= patIdxEnd; i++) {
        if (patArr[i] != '*') {
          return false;
        }
      }
      return true;
    }

    // process pattern between stars. padIdxStart and patIdxEnd point
    // always to a '*'.
    while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
      int patIdxTmp = -1;
      for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
        if (patArr[i] == '*') {
          patIdxTmp = i;
          break;
        }
      }
      if (patIdxTmp == patIdxStart + 1) {
        // Two stars next to each other, skip the first one.
        patIdxStart++;
        continue;
      }
      // Find the pattern between padIdxStart & padIdxTmp in str between
      // strIdxStart & strIdxEnd
      int patLength = (patIdxTmp - patIdxStart - 1);
      int strLength = (strIdxEnd - strIdxStart + 1);
      int foundIdx = -1;
      strLoop:
      for (int i = 0; i <= strLength - patLength; i++) {
        for (int j = 0; j < patLength; j++) {
          ch = patArr[patIdxStart + j + 1];
          if (ch != '?') {
            if (ch != strArr[strIdxStart + i + j]) {
              continue strLoop;
            }
          }
        }

        foundIdx = strIdxStart + i;
        break;
      }

      if (foundIdx == -1) {
        return false;
      }

      patIdxStart = patIdxTmp;
      strIdxStart = foundIdx + patLength;
    }

    // All characters in the string are used. Check if only '*'s are left
    // in the pattern. If so, we succeeded. Otherwise failure.
    for (int i = patIdxStart; i <= patIdxEnd; i++) {
      if (patArr[i] != '*') {
        return false;
      }
    }

    return true;
  }

  /**
   * <p>
   * This method takes a String key and a map from Strings to values of any type. During
   * processing, the method will identify the most specific key in the map that matches
   * the line. Once the correct is identified, its value is returned. Note that if the
   * map contains the wildcard string "*" as a key, then it will serve as the "default"
   * case, matching every line that does not match anything else.
   *
   * <p>
   * If no matching prefix is found, a {@link IllegalStateException} will be thrown.
   *
   * <p>
   * Null keys are not allowed in the map.
   *
   * @param line An input string
   * @return the value whose prefix matches the given line
   */
  public S match(String line) {
    Assert.notNull(line, "A non-null key must be provided to match against.");
    S value = null;
    for (String key : this.sorted) {
      if (PatternMatcher.match(key, line)) {
        value = this.map.get(key);
        break;
      }
    }

    if (value == null) {
      throw new IllegalStateException("Could not find a matching pattern for key=[" + line + "]");
    }
    return value;

  }

}
