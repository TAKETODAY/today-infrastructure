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
package cn.taketoday.asm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MethodVisitor}.
 *
 * @author Eric Bruneton
 */
public class MethodVisitorTest extends AsmTest {

  @Test
  public void testVisitFrame_consecutiveFrames_sameFrame() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "C", null, "D", null);
    MethodVisitor methodVisitor =
            classWriter.visitMethod(Opcodes.ACC_STATIC, "m", "()V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

    Executable visitFrame = () -> methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

    assertDoesNotThrow(visitFrame);
  }

  @Test
  public void testVisitFrame_consecutiveFrames() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC, "C", null, "D", null);
    MethodVisitor methodVisitor =
            classWriter.visitMethod(Opcodes.ACC_STATIC, "m", "()V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

    Executable visitFrame =
            () ->
                    methodVisitor.visitFrame(Opcodes.F_APPEND, 1, new Object[] { Opcodes.INTEGER }, 0, null);

    assertThrows(IllegalStateException.class, visitFrame);
  }

  @Test
  public void testVisitFrame_compressedFrameWithV1_5class() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "C", null, "D", null);
    MethodVisitor methodVisitor =
            new ClassWriter(0).visitMethod(Opcodes.ACC_STATIC, "m", "()V", null, null);
    methodVisitor.visitCode();

    Executable visitFrame = () -> methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertTrue(exception.getMessage().contains("versions V1_5 or less must use F_NEW frames."));
  }

  /** Tests the ASM4 visitMethodInsn on an ASM4 visitor. */
  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedVisitMethodInsn_asm4Visitor() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new MethodVisitor4(logMethodVisitor);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V");

    assertEquals("LogMethodVisitor:m()V;", log.toString());
  }

  /** Tests the ASM4 visitMethodInsn on an ASM5 visitor. */
  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedVisitMethodInsn_asm5Visitor() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new MethodVisitor5(logMethodVisitor);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V");

    assertEquals("LogMethodVisitor:m()V;", log.toString());
  }

  /** Tests the ASM4 visitMethodInsn on an ASM5 visitor which overrides the ASM5 method. */
  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedVisitMethodInsn_overridenAsm5Visitor() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new MethodVisitor5Override(logMethodVisitor, log);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V");

    assertEquals("LogMethodVisitor:m5()V;MethodVisitor5:m()V;", log.toString());
  }

  /**
   * Tests the ASM4 visitMethodInsn on an ASM4 visitor which overrides this method, and is a
   * subclass of an ASM subclass of MethodVisitor.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedVisitMethodInsn_userTraceMethodVisitor4() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new UserTraceMethodVisitor4(logMethodVisitor, log);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V");

    assertEquals(
            "LogMethodVisitor:m()V;TraceMethodVisitor:m()V;UserTraceMethodVisitor4:m()V;",
            log.toString());
  }

  /**
   * Tests the ASM4 visitMethodInsn on an ASM5 visitor which overrides the ASM5 method, and is a
   * subclass of an ASM subclass of MethodVisitor.
   */
  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedVisitMethodInsn_userTraceMethodVisitor5() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new UserTraceMethodVisitor5(logMethodVisitor, log);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V");

    assertEquals(
            "LogMethodVisitor:m()V;TraceMethodVisitor:m()V;UserTraceMethodVisitor5:m()V;",
            log.toString());
  }

  /** Tests the ASM5 visitMethodInsn on an ASM4 visitor, with isInterface set to false. */
  @Test
  public void testVisitMethodInsn_asm4Visitor_isNotInterface() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new MethodVisitor4(logMethodVisitor);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V", false);

    assertEquals("LogMethodVisitor:m()V;", log.toString());
  }

  /** Tests the ASM5 visitMethodInsn on an ASM5 visitor. */
  @Test
  public void testVisitMethodInsn_asm5Visitor() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new MethodVisitor5(logMethodVisitor);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V", false);

    assertEquals("LogMethodVisitor:m()V;", log.toString());
  }

  /** Tests the ASM5 visitMethodInsn on an ASM5 visitor which overrides this method. */
  @Test
  public void testVisitMethodInsn_overridenAsm5Visitor() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new MethodVisitor5Override(logMethodVisitor, log);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V", false);

    assertEquals("LogMethodVisitor:m5()V;MethodVisitor5:m()V;", log.toString());
  }

  /**
   * Tests the ASM5 visitMethodInsn on an ASM4 visitor which overrides this method, and is a
   * subclass of an ASM subclass of MethodVisitor.
   */
  @Test
  public void testVisitMethodInsn_userTraceMethodVisitor4() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new UserTraceMethodVisitor4(logMethodVisitor, log);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V");

    assertEquals(
            "LogMethodVisitor:m()V;TraceMethodVisitor:m()V;UserTraceMethodVisitor4:m()V;",
            log.toString());
  }

  /**
   * Tests the ASM5 visitMethodInsn on an ASM5 visitor which overrides this method, and is a
   * subclass of an ASM subclass of MethodVisitor.
   */
  @Test
  public void testVisitMethodInsn_userTraceMethodVisitor5() {
    StringWriter log = new StringWriter();
    LogMethodVisitor logMethodVisitor = new LogMethodVisitor(log);
    MethodVisitor methodVisitor = new UserTraceMethodVisitor5(logMethodVisitor, log);

    methodVisitor.visitMethodInsn(0, "C", "m", "()V", false);

    assertEquals(
            "LogMethodVisitor:m()V;TraceMethodVisitor:m()V;UserTraceMethodVisitor5:m()V;",
            log.toString());
  }

  /** An ASM4 {@link MethodVisitor} which does not override the ASM4 visitMethodInsn method. */
  private static class MethodVisitor4 extends MethodVisitor {
    MethodVisitor4(final MethodVisitor methodVisitor) {
      super(methodVisitor);
    }
  }

  /** An ASM5 {@link MethodVisitor} which does not override the ASM5 visitMethodInsn method. */
  private static class MethodVisitor5 extends MethodVisitor {
    MethodVisitor5(final MethodVisitor methodVisitor) {
      super(methodVisitor);
    }
  }

  /** An ASM5 {@link MethodVisitor} which overrides the ASM5 visitMethodInsn method. */
  private static class MethodVisitor5Override extends MethodVisitor {

    private final StringWriter log;

    MethodVisitor5Override(final MethodVisitor methodVisitor, final StringWriter log) {
      super(methodVisitor);
      this.log = log;
    }

    @Override
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
      super.visitMethodInsn(opcode, owner, name + "5", descriptor, isInterface);
      log.append("MethodVisitor5:" + name + descriptor + ";");
    }
  }

  /**
   * An ASM-like {@link MethodVisitor} subclass, which overrides the ASM5 visitMethodInsn method,
   * but can be used with any API version.
   */
  private static class TraceMethodVisitor extends MethodVisitor {

    protected final StringWriter log;

    TraceMethodVisitor(final MethodVisitor methodVisitor, final StringWriter log) {
      super(methodVisitor);
      this.log = log;
    }

    @Override
    public void visitMethodInsn(
            final int opcodeAndSource,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
      super.visitMethodInsn(opcodeAndSource, owner, name, descriptor, isInterface);
      log.append("TraceMethodVisitor:" + name + descriptor + ";");
    }
  }

  /** A user subclass of {@link TraceMethodVisitor}, implemented for ASM4. */
  private static class UserTraceMethodVisitor4 extends TraceMethodVisitor {

    UserTraceMethodVisitor4(final MethodVisitor methodVisitor, final StringWriter log) {
      super(methodVisitor, log);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void visitMethodInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
      super.visitMethodInsn(opcode, owner, name, descriptor);
      log.append("UserTraceMethodVisitor4:" + name + descriptor + ";");
    }
  }

  /** A user subclass of {@link TraceMethodVisitor}, implemented for ASM5. */
  private static class UserTraceMethodVisitor5 extends TraceMethodVisitor {

    UserTraceMethodVisitor5(final MethodVisitor methodVisitor, final StringWriter log) {
      super(methodVisitor, log);
    }

    @Override
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
      log.append("UserTraceMethodVisitor5:" + name + descriptor + ";");
    }
  }

  /** A {@link MethodVisitor} that logs the calls to its visitMethodInsn method. */
  private static class LogMethodVisitor extends MethodVisitor {

    private final StringWriter log;

    LogMethodVisitor(final StringWriter log) {
      this.log = log;
    }

    @Override
    public void visitMethodInsn(
            final int opcode,
            final String owner,
            final String name,
            final String descriptor,
            final boolean isInterface) {
      log.append("LogMethodVisitor:" + name + descriptor + ";");
    }
  }
}
