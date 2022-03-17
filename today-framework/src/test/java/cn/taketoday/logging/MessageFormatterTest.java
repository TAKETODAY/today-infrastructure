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
package cn.taketoday.logging;

import org.junit.jupiter.api.Test;

import static cn.taketoday.logging.MessageFormatter.format;
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
