/*
 * Copyright 2017 - 2024 the original author or authors.
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
package cn.taketoday.bytecode.tree.analysis;

import org.junit.jupiter.api.Test;

import cn.taketoday.bytecode.Type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link BasicValue}.
 *
 * @author Eric Bruneton
 */
public class BasicValueTest {

  @Test
  public void testIsReference() {
    assertTrue(BasicValue.REFERENCE_VALUE.isReference());
    assertTrue(new BasicValue(Type.forInternalName("[I")).isReference());
    assertFalse(BasicValue.UNINITIALIZED_VALUE.isReference());
    assertFalse(BasicValue.INT_VALUE.isReference());
  }

  @Test
  public void testEquals() {
    boolean equalsSameUninitializedValue =
            new BasicValue(null).equals(BasicValue.UNINITIALIZED_VALUE);
    boolean equalsSameValue = new BasicValue(Type.INT_TYPE).equals(BasicValue.INT_VALUE);
    boolean equalsThis = BasicValue.INT_VALUE.equals(BasicValue.INT_VALUE);
    boolean equalsDifferentClass = BasicValue.INT_VALUE.equals(new Object());

    assertTrue(equalsSameUninitializedValue);
    assertTrue(equalsSameValue);
    assertTrue(equalsThis);
    assertFalse(equalsDifferentClass);
  }

  @Test
  public void testHashCode() {
    assertEquals(0, BasicValue.UNINITIALIZED_VALUE.hashCode());
    assertNotEquals(0, BasicValue.INT_VALUE.hashCode());
  }

  @Test
  public void testToString() {
    assertEquals(".", BasicValue.UNINITIALIZED_VALUE.toString());
    assertEquals("A", BasicValue.RETURNADDRESS_VALUE.toString());
    assertEquals("R", BasicValue.REFERENCE_VALUE.toString());
    assertEquals("LI;", new BasicValue(Type.forInternalName("I")).toString());
  }
}
