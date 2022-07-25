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
package cn.taketoday.bytecode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link TypePath}.
 *
 * @author Eric Bruneton
 */
public class TypePathTest {

  /** Tests that {@link TypePath#getLength()} returns correct values. */
  @Test
  public void testGetLength() {
    assertEquals(5, TypePath.fromString("[.[*0").getLength());
    assertEquals(5, TypePath.fromString("[*0;*[").getLength());
    assertEquals(1, TypePath.fromString("10;").getLength());
    assertEquals(2, TypePath.fromString("1;0;").getLength());
  }

  /** Tests that {@link TypePath#getStep(int)} returns correct values. */
  @Test
  public void testGetStep() {
    TypePath typePath = TypePath.fromString("[.[*7");

    assertEquals(TypePath.ARRAY_ELEMENT, typePath.getStep(0));
    assertEquals(TypePath.INNER_TYPE, typePath.getStep(1));
    assertEquals(TypePath.WILDCARD_BOUND, typePath.getStep(3));
    assertEquals(TypePath.TYPE_ARGUMENT, typePath.getStep(4));
    assertEquals(7, typePath.getStepArgument(4));
  }

  /** Tests that type paths are unchanged via a fromString -> toString transform. */
  @Test
  public void testFromAndToString() {
    assertEquals(null, TypePath.fromString(null));
    assertEquals(null, TypePath.fromString(""));
    assertEquals("[.[*0;", TypePath.fromString("[.[*0").toString());
    assertEquals("[*0;*[", TypePath.fromString("[*0;*[").toString());
    assertEquals("10;", TypePath.fromString("10;").toString());
    assertEquals("1;0;", TypePath.fromString("1;0;").toString());
    assertThrows(IllegalArgumentException.class, () -> TypePath.fromString("-"));
    assertThrows(IllegalArgumentException.class, () -> TypePath.fromString("="));
    assertThrows(IllegalArgumentException.class, () -> TypePath.fromString("1-"));
  }
}
