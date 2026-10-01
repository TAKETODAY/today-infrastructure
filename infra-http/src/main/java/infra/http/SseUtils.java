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

package infra.http;

import infra.util.Assert;

/**
 * Utility methods for writing content as
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">Server-Sent Events</a>,
 * shared by servlet and reactive SSE support.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class SseUtils {

  /**
   * Append a field value to the output, replacing each line separator with
   * a new line for the same field.
   *
   * @param field the SSE field name, or an empty string for a comment
   * @param value the field value
   * @param output the destination
   */
  public static void appendFieldValue(String field, String value, StringBuilder output) {
    if (value.indexOf('\n') == -1 && value.indexOf('\r') == -1) {
      output.append(value);
      return;
    }
    String replacement = "\n" + field + (field.isEmpty() ? ":" : ": ");
    int length = value.length();
    for (int i = 0; i < length; i++) {
      char ch = value.charAt(i);
      if (ch == '\r') {
        if (i + 1 < length && value.charAt(i + 1) == '\n') {
          i++;
        }
        output.append(replacement);
      }
      else if (ch == '\n') {
        output.append(replacement);
      }
      else {
        output.append(ch);
      }
    }
  }

  /**
   * Assert that a single-line SSE field contains no line separators.
   *
   * @param content the field value
   * @throws IllegalArgumentException if the value contains a line separator
   */
  public static void assertNoLineSeparator(String content) {
    Assert.isTrue(content.indexOf('\n') == -1 && content.indexOf('\r') == -1,
            "illegal character '\\n' or '\\r' in event content");
  }
}
