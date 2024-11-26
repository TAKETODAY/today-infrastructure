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
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;

import infra.bytecode.AsmTest;
import infra.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link FrameNode}.
 *
 * @author Eric Bruneton
 */
public class FrameNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    Object[] locals = new Object[] { "l" };
    Object[] stack = new Object[] { "s", "t" };

    FrameNode frameNode = new FrameNode(Opcodes.F_FULL, 1, locals, 2, stack);

    assertEquals(AbstractInsnNode.FRAME, frameNode.getType());
    assertEquals(Opcodes.F_FULL, frameNode.type);
    assertEquals(Arrays.asList(locals), frameNode.local);
    assertEquals(Arrays.asList(stack), frameNode.stack);
  }

  @Test
  public void testConstructor_illegalArgument() {
    Executable constructor = () -> new FrameNode(Integer.MAX_VALUE, 0, null, 0, null);

    assertThrows(IllegalArgumentException.class, constructor);
  }
}
