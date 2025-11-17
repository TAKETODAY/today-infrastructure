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
package infra.bytecode.tree;

import org.junit.jupiter.api.Test;

import infra.bytecode.AsmTest;
import infra.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link FieldInsnNode}.
 *
 * @author Eric Bruneton
 */
public class FieldInsnNodeTests extends AsmTest {

  @Test
  public void testConstructor() {
    FieldInsnNode fieldInsnNode = new FieldInsnNode(Opcodes.GETSTATIC, "owner", "name", "I");

    assertEquals(AbstractInsnNode.FIELD_INSN, fieldInsnNode.getType());
    assertEquals(Opcodes.GETSTATIC, fieldInsnNode.getOpcode());
    assertEquals("owner", fieldInsnNode.owner);
    assertEquals("name", fieldInsnNode.name);
    assertEquals("I", fieldInsnNode.desc);
  }

  @Test
  public void testSetOpcode() {
    FieldInsnNode fieldInsnNode = new FieldInsnNode(Opcodes.GETSTATIC, "owner", "name", "I");

    fieldInsnNode.setOpcode(Opcodes.PUTSTATIC);

    assertEquals(Opcodes.PUTSTATIC, fieldInsnNode.getOpcode());
  }
}
