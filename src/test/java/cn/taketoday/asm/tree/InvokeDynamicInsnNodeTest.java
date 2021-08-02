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
package cn.taketoday.asm.tree;

import org.junit.jupiter.api.Test;

import cn.taketoday.asm.Handle;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.AsmTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link InvokeDynamicInsnNode}.
 *
 * @author Eric Bruneton
 */
public class InvokeDynamicInsnNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    Handle handle = new Handle(Opcodes.H_INVOKESTATIC, "owner", "name", "()V", false);
    Object[] bootstrapMethodArguments = new Object[] { "s" };

    InvokeDynamicInsnNode invokeDynamicInsnNode =
            new InvokeDynamicInsnNode("name", "()V", handle, bootstrapMethodArguments);

    assertEquals(Opcodes.INVOKEDYNAMIC, invokeDynamicInsnNode.getOpcode());
    assertEquals(AbstractInsnNode.INVOKE_DYNAMIC_INSN, invokeDynamicInsnNode.getType());
    assertEquals("name", invokeDynamicInsnNode.name);
    assertEquals("()V", invokeDynamicInsnNode.desc);
    assertEquals(handle, invokeDynamicInsnNode.bsm);
    assertEquals(bootstrapMethodArguments, invokeDynamicInsnNode.bsmArgs);
  }
}
