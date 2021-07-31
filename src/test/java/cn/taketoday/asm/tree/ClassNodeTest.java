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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.taketoday.asm.AnnotationVisitor;
import cn.taketoday.asm.AsmTest;
import cn.taketoday.asm.Attribute;
import cn.taketoday.asm.ClassFile;
import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.ClassWriter;
import cn.taketoday.asm.FieldVisitor;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.ModuleVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.RecordComponentVisitor;
import cn.taketoday.asm.TypePath;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    assertTrue(classNode.interfaces.isEmpty());
    assertTrue(classNode.innerClasses.isEmpty());
    assertTrue(classNode.fields.isEmpty());
    assertTrue(classNode.methods.isEmpty());
  }

  /** Tests that classes are unchanged with a ClassReader->ClassNode->ClassWriter transform. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testVisitAndAccept(final PrecompiledClass classParameter, final Api apiParameter) {
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
          final PrecompiledClass classParameter, final Api apiParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassNode classNode = new ClassNode() { };
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(classNode, attributes(), 0);
    for (MethodNode methodNode : classNode.methods) {
      cloneInstructions(methodNode);
    }
    classNode.accept(classWriter);

    Assertions.assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /** Tests that ClassNode accepts visitors that remove class elements. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testVisitAndAccept_removeMembersVisitor(
          final PrecompiledClass classParameter, final Api apiParameter) {
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

  @SuppressWarnings("serial")
  private static void cloneInstructions(final MethodNode methodNode) {
    Map<LabelNode, LabelNode> labelCloneMap =
            new HashMap<LabelNode, LabelNode>() {
              @Override
              public LabelNode get(final Object o) {
                return (LabelNode) o;
              }
            };
    Iterator<AbstractInsnNode> insnIterator = methodNode.instructions.iterator();
    while (insnIterator.hasNext()) {
      AbstractInsnNode insn = insnIterator.next();
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
