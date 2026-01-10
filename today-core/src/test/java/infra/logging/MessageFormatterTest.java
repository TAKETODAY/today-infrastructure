/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.logging;

import org.junit.jupiter.api.Test;

import static infra.logging.MessageFormatter.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2019-12-06 23:12
 */
public class MessageFormatterTest {

  @Test
  public void testMessageFormatter() throws Exception {

    final String messagePattern = "message: [{}]";
    final String format = format(messagePattern, "TEST VALUE");

    assertTrue(format.equals("message: [TEST VALUE]"));

    assertNull(format(null, null));

    assertTrue(format(messagePattern, null).equals(messagePattern)); // no params

    assertTrue(format("message: []", "").equals("message: []")); // empty {}
    assertTrue(format("message: [\\{}]", "").equals("message: [{}]")); //isEscapedDelimeter
    assertTrue(format("message: [\\\\{}]", "").equals("message: [\\]")); // double Escaped Delimeter

    // TODO deep append parameters

    Object[] params = new Object[] { //
            "TEST", 123.124D, 123.123F, 123
    };

    final String ret = "string: [TEST], double: [123.124], float: [123.123], int: [123]";

    final String format2 = format("string: [{}], double: [{}], float: [{}], int: [{}]", params);
    System.err.println(format2);
    assertTrue(format2.equals(ret));

    // deep
    Object[] paramsArray = new Object[] { //
            "TEST", new double[] { 123.124D }, new float[] { 123.123F }, //
            new int[] { 123 }, new long[] { 123L }, new byte[] { 123 }, //
            new char[] { 'c' }, new boolean[] { true }, new short[] { 123 }, null, new Object[] { "123", 'c' }
    };
    final String retArray = "s: [TEST], d: [123.124], f: [123.123], i: [123], l: [123], b: [123], c: [c], b: [true], s: [123], null, [123, c]";
    final String formatArray = format("s: [{}], d: {}, f: {}, i: {}, l: {}, b: {}, c: {}, b: {}, s: {}, {}, {}",
            paramsArray);

    System.err.println(formatArray);
    assertEquals(formatArray, retArray);
  }

}
