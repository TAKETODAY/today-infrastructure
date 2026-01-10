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

// Modifications Copyright 2017 - 2026 the TODAY authors.
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
class ClassWriterFlagsTests {

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
