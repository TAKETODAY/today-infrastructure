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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Map;

import cn.taketoday.bytecode.AnnotationVisitor;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.FieldVisitor;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.ModuleVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.RecordComponentVisitor;
import cn.taketoday.bytecode.TypePath;
import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.ClassFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ClassNode}.
 *
 * @author Eric Bruneton
 */
public class ClassNodeTest extends AsmTest {

  @Test
  public void testConstructor() {
    ClassNode classNode = new ClassNode();

    assertNull(classNode.interfaces);
    assertNull(classNode.innerClasses);
    assertTrue(classNode.fields.isEmpty());
    assertTrue(classNode.methods.isEmpty());
  }

  /** Tests that classes are unchanged with a ClassReader->ClassNode->ClassWriter transform. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testVisitAndAccept(final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassNode classNode = new ClassNode() { };
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classNode, attributes(), 0);
    classNode.accept(classWriter);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /**
   * Tests that classes are unchanged with a ClassReader->ClassNode->ClassWriter transform, when all
   * instructions are cloned.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testVisitAndAccept_cloneInstructions(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassNode classNode = new ClassNode() { };
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classNode, attributes(), 0);
    for (MethodNode methodNode : classNode.methods) {
      cloneInstructions(methodNode);
    }
    classNode.accept(classWriter);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /** Tests that ClassNode accepts visitors that remove class elements. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testVisitAndAccept_removeMembersVisitor(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassNode classNode = new ClassNode() { };
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classNode, attributes(), 0);
    classNode.accept(new RemoveMembersClassVisitor(classWriter));

    ClassWriter expectedClassWriter = new ClassWriter(0);
    classReader.accept(new RemoveMembersClassVisitor(expectedClassWriter), 0);
    assertEquals(
            new ClassFile(expectedClassWriter.toByteArray()), new ClassFile(classWriter.toByteArray()));
  }

  private static Attribute[] attributes() {
    return new Attribute[] { new Comment(), new CodeComment() };
  }

  private static void cloneInstructions(final MethodNode methodNode) {
    Map<LabelNode, LabelNode> labelCloneMap = new HashMap<LabelNode, LabelNode>() {
      @Override
      public LabelNode get(final Object o) {
        return (LabelNode) o;
      }
    };

    for (AbstractInsnNode insn : methodNode.instructions) {
      methodNode.instructions.set(insn, insn.clone(labelCloneMap));
    }
  }

  private static class RemoveMembersClassVisitor extends ClassVisitor {

    RemoveMembersClassVisitor(final ClassVisitor classVisitor) {
      super(classVisitor);
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
      super.visit(version, access & ~Opcodes.ACC_RECORD, name, signature, superName, interfaces);
    }

    @Override
    public ModuleVisitor visitModule(final String name, final int access, final String version) {
      return null;
    }

    @Override
    public void visitNestHost(final String nestHost) { }

    @Override
    public void visitNestMember(final String nestMember) { }

    @Override
    public void visitPermittedSubclass(final String permittedSubclass) { }

    @Override
    public AnnotationVisitor visitAnnotation(final String descriptor, final boolean visible) {
      return null;
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(
            final int typeRef,
            final TypePath typePath,
            final String descriptor,
            final boolean visible) {
      return null;
    }

    @Override
    public void visitAttribute(final Attribute attribute) { }

    @Override
    public RecordComponentVisitor visitRecordComponent(
            final String name, final String descriptor, final String signature) {
      return null;
    }

    @Override
    public FieldVisitor visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
      return null;
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      return null;
    }
  }
}
