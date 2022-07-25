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
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link AnalyzerAdapter}.
 *
 * @author Eric Bruneton
 */
public class AnalyzerAdapterTest extends AsmTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(
            () -> new AnalyzerAdapter("pkg/Class", Opcodes.ACC_PUBLIC, "name", "()V", null));
  }

  @Test
  public void testVisitFrame_emptyFrame() {
    AnalyzerAdapter analyzerAdapter =
            new AnalyzerAdapter("pkg/Class", Opcodes.ACC_PUBLIC, "name", "()V", null);

    Executable visitFrame = () -> analyzerAdapter.visitFrame(Opcodes.F_NEW, 0, null, 0, null);

    assertDoesNotThrow(visitFrame);
  }

  @Test
  public void testVisitFrame_invalidFrameType() {
    AnalyzerAdapter analyzerAdapter =
            new AnalyzerAdapter("pkg/Class", Opcodes.ACC_PUBLIC, "name", "()V", null);

    Executable visitFrame = () -> analyzerAdapter.visitFrame(Opcodes.F_FULL, 0, null, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals(
            "AnalyzerAdapter only accepts expanded frames (see ClassReader.EXPAND_FRAMES)",
            exception.getMessage());
  }

  /**
   * Tests that classes with additional frames inserted at each instruction, using the results of an
   * AnalyzerAdapter, can be instantiated and loaded. This makes sure the intermediate frames
   * computed by AnalyzerAdapter are correct, i.e. pass bytecode verification.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAllMethods_precompiledClass(
          final PrecompiledClass classParameter) throws Exception {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor classAnalyzerAdapter = new ClassAnalyzerAdapter(classWriter);

    Executable accept = () -> classReader.accept(classAnalyzerAdapter, ClassReader.EXPAND_FRAMES);

    // jdk3.AllInstructions and jdk3.LargeMethod contain unsupported jsr/ret instructions.
    if (classParameter == PrecompiledClass.JDK3_ALL_INSTRUCTIONS
            || classParameter == PrecompiledClass.JDK3_LARGE_METHOD) {
      Exception exception = assertThrows(IllegalArgumentException.class, accept);
      assertEquals("JSR/RET are not supported", exception.getMessage());
    }
    else {
      assertDoesNotThrow(accept);
      Executable newInstance = () -> new ClassFile(classWriter.toByteArray()).newInstance();
      if (classParameter.isNotCompatibleWithCurrentJdk()) {
        assertThrows(UnsupportedClassVersionError.class, newInstance);
      }
      else {
        assertDoesNotThrow(newInstance);
      }
    }
  }

  /**
   * A ClassVisitor that inserts intermediate frames before each instruction of each method, using
   * the types computed with an AnalyzerAdapter (in order to check that these intermediate frames
   * are correct).
   */
  static class ClassAnalyzerAdapter extends ClassVisitor {

    private String owner;

    ClassAnalyzerAdapter(final ClassVisitor classVisitor) {
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
      owner = name;
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      MethodVisitor methodVisitor =
              super.visitMethod(access, name, descriptor, signature, exceptions);
      AnalyzedFramesInserter inserter = new AnalyzedFramesInserter(methodVisitor);
      AnalyzerAdapter analyzerAdapter =
              new AnalyzerAdapter(owner, access, name, descriptor, inserter) {

                @Override
                public void visitMaxs(final int maxStack, final int maxLocals) {
                  // AnalyzerAdapter should correctly recompute maxLocals from scratch.
                  super.visitMaxs(maxStack, 0);
                }
              };
      inserter.setAnalyzerAdapter(analyzerAdapter);
      return analyzerAdapter;
    }
  }

  /**
   * Inserts intermediate frames before each instruction, using the types computed with an
   * AnalyzerAdapter.
   */
  static class AnalyzedFramesInserter extends MethodVisitor {

    private AnalyzerAdapter analyzerAdapter;
    private boolean hasOriginalFrame;

    AnalyzedFramesInserter(final MethodVisitor methodVisitor) {
      super(methodVisitor);
    }

    void setAnalyzerAdapter(final AnalyzerAdapter analyzerAdapter) {
      this.analyzerAdapter = analyzerAdapter;
    }

    @Override
    public void visitFrame(
            final int type,
            final int numLocal,
            final Object[] local,
            final int numStack,
            final Object[] stack) {
      super.visitFrame(type, numLocal, local, numStack, stack);
      hasOriginalFrame = true;
    }

    private void maybeInsertFrame() {
      // Don't insert a frame if we already have one for this instruction, from the original class.
      if (!hasOriginalFrame) {
        if (analyzerAdapter.locals != null && analyzerAdapter.stack != null) {
          ArrayList<Object> local = toFrameTypes(analyzerAdapter.locals);
          ArrayList<Object> stack = toFrameTypes(analyzerAdapter.stack);
          super.visitFrame(
                  Opcodes.F_NEW, local.size(), local.toArray(), stack.size(), stack.toArray());
        }
      }
      hasOriginalFrame = false;
    }

    /**
     * Converts local and stack types from AnalyzerAdapter to visitFrame format (long and double are
     * represented with one element in visitFrame, but with two elements in AnalyzerAdapter).
     */
    private ArrayList<Object> toFrameTypes(final List<Object> analyzerTypes) {
      ArrayList<Object> frameTypes = new ArrayList<>();
      for (int i = 0; i < analyzerTypes.size(); ++i) {
        if (i > 0
                && (analyzerTypes.get(i - 1) == Opcodes.LONG
                || analyzerTypes.get(i - 1) == Opcodes.DOUBLE)) {
          continue;
        }
        Object value = analyzerTypes.get(i);
        frameTypes.add(value);
      }
      return frameTypes;
    }

    @Override
    public void visitInsn(final int opcode) {
      maybeInsertFrame();
      super.visitInsn(opcode);
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
      maybeInsertFrame();
      super.visitIntInsn(opcode, operand);
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
      maybeInsertFrame();
      super.visitVarInsn(opcode, var);
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
      maybeInsertFrame();
      super.visitTypeInsn(opcode, type);
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
      maybeInsertFrame();
      super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
      maybeInsertFrame();
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
      maybeInsertFrame();
      super.visitInvokeDynamicInsn(
              name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
      maybeInsertFrame();
      super.visitJumpInsn(opcode, label);
    }

    @Override
    public void visitLdcInsn(final Object value) {
      maybeInsertFrame();
      super.visitLdcInsn(value);
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
      maybeInsertFrame();
      super.visitIincInsn(var, increment);
    }

    @Override
    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {
      maybeInsertFrame();
      super.visitTableSwitchInsn(min, max, dflt, labels);
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
      maybeInsertFrame();
      super.visitLookupSwitchInsn(dflt, keys, labels);
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
      maybeInsertFrame();
      super.visitMultiANewArrayInsn(descriptor, numDimensions);
    }
  }
}
