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
package cn.taketoday.asm.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import cn.taketoday.asm.Attribute;
import cn.taketoday.asm.Handle;
import cn.taketoday.asm.Label;
import cn.taketoday.asm.Opcodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link Printer}.
 *
 * @author Eric Bruneton
 */
public class PrinterTest {

  private static final String UNSUPPORTED_OPERATION_MESSAGE = "Must be overridden";

  @Test
  public void testVisitModule_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitModule = () -> printer.visitModule(null, 0, null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitModule);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitNestHost_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitNestHost = () -> printer.visitNestHost(null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitNestHost);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitClassTypeAnnotation_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitClassTypeAnnotation =
            () -> printer.visitClassTypeAnnotation(0, null, null, false);

    Exception exception =
            assertThrows(UnsupportedOperationException.class, visitClassTypeAnnotation);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitNestMember_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitNestMember = () -> printer.visitNestMember(null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitNestMember);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitPermittedSubclass_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitPermittedSubclass = () -> printer.visitPermittedSubclass(null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitPermittedSubclass);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitMainClass_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitMainClass = () -> printer.visitMainClass(null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitMainClass);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitPackage_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitPackage = () -> printer.visitPackage(null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitPackage);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitRequire_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitRequire = () -> printer.visitRequire(null, 0, null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitRequire);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitExport_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitExport = () -> printer.visitExport(null, 0);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitExport);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitOpen_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitOpen = () -> printer.visitOpen(null, 0);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitOpen);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitUse_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitUse = () -> printer.visitUse(null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitUse);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitProvide_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitProvide = () -> printer.visitProvide(null);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitProvide);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitModuleEnd_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitModuleEnd = () -> printer.visitModuleEnd();

    Exception exception = assertThrows(UnsupportedOperationException.class, visitModuleEnd);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitFieldTypeAnnotation_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitFieldTypeAnnotation =
            () -> printer.visitFieldTypeAnnotation(0, null, null, false);

    Exception exception =
            assertThrows(UnsupportedOperationException.class, visitFieldTypeAnnotation);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitParameter_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitParameter = () -> printer.visitParameter(null, 0);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitParameter);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitMethodTypeAnnotation_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitMethodTypeAnnotation =
            () -> printer.visitMethodTypeAnnotation(0, null, null, false);

    Exception exception =
            assertThrows(UnsupportedOperationException.class, visitMethodTypeAnnotation);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitAnnotableParameterCount_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitAnnotableParameterCount = () -> printer.visitAnnotableParameterCount(0, false);

    Exception exception =
            assertThrows(UnsupportedOperationException.class, visitAnnotableParameterCount);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_asm4_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitMethodInsn =
            () -> printer.visitMethodInsn(Opcodes.INVOKESPECIAL, "owner", "name", "()V", false);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitMethodInsn);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitMethodInsn =
            () -> printer.visitMethodInsn(Opcodes.INVOKESPECIAL, "owner", "name", "()V", false);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitMethodInsn);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_ifItf_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitMethodInsn =
            () -> printer.visitMethodInsn(Opcodes.INVOKESPECIAL, "owner", "name", "()V", true);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitMethodInsn);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitInsnAnnotation_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitInsnAnnotation = () -> printer.visitInsnAnnotation(0, null, null, false);

    Exception exception = assertThrows(UnsupportedOperationException.class, visitInsnAnnotation);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitTryCatchAnnotation_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitTryCatchAnnotation =
            () -> printer.visitTryCatchAnnotation(0, null, null, false);

    Exception exception =
            assertThrows(UnsupportedOperationException.class, visitTryCatchAnnotation);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  @Test
  public void testVisitLocalVariableAnnotation_unsupportedByDefault() {
    Printer printer = new EmptyPrinter();

    Executable visitLocalVariableAnnotation =
            () -> printer.visitLocalVariableAnnotation(0, null, null, null, null, null, false);

    Exception exception =
            assertThrows(UnsupportedOperationException.class, visitLocalVariableAnnotation);
    assertEquals(UNSUPPORTED_OPERATION_MESSAGE, exception.getMessage());
  }

  static class EmptyPrinter extends Printer {

    EmptyPrinter() {
    }

    @Override
    public void visit(
            final int version,
            final int access,
            final String name,
            final String signature,
            final String superName,
            final String[] interfaces) {
      // Do nothing.
    }

    @Override
    public void visitSource(final String source, final String debug) {
      // Do nothing.
    }

    @Override
    public void visitOuterClass(final String owner, final String name, final String descriptor) {
      // Do nothing.
    }

    @Override
    public Printer visitClassAnnotation(final String descriptor, final boolean visible) {
      return null;
    }

    @Override
    public void visitClassAttribute(final Attribute attribute) {
      // Do nothing.
    }

    @Override
    public void visitInnerClass(
            final String name, final String outerName, final String innerName, final int access) {
      // Do nothing.
    }

    @Override
    public Printer visitField(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final Object value) {
      return null;
    }

    @Override
    public Printer visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      return null;
    }

    @Override
    public void visitClassEnd() {
      // Do nothing.
    }

    // DontCheck(OverloadMethodsDeclarationOrder): overloads are semantically different.
    @Override
    public void visit(final String name, final Object value) {
      // Do nothing.
    }

    @Override
    public void visitEnum(final String name, final String descriptor, final String value) {
      // Do nothing.
    }

    @Override
    public Printer visitAnnotation(final String name, final String descriptor) {
      return null;
    }

    @Override
    public Printer visitArray(final String name) {
      return null;
    }

    @Override
    public void visitAnnotationEnd() {
      // Do nothing.
    }

    @Override
    public Printer visitFieldAnnotation(final String descriptor, final boolean visible) {
      return null;
    }

    @Override
    public void visitFieldAttribute(final Attribute attribute) {
      // Do nothing.
    }

    @Override
    public void visitFieldEnd() {
      // Do nothing.
    }

    @Override
    public Printer visitAnnotationDefault() {
      return null;
    }

    @Override
    public Printer visitMethodAnnotation(final String descriptor, final boolean visible) {
      return null;
    }

    @Override
    public Printer visitParameterAnnotation(
            final int parameter, final String descriptor, final boolean visible) {
      return null;
    }

    @Override
    public void visitMethodAttribute(final Attribute attribute) {
      // Do nothing.
    }

    @Override
    public void visitCode() {
      // Do nothing.
    }

    @Override
    public void visitFrame(
            final int type,
            final int numLocal,
            final Object[] local,
            final int numStack,
            final Object[] stack) {
      // Do nothing.
    }

    @Override
    public void visitInsn(final int opcode) {
      // Do nothing.
    }

    @Override
    public void visitIntInsn(final int opcode, final int operand) {
      // Do nothing.
    }

    @Override
    public void visitVarInsn(final int opcode, final int var) {
      // Do nothing.
    }

    @Override
    public void visitTypeInsn(final int opcode, final String type) {
      // Do nothing.
    }

    @Override
    public void visitFieldInsn(
            final int opcode, final String owner, final String name, final String descriptor) {
      // Do nothing.
    }

    @Override
    public void visitInvokeDynamicInsn(
            final String name,
            final String descriptor,
            final Handle bootstrapMethodHandle,
            final Object... bootstrapMethodArguments) {
      // Do nothing.
    }

    @Override
    public void visitJumpInsn(final int opcode, final Label label) {
      // Do nothing.
    }

    @Override
    public void visitLabel(final Label label) {
      // Do nothing.
    }

    @Override
    public void visitLdcInsn(final Object value) {
      // Do nothing.
    }

    @Override
    public void visitIincInsn(final int var, final int increment) {
      // Do nothing.
    }

    @Override
    public void visitTableSwitchInsn(
            final int min, final int max, final Label dflt, final Label... labels) {
      // Do nothing.
    }

    @Override
    public void visitLookupSwitchInsn(final Label dflt, final int[] keys, final Label[] labels) {
      // Do nothing.
    }

    @Override
    public void visitMultiANewArrayInsn(final String descriptor, final int numDimensions) {
      // Do nothing.
    }

    @Override
    public void visitTryCatchBlock(
            final Label start, final Label end, final Label handler, final String type) {
      // Do nothing.
    }

    @Override
    public void visitLocalVariable(
            final String name,
            final String descriptor,
            final String signature,
            final Label start,
            final Label end,
            final int index) {
      // Do nothing.
    }

    @Override
    public void visitLineNumber(final int line, final Label start) {
      // Do nothing.
    }

    @Override
    public void visitMaxs(final int maxStack, final int maxLocals) {
      // Do nothing.
    }

    @Override
    public void visitMethodEnd() {
      // Do nothing.
    }
  }
}
