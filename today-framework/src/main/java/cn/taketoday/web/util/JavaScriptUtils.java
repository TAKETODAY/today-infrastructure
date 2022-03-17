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

package cn.taketoday.web.util;

/**
 * Utility class for JavaScript escaping.
 * Escapes based on the JavaScript 1.5 recommendation.
 *
 * <p>Reference:
 * <a href="https://developer.mozilla.org/en-US/docs/JavaScript/Guide/Values,_variables,_and_literals#String_literals">
 * JavaScript Guide</a> on Mozilla Developer Network.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class JavaScriptUtils {

  /**
   * Turn JavaScript special characters into escaped characters.
   *
   * @param input the input string
   * @return the string with escaped characters
   */
  public static String javaScriptEscape(String input) {
    StringBuilder filtered = new StringBuilder(input.length());
    char prevChar = '\u0000';
    char c;
    for (int i = 0; i < input.length(); i++) {
      c = input.charAt(i);
      if (c == '"') {
        filtered.append("\\\"");
      }
      else if (c == '\'') {
        filtered.append("\\'");
      }
      else if (c == '\\') {
        filtered.append("\\\\");
      }
      else if (c == '/') {
        filtered.append("\\/");
      }
      else if (c == '\t') {
        filtered.append("\\t");
      }
      else if (c == '\n') {
        if (prevChar != '\r') {
          filtered.append("\\n");
        }
      }
      else if (c == '\r') {
        filtered.append("\\n");
      }
      else if (c == '\f') {
        filtered.append("\\f");
      }
      else if (c == '\b') {
        filtered.append("\\b");
      }
      // No '\v' in Java, use octal value for VT ascii char
      else if (c == '\013') {
        filtered.append("\\v");
      }
      else if (c == '<') {
        filtered.append("\\u003C");
      }
      else if (c == '>') {
        filtered.append("\\u003E");
      }
      // Unicode for PS (line terminator in ECMA-262)
      else if (c == '\u2028') {
        filtered.append("\\u2028");
      }
      // Unicode for LS (line terminator in ECMA-262)
      else if (c == '\u2029') {
        filtered.append("\\u2029");
      }
      else {
        filtered.append(c);
      }
      prevChar = c;

    }
    return filtered.toString();
  }

}
