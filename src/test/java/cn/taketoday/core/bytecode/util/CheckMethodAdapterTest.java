/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.HashMap;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.Handle;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.MethodVisitor;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.TypeReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CheckMethodAdapter}.
 *
 * @author Eric Bruneton
 */
public class CheckMethodAdapterTest extends AsmTest implements Opcodes {

  private final CheckMethodAdapter checkMethodAdapter = new CheckMethodAdapter(null);

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new CheckMethodAdapter(null));
  }

  @Test
  public void testVisitTypeAnnotation_illegalTypeRef() {
    Executable visitTypeAnnotation =
            () -> checkMethodAdapter.visitTypeAnnotation(0xFFFFFFFF, null, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeAnnotation);
    assertEquals("Invalid type reference sort 0xff", exception.getMessage());
  }

  @Test
  public void testVisitParameterAnnotation_invisibleAnnotation_illegalParameterIndex() {
    checkMethodAdapter.visitAnnotableParameterCount(1, false);

    Executable visitParameterAnnotation =
            () -> checkMethodAdapter.visitParameterAnnotation(1, "LA;", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitParameterAnnotation);
    assertEquals("Invalid parameter index", exception.getMessage());
  }

  @Test
  public void testVisitParameterAnnotation_visibleAnnotation_illegalParameterIndex() {
    checkMethodAdapter.visitAnnotableParameterCount(2, true);

    Executable visitParameterAnnotation =
            () -> checkMethodAdapter.visitParameterAnnotation(2, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitParameterAnnotation);
    assertEquals("Invalid parameter index", exception.getMessage());
  }

  @Test
  public void testVisitParameterAnnotation_illegalDescriptor() {
    Executable visitParameterAnnotation =
            () -> checkMethodAdapter.visitParameterAnnotation(0, "'", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitParameterAnnotation);
    assertEquals("Invalid descriptor: '", exception.getMessage());
  }

  @Test
  public void testVisitAttribute_afterEnd() {
    checkMethodAdapter.visitEnd();

    Executable visitAttribute = () -> checkMethodAdapter.visitAttribute(new Comment());

    Exception exception = assertThrows(IllegalStateException.class, visitAttribute);
    assertEquals("Cannot visit elements after visitEnd has been called.", exception.getMessage());
  }

  @Test
  public void testVisitAttribute_nullAttribute() {
    Executable visitAttribute = () -> checkMethodAdapter.visitAttribute(null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitAttribute);
    assertEquals("Invalid attribute (must not be null)", exception.getMessage());
  }

  @Test
  public void testVisitCode_abstractMethod() {
    CheckMethodAdapter checkAbstractMethodAdapter =
            new CheckMethodAdapter(Opcodes.ACC_ABSTRACT, "m", "()V", null, new HashMap<>());

    Executable visitCode = () -> checkAbstractMethodAdapter.visitCode();

    Exception exception = assertThrows(UnsupportedOperationException.class, visitCode);
    assertEquals("Abstract methods cannot have code", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalFrameType() {
    checkMethodAdapter.visitCode();

    Executable visitFrame = () -> checkMethodAdapter.visitFrame(123, 0, null, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Invalid frame type 123", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalLocalCount() {
    checkMethodAdapter.visitCode();

    Executable visitFrame =
            () -> checkMethodAdapter.visitFrame(F_SAME, 1, new Object[] { INTEGER }, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Invalid numLocal=1 for frame type 3", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalStackCount() {
    checkMethodAdapter.visitCode();

    Executable visitFrame =
            () -> checkMethodAdapter.visitFrame(F_SAME, 0, null, 1, new Object[] { INTEGER });

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Invalid numStack=1 for frame type 3", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalLocalArray() {
    checkMethodAdapter.visitCode();

    Executable visitFrame =
            () -> checkMethodAdapter.visitFrame(F_APPEND, 1, new Object[0], 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Array local[] is shorter than numLocal", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalStackArray() {
    checkMethodAdapter.visitCode();

    Executable visitFrame = () -> checkMethodAdapter.visitFrame(F_SAME1, 0, null, 1, new Object[0]);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Array stack[] is shorter than numStack", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitFrame =
            () -> checkMethodAdapter.visitFrame(F_FULL, 1, new Object[] { "LC;" }, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals(
            "Invalid Invalid stack frame value (must be an internal class name): LC;",
            exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalPrimitiveType() {
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitInsn(NOP);
    Integer invalidFrameValue = 0; // NOPMD(IntegerInstantiation): needed to build an invalid value.

    Executable visitFrame =
            () -> checkMethodAdapter.visitFrame(F_FULL, 1, new Object[] { invalidFrameValue }, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Invalid stack frame value: 0", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalValueClass() {
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitInsn(NOP);

    Executable visitFrame =
            () -> checkMethodAdapter.visitFrame(F_FULL, 1, new Object[] { 0.0f }, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Invalid stack frame value: 0.0", exception.getMessage());
  }

  @Test
  public void testVisitFrame_illegalMixedFrameTypes() {
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitFrame(F_NEW, 0, null, 0, null);
    checkMethodAdapter.visitInsn(NOP);

    Executable visitFrame = () -> checkMethodAdapter.visitFrame(F_FULL, 0, null, 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFrame);
    assertEquals("Expanded and compressed frames must not be mixed.", exception.getMessage());
  }

  @Test
  public void testVisitInsn_beforeStart() {
    Executable visitInsn = () -> checkMethodAdapter.visitInsn(NOP);

    Exception exception = assertThrows(IllegalStateException.class, visitInsn);
    assertEquals(
            "Cannot visit instructions before visitCode has been called.", exception.getMessage());
  }

  @Test
  public void testVisitInsn_IllegalInsnVisitAfterEnd() {
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitMaxs(0, 0);

    Executable visitInsn = () -> checkMethodAdapter.visitInsn(NOP);

    Exception exception = assertThrows(IllegalStateException.class, visitInsn);
    assertEquals(
            "Cannot visit instructions after visitMaxs has been called.", exception.getMessage());
  }

  @Test
  public void testVisitIntInsn_illegalOpcode() {
    checkMethodAdapter.visitCode();

    Executable visitIntInsn = () -> checkMethodAdapter.visitIntInsn(-1, 0);

    Exception exception = assertThrows(IllegalArgumentException.class, visitIntInsn);
    assertEquals("Invalid opcode: -1", exception.getMessage());
  }

  @Test
  public void testVisitIntInsn_illegalOperand() {
    checkMethodAdapter.visitCode();

    Executable visitIntInsn = () -> checkMethodAdapter.visitIntInsn(NEWARRAY, 0);

    Exception exception = assertThrows(IllegalArgumentException.class, visitIntInsn);
    assertEquals("Invalid operand (must be an array type code T_...): 0", exception.getMessage());
  }

  @Test
  public void testVisitIntInsn_illegalByteOperand() {
    checkMethodAdapter.visitCode();

    Executable visitIntInsn = () -> checkMethodAdapter.visitIntInsn(BIPUSH, Integer.MAX_VALUE);

    Exception exception = assertThrows(IllegalArgumentException.class, visitIntInsn);
    assertEquals("Invalid operand (must be a signed byte): 2147483647", exception.getMessage());
  }

  @Test
  public void testVisitIntInsn_illegalShortOperand() {
    checkMethodAdapter.visitCode();

    Executable visitIntInsn = () -> checkMethodAdapter.visitIntInsn(SIPUSH, Integer.MAX_VALUE);

    Exception exception = assertThrows(IllegalArgumentException.class, visitIntInsn);
    assertEquals("Invalid operand (must be a signed short): 2147483647", exception.getMessage());
  }

  @Test
  public void testVisitVarInsn_illegalOperand() {
    checkMethodAdapter.visitCode();

    Executable visitVarInsn = () -> checkMethodAdapter.visitVarInsn(ALOAD, -1);

    Exception exception = assertThrows(IllegalArgumentException.class, visitVarInsn);
    assertEquals(
            "Invalid local variable index (must be an unsigned short): -1", exception.getMessage());
  }

  @Test
  public void testVisitTypeInsn_illegalOperand() {
    checkMethodAdapter.visitCode();

    Executable visitTypeInsn = () -> checkMethodAdapter.visitTypeInsn(NEW, "[I");

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeInsn);
    assertEquals("NEW cannot be used to create arrays: [I", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_nullOwner() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, null, "i", "I");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid owner (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_invalidOwnerDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "-", "i", "I");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid owner (must be an internal class name): -", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_nullName() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", null, "I");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid name (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_invalidFieldName() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "-", "I");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid name (must be a valid Java identifier): -", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_invalidFieldName2() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "a-", "I");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid name (must be a valid Java identifier): a-", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_nullDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "i", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid type descriptor (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_voidDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "i", "V");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid descriptor: V", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_invalidPrimitiveDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "i", "II");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid descriptor: II", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_illegalArrayDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "i", "[");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid descriptor: [", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_invalidReferenceDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "i", "L");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid descriptor: L", exception.getMessage());
  }

  @Test
  public void testVisitFieldInsn_invalidReferenceDescriptor2() {
    checkMethodAdapter.visitCode();

    Executable visitFieldInsn = () -> checkMethodAdapter.visitFieldInsn(GETFIELD, "C", "i", "L-;");

    Exception exception = assertThrows(IllegalArgumentException.class, visitFieldInsn);
    assertEquals("Invalid descriptor: L-;", exception.getMessage());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedVisitMethodInsn_invalidOpcode() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn = () -> checkMethodAdapter.visitMethodInsn(42, "o", "m", "()V");

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("Invalid opcode: 42", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_invalidOpcode() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(42, "o", "m", "()V", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("Invalid opcode: 42", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_nullName() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", null, "()V", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("Invalid name (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_invalidName() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", "-", "()V", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals(
            "Invalid name (must be a '<init>', '<clinit>' or a valid Java identifier): -",
            exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_invalidName2() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", "a-", "()V", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals(
            "Invalid name (must be a '<init>', '<clinit>' or a valid Java identifier): a-",
            exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_nullDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", "m", null, false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("Invalid method descriptor (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_invalidDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "I", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("Invalid descriptor: I", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_invalidParameterDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "(V)V", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("Invalid descriptor: (V)V", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_invalidReturnDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "()VV", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("Invalid descriptor: ()VV", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_illegalInvokeInterface() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEINTERFACE, "C", "m", "()V", false);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("INVOKEINTERFACE can't be used with classes", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_illegalInvokeInterface2() {
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKEVIRTUAL, "C", "m", "()V", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals("INVOKEVIRTUAL can't be used with interfaces", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_illegalInvokeSpecial() {
    checkMethodAdapter.version = Opcodes.V1_7;
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKESPECIAL, "C", "m", "()V", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethodInsn);
    assertEquals(
            "INVOKESPECIAL can't be used with interfaces prior to Java 8", exception.getMessage());
  }

  @Test
  public void testVisitMethodInsn_invokeSpecialOnInterface() {
    checkMethodAdapter.version = Opcodes.V1_8;
    checkMethodAdapter.visitCode();

    Executable visitMethodInsn =
            () -> checkMethodAdapter.visitMethodInsn(INVOKESPECIAL, "C", "m", "()V", true);

    assertDoesNotThrow(visitMethodInsn);
  }

  @Test
  public void testVisitInvokeDynamicInsn_illegalHandleTag() {
    checkMethodAdapter.visitCode();

    Executable visitInvokeDynamicInsn =
            () ->
                    checkMethodAdapter.visitInvokeDynamicInsn(
                            "m", "()V", new Handle(Opcodes.GETFIELD, "o", "m", "()V", false));

    Exception exception = assertThrows(IllegalArgumentException.class, visitInvokeDynamicInsn);
    assertEquals("invalid handle tag 180", exception.getMessage());
  }

  @Test
  public void testVisitLabel_alreadyVisitedLabel() {
    Label label = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitLabel(label);

    Executable visitLabel = () -> checkMethodAdapter.visitLabel(label);

    Exception exception = assertThrows(IllegalStateException.class, visitLabel);
    assertEquals("Already visited label", exception.getMessage());
  }

  @Test
  public void testVisitLdcInsn_v11_illegalOperandType() {
    checkMethodAdapter.version = Opcodes.V1_1;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn = () -> checkMethodAdapter.visitLdcInsn(new Object());

    Exception exception = assertThrows(IllegalArgumentException.class, visitLdcInsn);
    assertTrue(exception.getMessage().startsWith("Invalid constant: java.lang.Object@"));
  }

  @Test
  public void testVisitLdcInsn_v11_primitiveDescriptor() {
    checkMethodAdapter.version = Opcodes.V1_1;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn = () -> checkMethodAdapter.visitLdcInsn(Type.fromDescriptor("I"));

    Exception exception = assertThrows(IllegalArgumentException.class, visitLdcInsn);
    assertEquals("Illegal LDC constant value", exception.getMessage());
  }

  @Test
  public void testVisitLdcInsn_v11_illegalConstantClass() {
    checkMethodAdapter.version = Opcodes.V1_1;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn = () -> checkMethodAdapter.visitLdcInsn(Type.fromInternalName("I"));

    Exception exception = assertThrows(IllegalArgumentException.class, visitLdcInsn);
    assertEquals("ldc of a constant class requires at least version 1.5", exception.getMessage());
  }

  @Test
  public void testVisitLdcInsn_v11_methodDescriptor() {
    checkMethodAdapter.version = Opcodes.V1_1;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn = () -> checkMethodAdapter.visitLdcInsn(Type.fromMethod("()V"));

    Exception exception = assertThrows(IllegalArgumentException.class, visitLdcInsn);
    assertEquals("ldc of a method type requires at least version 1.7", exception.getMessage());
  }

  @Test
  public void testVisitLdcInsn_v11_handle() {
    checkMethodAdapter.version = Opcodes.V1_1;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn =
            () -> checkMethodAdapter.visitLdcInsn(new Handle(Opcodes.GETFIELD, "o", "m", "()V", false));

    Exception exception = assertThrows(IllegalArgumentException.class, visitLdcInsn);
    assertEquals("ldc of a Handle requires at least version 1.7", exception.getMessage());
  }

  @Test
  public void testVisitLdcInsn_v18_invalidHandleTag() {
    checkMethodAdapter.version = Opcodes.V1_8;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn =
            () -> checkMethodAdapter.visitLdcInsn(new Handle(-1, "o", "m", "()V", false));

    Exception exception = assertThrows(IllegalArgumentException.class, visitLdcInsn);
    assertEquals("invalid handle tag -1", exception.getMessage());
  }

  @Test
  public void testVisitLdcInsn_v18_handle() {
    checkMethodAdapter.version = Opcodes.V1_8;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn =
            () ->
                    checkMethodAdapter.visitLdcInsn(
                            new Handle(Opcodes.H_NEWINVOKESPECIAL, "o", "<init>", "()V", false));

    assertDoesNotThrow(visitLdcInsn);
  }

  @Test
  public void testVisitLdcInsn_v18_illegalHandleName() {
    checkMethodAdapter.version = Opcodes.V1_8;
    checkMethodAdapter.visitCode();

    Executable visitLdcInsn =
            () ->
                    checkMethodAdapter.visitLdcInsn(
                            new Handle(Opcodes.H_INVOKEVIRTUAL, "o", "<init>", "()V", false));

    Exception exception = assertThrows(IllegalArgumentException.class, visitLdcInsn);
    assertEquals(
            "Invalid handle name (must be a valid unqualified name): <init>", exception.getMessage());
  }

  @Test
  public void testVisitTableSwitchInsn_invalidMinMax() {
    checkMethodAdapter.visitCode();

    Executable visitTableSwitchInsn =
            () -> checkMethodAdapter.visitTableSwitchInsn(1, 0, new Label(), new Label[0]);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTableSwitchInsn);
    assertEquals("Max = 0 must be greater than or equal to min = 1", exception.getMessage());
  }

  @Test
  public void testVisitTableSwitchInsn_invalidDefaultLabel() {
    checkMethodAdapter.visitCode();

    Executable visitTableSwitchInsn =
            () -> checkMethodAdapter.visitTableSwitchInsn(0, 1, null, new Label[0]);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTableSwitchInsn);
    assertEquals("Invalid default label (must not be null)", exception.getMessage());
  }

  @Test
  public void testVisitTableSwitchInsn_nullKeyLabels() {
    checkMethodAdapter.visitCode();

    Executable visitTableSwitchInsn =
            () -> checkMethodAdapter.visitTableSwitchInsn(0, 1, new Label(), (Label[]) null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTableSwitchInsn);
    assertEquals("There must be max - min + 1 labels", exception.getMessage());
  }

  @Test
  public void testVisitTableSwitchInsn_invalidKeyLabelCount() {
    checkMethodAdapter.visitCode();

    Executable visitTableSwitchInsn =
            () -> checkMethodAdapter.visitTableSwitchInsn(0, 1, new Label(), new Label[0]);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTableSwitchInsn);
    assertEquals("There must be max - min + 1 labels", exception.getMessage());
  }

  @Test
  public void testVisitLookupSwitchInsn_nullKeyArray_oneLabel() {
    checkMethodAdapter.visitCode();

    Executable visitLookupSwitchInsn =
            () -> checkMethodAdapter.visitLookupSwitchInsn(new Label(), null, new Label[0]);

    Exception exception = assertThrows(IllegalArgumentException.class, visitLookupSwitchInsn);
    assertEquals("There must be the same number of keys and labels", exception.getMessage());
  }

  @Test
  public void testVisitLookupSwitchInsn_noKey_nullLabelArray() {
    checkMethodAdapter.visitCode();

    Executable visitLookupSwitchInsn =
            () -> checkMethodAdapter.visitLookupSwitchInsn(new Label(), new int[0], null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitLookupSwitchInsn);
    assertEquals("There must be the same number of keys and labels", exception.getMessage());
  }

  @Test
  public void testVisitLookupSwitchInsn_noKey_oneNullLabel() {
    checkMethodAdapter.visitCode();

    Executable visitLookupSwitchInsn =
            () -> checkMethodAdapter.visitLookupSwitchInsn(new Label(), new int[0], new Label[1]);

    Exception exception = assertThrows(IllegalArgumentException.class, visitLookupSwitchInsn);
    assertEquals("There must be the same number of keys and labels", exception.getMessage());
  }

  @Test
  public void testVisitMultiANewArrayInsn_invalidDescriptor() {
    checkMethodAdapter.visitCode();

    Executable visitMultiANewArrayInsn = () -> checkMethodAdapter.visitMultiANewArrayInsn("I", 1);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMultiANewArrayInsn);
    assertEquals(
            "Invalid descriptor (must be an array type descriptor): I", exception.getMessage());
  }

  @Test
  public void testVisitMultiANewArrayInsn_notEnoughDimensions() {
    checkMethodAdapter.visitCode();

    Executable visitMultiANewArrayInsn = () -> checkMethodAdapter.visitMultiANewArrayInsn("[[I", 0);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMultiANewArrayInsn);
    assertEquals("Invalid dimensions (must be greater than 0): 0", exception.getMessage());
  }

  @Test
  public void testVisitMultiANewArrayInsn_tooManyDimensions() {
    checkMethodAdapter.visitCode();

    Executable visitMultiANewArrayInsn = () -> checkMethodAdapter.visitMultiANewArrayInsn("[[I", 3);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMultiANewArrayInsn);
    assertEquals(
            "Invalid dimensions (must not be greater than numDimensions(descriptor)): 3",
            exception.getMessage());
  }

  @Test
  public void testVisitInsnAnnotation_invalidTypeReference() {
    checkMethodAdapter.visitCode();

    Executable visitInsnAnnotation =
            () ->
                    checkMethodAdapter.visitInsnAnnotation(
                            TypeReference.newSuperTypeReference(0).getValue(), null, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitInsnAnnotation);
    assertEquals("Invalid type reference sort 0x10", exception.getMessage());
  }

  @Test
  public void testVisitTryCatchBlock_afterStartLabel() {
    Label label0 = new Label();
    Label label1 = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitLabel(label0);

    Executable visitTryCatchBlock =
            () -> checkMethodAdapter.visitTryCatchBlock(label0, label1, label1, null);

    Exception exception = assertThrows(IllegalStateException.class, visitTryCatchBlock);
    assertEquals("Try catch blocks must be visited before their labels", exception.getMessage());
  }

  @Test
  public void testVisitTryCatchBlock_afterEndLabel() {
    Label label0 = new Label();
    Label label1 = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitLabel(label0);

    Executable visitTryCatchBlock =
            () -> checkMethodAdapter.visitTryCatchBlock(label1, label0, label1, null);

    Exception exception = assertThrows(IllegalStateException.class, visitTryCatchBlock);
    assertEquals("Try catch blocks must be visited before their labels", exception.getMessage());
  }

  @Test
  public void testVisitTryCatchBlock_afterHandlerLabel() {
    Label label0 = new Label();
    Label label1 = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitLabel(label0);

    Executable visitTryCatchBlock =
            () -> checkMethodAdapter.visitTryCatchBlock(label1, label1, label0, null);

    Exception exception = assertThrows(IllegalStateException.class, visitTryCatchBlock);
    assertEquals("Try catch blocks must be visited before their labels", exception.getMessage());
  }

  @Test
  public void testVisitTryCatchAnnotation_invalidTypeReference() {
    checkMethodAdapter.visitCode();

    Executable visitTryCatchAnnotation =
            () ->
                    checkMethodAdapter.visitTryCatchAnnotation(
                            TypeReference.newSuperTypeReference(0).getValue(), null, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTryCatchAnnotation);
    assertEquals("Invalid type reference sort 0x10", exception.getMessage());
  }

  @Test
  public void testVisitLocalVariable_invalidRange() {
    Label startLabel = new Label();
    Label endLabel = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitLabel(startLabel);
    checkMethodAdapter.visitInsn(NOP);
    checkMethodAdapter.visitLabel(endLabel);

    Executable visitLocalVariable =
            () -> checkMethodAdapter.visitLocalVariable("i", "I", null, endLabel, startLabel, 0);

    Exception exception = assertThrows(IllegalArgumentException.class, visitLocalVariable);
    assertEquals(
            "Invalid start and end labels (end must be greater than start)", exception.getMessage());
  }

  @Test
  public void testVisitLocalVariableAnnotation_invalidTypeReference() {
    checkMethodAdapter.visitCode();

    Executable visitLocalVariableAnnotation =
            () ->
                    checkMethodAdapter.visitLocalVariableAnnotation(
                            TypeReference.newSuperTypeReference(0).getValue(),
                            null,
                            new Label[0],
                            new Label[0],
                            new int[0],
                            "LA;",
                            true);

    Exception exception =
            assertThrows(IllegalArgumentException.class, visitLocalVariableAnnotation);
    assertEquals("Invalid type reference sort 0x10", exception.getMessage());
  }

  @Test
  public void testVisitLocalVariableAnnotation_nullStart_noEnd_noIndex() {
    checkMethodAdapter.visitCode();

    Executable visitLocalVariableAnnotation =
            () ->
                    checkMethodAdapter.visitLocalVariableAnnotation(
                            TypeReference.LOCAL_VARIABLE << 24,
                            null,
                            null,
                            new Label[0],
                            new int[0],
                            "LA;",
                            true);

    Exception exception =
            assertThrows(IllegalArgumentException.class, visitLocalVariableAnnotation);
    assertEquals(
            "Invalid start, end and index arrays (must be non null and of identical length",
            exception.getMessage());
  }

  @Test
  public void testVisitLocalVariableAnnotation_noStart_nullEnd_noIndex() {
    checkMethodAdapter.visitCode();

    Executable visitLocalVariableAnnotation =
            () ->
                    checkMethodAdapter.visitLocalVariableAnnotation(
                            TypeReference.LOCAL_VARIABLE << 24,
                            null,
                            new Label[0],
                            null,
                            new int[0],
                            "LA;",
                            true);

    Exception exception =
            assertThrows(IllegalArgumentException.class, visitLocalVariableAnnotation);
    assertEquals(
            "Invalid start, end and index arrays (must be non null and of identical length",
            exception.getMessage());
  }

  @Test
  public void testVisitLocalVariableAnnotation_noStart_oneNullEnd_noIndex() {
    checkMethodAdapter.visitCode();

    Executable visitLocalVariableAnnotation =
            () ->
                    checkMethodAdapter.visitLocalVariableAnnotation(
                            TypeReference.LOCAL_VARIABLE << 24,
                            null,
                            new Label[0],
                            new Label[1],
                            new int[0],
                            "LA;",
                            true);

    Exception exception =
            assertThrows(IllegalArgumentException.class, visitLocalVariableAnnotation);
    assertEquals(
            "Invalid start, end and index arrays (must be non null and of identical length",
            exception.getMessage());
  }

  @Test
  public void testVisitLocalVariableAnnotation_noStart_noEnd_oneIndex() {
    checkMethodAdapter.visitCode();

    Executable visitLocalVariableAnnotation =
            () ->
                    checkMethodAdapter.visitLocalVariableAnnotation(
                            TypeReference.RESOURCE_VARIABLE << 24,
                            null,
                            new Label[0],
                            new Label[0],
                            new int[1],
                            "LA;",
                            true);

    Exception exception =
            assertThrows(IllegalArgumentException.class, visitLocalVariableAnnotation);
    assertEquals(
            "Invalid start, end and index arrays (must be non null and of identical length",
            exception.getMessage());
  }

  @Test
  public void testVisitLocalVariableAnnotation_invalidRange() {
    Label startLabel = new Label();
    Label endLabel = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitLabel(startLabel);
    checkMethodAdapter.visitInsn(NOP);
    checkMethodAdapter.visitLabel(endLabel);

    Executable visitLocalVariableAnnotation =
            () ->
                    checkMethodAdapter.visitLocalVariableAnnotation(
                            TypeReference.RESOURCE_VARIABLE << 24,
                            null,
                            new Label[] { endLabel },
                            new Label[] { startLabel },
                            new int[1],
                            "LA;",
                            true);

    Exception exception =
            assertThrows(IllegalArgumentException.class, visitLocalVariableAnnotation);
    assertEquals(
            "Invalid start and end labels (end must be greater than start)", exception.getMessage());
  }

  @Test
  public void testVisitLineNumber_beforeLabel() {
    checkMethodAdapter.visitCode();

    Executable visitLineNumber = () -> checkMethodAdapter.visitLineNumber(0, new Label());

    Exception exception = assertThrows(IllegalArgumentException.class, visitLineNumber);
    assertEquals("Invalid start label (must be visited first)", exception.getMessage());
  }

  @Test
  public void testVisitMaxs_unvisitedJumpLabels() {
    Label label = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitJumpInsn(IFEQ, label);

    Executable visitMaxs = () -> checkMethodAdapter.visitMaxs(0, 0);

    Exception exception = assertThrows(IllegalStateException.class, visitMaxs);
    assertEquals("Undefined label used", exception.getMessage());
  }

  @Test
  public void testVisitMaxs_unvisitedTryCatchLabels() {
    Label startLabel = new Label();
    Label endLabel = new Label();
    Label handlerLabel = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitTryCatchBlock(startLabel, endLabel, handlerLabel, "E");
    checkMethodAdapter.visitLabel(endLabel);
    checkMethodAdapter.visitLabel(handlerLabel);

    Executable visitMaxs = () -> checkMethodAdapter.visitMaxs(0, 0);

    Exception exception = assertThrows(IllegalStateException.class, visitMaxs);
    assertEquals("Undefined try catch block labels", exception.getMessage());
  }

  @Test
  public void testVisitMaxs_invalidTryCatchRange() {
    Label startLabel = new Label();
    Label endLabel = new Label();
    Label handlerLabel = new Label();
    checkMethodAdapter.visitCode();
    checkMethodAdapter.visitTryCatchBlock(endLabel, startLabel, handlerLabel, "E");
    checkMethodAdapter.visitLabel(startLabel);
    checkMethodAdapter.visitInsn(NOP);
    checkMethodAdapter.visitLabel(endLabel);
    checkMethodAdapter.visitLabel(handlerLabel);

    Executable visitMaxs = () -> checkMethodAdapter.visitMaxs(0, 0);

    Exception exception = assertThrows(IllegalStateException.class, visitMaxs);
    assertEquals("Emty try catch block handler range", exception.getMessage());
  }

  @Test
  public void testVisitEnd_invalidDataFlow() {
    MethodVisitor dataFlowCheckMethodAdapter =
            new CheckMethodAdapter(ACC_PUBLIC, "m", "(I)I", null, new HashMap<>());
    dataFlowCheckMethodAdapter.visitCode();
    dataFlowCheckMethodAdapter.visitInsn(RETURN);
    dataFlowCheckMethodAdapter.visitMaxs(0, 2);

    Executable visitEnd = () -> dataFlowCheckMethodAdapter.visitEnd();

    Exception exception = assertThrows(IllegalArgumentException.class, visitEnd);
    assertTrue(
            exception
                    .getMessage()
                    .startsWith("Error at instruction 0: Incompatible return type m(I)I"));
  }

  @Test
  public void testVisitEnd_invalidReturnType() {
    MethodVisitor dataFlowCheckMethodAdapter =
            new CheckMethodAdapter(ACC_PUBLIC, "m", "(I)V", null, new HashMap<>());
    dataFlowCheckMethodAdapter.visitCode();
    dataFlowCheckMethodAdapter.visitVarInsn(ILOAD, 1);
    dataFlowCheckMethodAdapter.visitInsn(IRETURN);
    dataFlowCheckMethodAdapter.visitMaxs(1, 2);

    Executable visitEnd = () -> dataFlowCheckMethodAdapter.visitEnd();

    Exception exception = assertThrows(IllegalArgumentException.class, visitEnd);
    assertTrue(
            exception
                    .getMessage()
                    .startsWith(
                            "Error at instruction 1: Incompatible return type: expected null, but found I m(I)V"));
  }

  @Test
  public void testVisitEnd_dataflowCheckRequiresMaxLocalsAndMaxStack() {
    CheckMethodAdapter dataFlowCheckMethodAdapter =
            new CheckMethodAdapter(0, "m", "()V", null, new HashMap<>());
    dataFlowCheckMethodAdapter.visitCode();
    dataFlowCheckMethodAdapter.visitVarInsn(ALOAD, 0);
    dataFlowCheckMethodAdapter.visitInsn(RETURN);
    dataFlowCheckMethodAdapter.visitMaxs(0, 0);

    Executable visitEnd = () -> dataFlowCheckMethodAdapter.visitEnd();

    Exception exception = assertThrows(IllegalArgumentException.class, visitEnd);
    assertEquals(
            "Data flow checking option requires valid, non zero maxLocals and maxStack.",
            exception.getMessage());
  }
}
