/*
 * Copyright 2017 - 2025 the original author or authors.
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
package infra.bytecode;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Attribute}.
 *
 * @author Eric Bruneton
 */
public class AttributeTest {

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
