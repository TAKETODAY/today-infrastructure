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

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.Opcodes;

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
