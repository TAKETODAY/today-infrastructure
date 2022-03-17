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
package cn.taketoday.core.bytecode.commons;

import org.junit.jupiter.api.Test;

import cn.taketoday.core.bytecode.ClassFile;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.ClassWriter;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link StaticInitMerger}.
 *
 * @author Eric Bruneton
 */
public class StaticInitMergerTest {

  @Test
  public void testAllMethods_multipleStaticInitBlocks() throws Exception {
    ClassNode classNode = newClassWithStaticInitBlocks(5);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    ClassVisitor staticInitMerger = new StaticInitMerger("$clinit$", classWriter);

    classNode.accept(staticInitMerger);

    Object instance = new ClassFile(classWriter.toByteArray()).newInstance();
    assertEquals(5, instance.getClass().getField("counter").getInt(instance));
  }

  private static ClassNode newClassWithStaticInitBlocks(final int numStaticInitBlocks) {
    ClassNode classNode = new ClassNode();
    classNode.visit(Opcodes.V1_1, Opcodes.ACC_PUBLIC, "A", null, "java/lang/Object", null);
    classNode.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC, "counter", "I", null, null);
    for (int i = 0; i < numStaticInitBlocks; ++i) {
      MethodVisitor methodVisitor =
              classNode.visitMethod(Opcodes.ACC_PUBLIC, "<clinit>", "()V", null, null);
      methodVisitor.visitFieldInsn(Opcodes.GETSTATIC, "A", "counter", "I");
      methodVisitor.visitInsn(Opcodes.ICONST_1);
      methodVisitor.visitInsn(Opcodes.IADD);
      methodVisitor.visitFieldInsn(Opcodes.PUTSTATIC, "A", "counter", "I");
      methodVisitor.visitInsn(Opcodes.RETURN);
      methodVisitor.visitMaxs(0, 0);
    }
    MethodVisitor methodVisitor =
            classNode.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
    methodVisitor.visitMethodInsn(
            Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    methodVisitor.visitInsn(Opcodes.RETURN);
    methodVisitor.visitMaxs(0, 0);
    classNode.visitEnd();
    return classNode;
  }
}
