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
package cn.taketoday.bytecode.commons;

import org.junit.jupiter.api.Test;

import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.tree.ClassNode;

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
