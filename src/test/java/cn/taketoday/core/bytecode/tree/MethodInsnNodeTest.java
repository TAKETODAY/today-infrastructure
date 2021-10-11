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
package cn.taketoday.core.bytecode.tree;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.Opcodes;

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
