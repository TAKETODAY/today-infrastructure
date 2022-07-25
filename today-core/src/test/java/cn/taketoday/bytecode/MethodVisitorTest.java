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
package cn.taketoday.bytecode;

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
