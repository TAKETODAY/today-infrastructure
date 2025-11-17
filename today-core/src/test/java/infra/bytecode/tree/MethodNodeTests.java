/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import infra.bytecode.AsmTest;
import infra.bytecode.Attribute;
import infra.bytecode.ClassFile;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassWriter;
import infra.bytecode.Label;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link MethodNode}.
 *
 * @author Eric Bruneton
 */
public class MethodNodeTests extends AsmTest {

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

    Assertions.assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
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
