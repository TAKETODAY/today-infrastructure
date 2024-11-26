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
package infra.bytecode.tree.analysis;

import org.junit.jupiter.api.Test;

import infra.bytecode.Opcodes;
import infra.bytecode.tree.InsnNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SourceValue} tests.
 *
 * @author Eric Bruneton
 */
public class SourceValueTest {

  @Test
  public void testGetSize() {
    assertEquals(2, new SourceValue(2).getSize());
  }

  @Test
  public void testEquals() {
    SourceValue nullValue = null;

    boolean equalsSame = new SourceValue(1).equals(new SourceValue(1));
    boolean equalsValueWithDifferentSource =
            new SourceValue(1).equals(new SourceValue(1, new InsnNode(Opcodes.NOP)));
    boolean equalsValueWithDifferentValue = new SourceValue(1).equals(new SourceValue(2));
    boolean equalsNull = new SourceValue(1).equals(nullValue);

    assertTrue(equalsSame);
    assertFalse(equalsValueWithDifferentSource);
    assertFalse(equalsValueWithDifferentValue);
    assertFalse(equalsNull);
  }

  @Test
  public void testHashcode() {
    assertEquals(0, new SourceValue(1).hashCode());
    assertNotEquals(0, new SourceValue(1, new InsnNode(Opcodes.NOP)).hashCode());
  }
}
