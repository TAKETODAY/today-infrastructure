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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LdcInsnNode}.
 *
 * @author Eric Bruneton
 */
public class LdcInsnNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    LdcInsnNode ldcInsnNode = new LdcInsnNode("s");

    assertEquals(AbstractInsnNode.LDC_INSN, ldcInsnNode.getType());
    assertEquals("s", ldcInsnNode.cst);
  }
}
