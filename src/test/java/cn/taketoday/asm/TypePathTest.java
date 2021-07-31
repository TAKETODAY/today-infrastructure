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
