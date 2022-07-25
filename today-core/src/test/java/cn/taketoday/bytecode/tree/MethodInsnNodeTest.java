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
package cn.taketoday.bytecode.tree;

import org.junit.jupiter.api.Test;

import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MethodInsnNode}.
 *
 * @author Eric Bruneton
 */
public class MethodInsnNodeTest extends AsmTest {

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedConstructor() {
    MethodInsnNode methodInsnNode1 =
            new MethodInsnNode(Opcodes.INVOKESTATIC, "owner", "name", "()I");
    MethodInsnNode methodInsnNode2 =
            new MethodInsnNode(Opcodes.INVOKEINTERFACE, "owner", "name", "()I");

    assertEquals(AbstractInsnNode.METHOD_INSN, methodInsnNode1.getType());
    assertEquals(AbstractInsnNode.METHOD_INSN, methodInsnNode2.getType());
    assertEquals(Opcodes.INVOKESTATIC, methodInsnNode1.getOpcode());
    assertEquals(Opcodes.INVOKEINTERFACE, methodInsnNode2.getOpcode());
    assertFalse(methodInsnNode1.itf);
    assertTrue(methodInsnNode2.itf);
  }

  @Test
  public void testConstrutor() {
    MethodInsnNode methodInsnNode =
            new MethodInsnNode(Opcodes.INVOKESTATIC, "owner", "name", "()I", false);

    assertEquals(AbstractInsnNode.METHOD_INSN, methodInsnNode.getType());
    assertEquals(Opcodes.INVOKESTATIC, methodInsnNode.getOpcode());
    assertEquals("owner", methodInsnNode.owner);
    assertEquals("name", methodInsnNode.name);
    assertEquals("()I", methodInsnNode.desc);
    assertFalse(methodInsnNode.itf);
  }

  @Test
  public void testSetOpcode() {
    MethodInsnNode methodInsnNode =
            new MethodInsnNode(Opcodes.INVOKESTATIC, "owner", "name", "()I", false);

    methodInsnNode.setOpcode(Opcodes.INVOKESPECIAL);

    assertEquals(Opcodes.INVOKESPECIAL, methodInsnNode.getOpcode());
  }
}
