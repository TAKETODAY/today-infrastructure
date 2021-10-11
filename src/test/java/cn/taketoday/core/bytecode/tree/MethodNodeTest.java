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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.Attribute;
import cn.taketoday.core.bytecode.ClassFile;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassWriter;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MethodNode}.
 *
 * @author Eric Bruneton
 */
public class MethodNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    MethodNode methodNode = new MethodNode(123, "method", "()V", null, null);

    assertEquals(123, methodNode.access);
    assertEquals("method", methodNode.name);
    assertEquals("()V", methodNode.desc);
  }

  /** Tests that an uninitialized MethodNode can receive any visit method call. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testVisitAndAccept_withUninitializedMethodNode(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassNode classNode =
            new ClassNode() {

              @Override
              public MethodVisitor visitMethod(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final String[] exceptions) {
                MethodNode method = new MethodNode();
                method.access = access;
                method.name = name;
                method.desc = descriptor;
                method.signature = signature;
                method.exceptions = exceptions;
                methods.add(method);
                return method;
              }
            };
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classNode, new Attribute[] { new Comment(), new CodeComment() }, 0);
    classNode.accept(classWriter);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  @Test
  public void testClone() {
    MethodNode methodNode = new MethodNode();
    methodNode.visitCode();
    methodNode.visitLabel(new Label());
    methodNode.visitInsn(Opcodes.NOP);
    methodNode.visitLabel(new Label());
    methodNode.visitEnd();

    MethodNode cloneMethodNode = new MethodNode();
    methodNode.accept(cloneMethodNode);
    methodNode.accept(cloneMethodNode);

    assertEquals(6, cloneMethodNode.instructions.size());
  }
}
