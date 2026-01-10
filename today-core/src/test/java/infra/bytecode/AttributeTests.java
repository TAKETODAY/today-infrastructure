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

// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.bytecode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Attribute}.
 *
 * @author Eric Bruneton
 */
class AttributeTests {

  @Test
  void testIsUnknown() {
    assertTrue(new Attribute("Comment").isUnknown());
  }

  @Test
  void testStaticWrite() {
    ClassWriter classWriter = new ClassWriter(0);
    ByteAttribute attribute = new ByteAttribute((byte) 42);
    byte[] content0 = Attribute.write(attribute, classWriter, null, -1, -1, -1);
    byte[] content1 = Attribute.write(attribute, classWriter, null, -1, -1, -1);

    assertEquals(42, content0[0]);
    assertEquals(42, content1[0]);
  }

  @Test
  void testCachedContent() {
    SymbolTable table = new SymbolTable(new ClassWriter(0));
    ByteAttribute attributes = new ByteAttribute((byte) 42);
    attributes.nextAttribute = new ByteAttribute((byte) 123);
    int size = attributes.computeAttributesSize(table, null, -1, -1, -1);
    ByteVector result = new ByteVector();
    attributes.putAttributes(table, result);

    assertEquals(14, size);
    assertEquals(42, result.data[6]);
    assertEquals(123, result.data[13]);
  }

  static class ByteAttribute extends Attribute {

    private byte value;

    ByteAttribute(final byte value) {
      super("Byte");
      this.value = value;
    }

    @Override
    protected ByteVector write(
            final ClassWriter classWriter,
            final byte[] code,
            final int codeLength,
            final int maxStack,
            final int maxLocals) {
      ByteVector result = new ByteVector();
      result.putByte(value++);
      return result;
    }
  }
}
