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
package infra.bytecode.tree;

import org.junit.jupiter.api.Test;

import infra.bytecode.AsmTest;
import infra.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link InsnNode}.
 *
 * @author Eric Bruneton
 */
public class InsnNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    InsnNode insnNode = new InsnNode(Opcodes.ACONST_NULL);

    assertEquals(AbstractInsnNode.INSN, insnNode.getType());
    assertEquals(Opcodes.ACONST_NULL, insnNode.getOpcode());
  }
}
