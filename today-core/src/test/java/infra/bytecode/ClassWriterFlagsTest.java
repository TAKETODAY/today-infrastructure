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
package infra.bytecode;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tests for {@link ClassWriter}. Checks that {@link ClassWriter} can be configured to compute maxs
 * and frames for each method.
 *
 * @author Volodya Lombrozo.
 */
class ClassWriterFlagsTest {

  @Test
  void switchesModesThreeTimes() {
    final ClassWriter writer = new ClassWriter(0);
    final DummyClass clazz = new DummyClass(writer);
    final String computeMaxs = "computeMaxs";
    final String computeNothing = "computeNothing";
    final String computeFrames = "computeFrames";
    writer.setFlags(ClassWriter.COMPUTE_MAXS);
    clazz.withDummyMethod(computeMaxs);
    writer.setFlags(0);
    clazz.withDummyMethod(computeNothing);
    writer.setFlags(ClassWriter.COMPUTE_FRAMES);
    clazz.withDummyMethod(computeFrames);
    final CompiledClass compiled = clazz.compile();
    final Maxs maxs = compiled.maxs(computeMaxs);
    Assertions.assertEquals(3, maxs.stack, "Max stack is not 3");
    Assertions.assertEquals(1, maxs.locals, "Max locals is not 1");
    final Maxs nothing = compiled.maxs(computeNothing);
    Assertions.assertEquals(1, nothing.stack, "Max stack is not 1");
    Assertions.assertEquals(0, nothing.locals, "Max locals is not 0");
    final Maxs frames = compiled.maxs(computeFrames);
    Assertions.assertEquals(3, frames.stack, "Max stack is not 3");
    Assertions.assertEquals(1, frames.locals, "Max locals is not 1");
  }

  @Test
  void computesFramesAsUsual() {
    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    final String method = "frames";
    final Maxs maxs = new DummyClass(writer).withDummyMethod(method).compile().maxs(method);
    Assertions.assertEquals(3, maxs.stack, "Max stack is not 3");
    Assertions.assertEquals(1, maxs.locals, "Max locals is not 1");
  }

  @Test
  void computesMaxsAsUsual() {
    final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    final String method = "maxs";
    final Maxs maxs = new DummyClass(writer).withDummyMethod(method).compile().maxs(method);
    Assertions.assertEquals(3, maxs.stack, "Max stack is not 3");
    Assertions.assertEquals(1, maxs.locals, "Max locals is not 1");
  }

  @Test
  void computesNothingAsUsual() {
    final ClassWriter writer = new ClassWriter(0);
    final String method = "nothing";
    final Maxs maxs = new DummyClass(writer).withDummyMethod(method).compile().maxs(method);
    Assertions.assertEquals(1, maxs.stack, "Max stack is not 3");
    Assertions.assertEquals(0, maxs.locals, "Max locals is not 1");
  }

  private static final class CompiledClass {

    private final byte[] clazz;

    private CompiledClass(final byte[] clazz) {
      this.clazz = clazz;
    }

    /**
     * Get max stack and locals for a method.
     *
     * @param method Method name.
     * @return Max stack and locals.
     */
    Maxs maxs(final String method) {
      final AtomicReference<Maxs> maxs = new AtomicReference<>();
      new ClassReader(clazz)
              .accept(
                      new ClassVisitor() {
                        @Override
                        public MethodVisitor visitMethod(
                                final int access,
                                final String name,
                                final String descriptor,
                                final String signature,
                                final String[] exceptions) {
                          if (name.equals(method)) {
                            return new MethodVisitor() {
                              @Override
                              public void visitMaxs(final int stack, final int locals) {
                                maxs.set(new Maxs(stack, locals));
                              }
                            };
                          }
                          else {
                            return super.visitMethod(access, name, descriptor, signature, exceptions);
                          }
                        }
                      },
                      0);
      return maxs.get();
    }
  }

  private static final class DummyClass {

    /** Class writer to use in a test. */
    private final ClassWriter writer;

    /**
     * Constructor.
     *
     * @param writer Class writer to use in a test.
     */
    private DummyClass(final ClassWriter writer) {
      this.writer = writer;
      this.writer.visit(
              Opcodes.V1_7, Opcodes.ACC_PUBLIC, "SomeClass", null, "java/lang/Object", null);
    }

    CompiledClass compile() {
      writer.visitEnd();
      return new CompiledClass(writer.toByteArray());
    }

    DummyClass withDummyMethod(final String method) {
      final Label start = new Label();
      final MethodVisitor mvisitor =
              writer.visitMethod(Opcodes.ACC_PUBLIC, method, "()V", null, null);
      mvisitor.visitCode();
      mvisitor.visitLabel(start);
      mvisitor.visitInsn(Opcodes.LCONST_0);
      final Label label = new Label();
      mvisitor.visitJumpInsn(Opcodes.GOTO, label);
      mvisitor.visitLabel(label);
      mvisitor.visitFrame(Opcodes.F_NEW, 0, null, 1, new Object[] { Opcodes.LONG });
      mvisitor.visitInsn(Opcodes.ACONST_NULL);
      mvisitor.visitInsn(Opcodes.RETURN);
      mvisitor.visitMaxs(1, 0);
      mvisitor.visitEnd();
      return this;
    }
  }

  private static final class Maxs {

    final int stack;
    final int locals;

    private Maxs(final int stack, final int locals) {
      this.stack = stack;
      this.locals = locals;
    }
  }
}
