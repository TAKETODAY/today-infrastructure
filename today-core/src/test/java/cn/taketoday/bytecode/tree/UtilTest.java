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
package cn.taketoday.bytecode.tree;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Util}.
 *
 * @author Eric Bruneton
 */
public class UtilTest {

  @Test
  public void testAsArrayList_nullArray() {
    assertTrue(Util.asArrayList((Object[]) null).isEmpty());
    assertTrue(Util.asArrayList((byte[]) null).isEmpty());
    assertTrue(Util.asArrayList((boolean[]) null).isEmpty());
    assertTrue(Util.asArrayList((short[]) null).isEmpty());
    assertTrue(Util.asArrayList((char[]) null).isEmpty());
    assertTrue(Util.asArrayList((int[]) null).isEmpty());
    assertTrue(Util.asArrayList((float[]) null).isEmpty());
    assertTrue(Util.asArrayList((long[]) null).isEmpty());
    assertTrue(Util.asArrayList((double[]) null).isEmpty());
  }

  @Test
  public void testAsArrayList_withLength() {
    List<String> strings = Util.asArrayList(3);

    assertEquals(3, strings.size());
    assertNull(strings.get(0));
    assertNull(strings.get(1));
    assertNull(strings.get(2));
  }

  @Test
  public void testAsArrayList_withLengthAndArray() {
    List<Integer> ints = Util.asArrayList(3, new Integer[] { 1, 2, 3, 4, 5 });

    assertEquals(3, ints.size());
    assertEquals(Integer.valueOf(1), ints.get(0));
    assertEquals(Integer.valueOf(2), ints.get(1));
    assertEquals(Integer.valueOf(3), ints.get(2));
  }
}
