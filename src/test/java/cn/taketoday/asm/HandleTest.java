// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
package cn.taketoday.asm;

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
