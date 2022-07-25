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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Handle}.
 *
 * @author Eric Bruneton
 */
public class HandleTest {

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedConstructor() {
    Handle handle1 = new Handle(Opcodes.H_INVOKEINTERFACE, "owner", "name", "descriptor");
    Handle handle2 = new Handle(Opcodes.H_INVOKESPECIAL, "owner", "name", "descriptor");

    assertTrue(handle1.isInterface());
    assertFalse(handle2.isInterface());
    assertEquals("owner.namedescriptor (9 itf)", handle1.toString());
    assertEquals("owner.namedescriptor (7)", handle2.toString());
  }

  @Test
  public void testConstructor() {
    Handle handle = new Handle(Opcodes.H_GETFIELD, "owner", "name", "descriptor", false);

    assertEquals(Opcodes.H_GETFIELD, handle.getTag());
    assertEquals("owner", handle.getOwner());
    assertEquals("name", handle.getName());
    assertEquals("descriptor", handle.getDesc());
    assertFalse(handle.isInterface());
    assertEquals("owner.namedescriptor (1)", handle.toString());
  }

  @Test
  public void testEquals() {
    Handle handle1 = new Handle(Opcodes.H_GETFIELD, "owner", "name", "descriptor", false);
    Handle handle2 = new Handle(Opcodes.H_GETFIELD, "owner", "name", "descriptor", false);
    Handle nullHandle = null;

    final boolean equalsThis = handle1.equals(handle1);
    final boolean equalsSame = handle1.equals(handle2);
    final boolean equalsNull = handle1.equals(nullHandle);
    final boolean equalsHandleWithDifferentTag =
            handle1.equals(new Handle(Opcodes.H_PUTFIELD, "owner", "name", "descriptor", false));
    final boolean equalsHandleWithDifferentOwner =
            handle1.equals(new Handle(Opcodes.H_GETFIELD, "o", "name", "descriptor", false));
    final boolean equalsHandleWithDifferentName =
            handle1.equals(new Handle(Opcodes.H_GETFIELD, "owner", "n", "descriptor", false));
    final boolean equalsHandleWithDifferentDescriptor =
            handle1.equals(new Handle(Opcodes.H_GETFIELD, "owner", "name", "d", false));
    final boolean equalsHandleWithDifferentIsInterface =
            handle1.equals(new Handle(Opcodes.H_GETFIELD, "owner", "n", "descriptor", true));

    assertTrue(equalsThis);
    assertTrue(equalsSame);
    assertFalse(equalsNull);
    assertFalse(equalsHandleWithDifferentTag);
    assertFalse(equalsHandleWithDifferentOwner);
    assertFalse(equalsHandleWithDifferentName);
    assertFalse(equalsHandleWithDifferentDescriptor);
    assertFalse(equalsHandleWithDifferentIsInterface);
  }

  @Test
  public void testHashCode() {
    Handle handle1 = new Handle(Opcodes.H_INVOKESTATIC, "owner", "name", "descriptor", false);
    Handle handle2 = new Handle(Opcodes.H_INVOKESTATIC, "owner", "name", "descriptor", true);

    assertNotEquals(0, handle1.hashCode());
    assertNotEquals(0, handle2.hashCode());
  }
}
