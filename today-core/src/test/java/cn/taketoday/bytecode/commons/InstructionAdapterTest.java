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

import java.util.stream.Collectors;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.util.Textifier;
import cn.taketoday.bytecode.util.TraceMethodVisitor;

import static cn.taketoday.bytecode.commons.MethodNodeBuilder.toText;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InstructionAdapter}.
 *
 * @author Eric Bruneton
 */
public class InstructionAdapterTest extends AsmTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new InstructionAdapter(new MethodNode()));
  }

  @Test
  public void testVisitInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitInsn = () -> instructionAdapter.visitInsn(Opcodes.GOTO);

    assertThrows(IllegalArgumentException.class, visitInsn);
  }

  @Test
  public void testVisitIntInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitIntInsn = () -> instructionAdapter.visitIntInsn(Opcodes.GOTO, 0);

    assertThrows(IllegalArgumentException.class, visitIntInsn);
  }

  @Test
  public void testVisitIntInsn_illegalNewArrayArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitIntInsn = () -> instructionAdapter.visitIntInsn(Opcodes.NEWARRAY, 0);

    assertThrows(IllegalArgumentException.class, visitIntInsn);
  }

  @Test
  public void testVisitVarInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitVarInsn = () -> instructionAdapter.visitVarInsn(Opcodes.GOTO, 0);

    assertThrows(IllegalArgumentException.class, visitVarInsn);
  }

  @Test
  public void testVisitTypeInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitTypeInsn = () -> instructionAdapter.visitTypeInsn(Opcodes.GOTO, "pkg/Class");

    assertThrows(IllegalArgumentException.class, visitTypeInsn);
  }

  @Test
  public void testVisitFieldInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitFieldInsn =
            () -> instructionAdapter.visitFieldInsn(Opcodes.INVOKEVIRTUAL, "pkg/Class", "name", "I");

    assertThrows(IllegalArgumentException.class, visitFieldInsn);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedVisitMethodInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitMethodInsn =
            () -> instructionAdapter.visitMethodInsn(Opcodes.GETFIELD, "pkg/Class", "name", "I");

    assertThrows(IllegalArgumentException.class, visitMethodInsn);
  }

  @Test
  public void testVisitMethodInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitMethodInsn =
            () -> instructionAdapter.visitMethodInsn(Opcodes.GETFIELD, "pkg/Class", "name", "I", false);

    assertThrows(IllegalArgumentException.class, visitMethodInsn);
  }

  @Test
  public void testVisitJumpInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitJumpInsn = () -> instructionAdapter.visitJumpInsn(Opcodes.NOP, new Label());

    assertThrows(IllegalArgumentException.class, visitJumpInsn);
  }

  @Test
  public void testVisitLdcInsn() {
    Textifier textifier = new Textifier();
    InstructionAdapter instructionAdapter =
            new InstructionAdapter(new TraceMethodVisitor(textifier));

    instructionAdapter.visitLdcInsn(Boolean.FALSE);
    instructionAdapter.visitLdcInsn(Boolean.TRUE);
    instructionAdapter.visitLdcInsn(Byte.valueOf((byte) 2));
    instructionAdapter.visitLdcInsn(Character.valueOf('3'));
    instructionAdapter.visitLdcInsn(Short.valueOf((short) 4));
    instructionAdapter.visitLdcInsn(Integer.valueOf(5));
    instructionAdapter.visitLdcInsn(Long.valueOf(6));
    instructionAdapter.visitLdcInsn(Float.valueOf(7.0f));
    instructionAdapter.visitLdcInsn(Double.valueOf(8.0));
    instructionAdapter.visitLdcInsn("9");
    instructionAdapter.visitLdcInsn(Type.fromInternalName("pkg/Class"));
    instructionAdapter.visitLdcInsn(
            new Handle(Opcodes.H_GETFIELD, "pkg/Class", "name", "I", /* isInterface= */ false));

    assertEquals(
            "ICONST_0 ICONST_1 ICONST_2 BIPUSH 51 ICONST_4 ICONST_5 LDC 6 LDC 7.0 LDC 8.0 LDC \"9\" "
                    + "LDC Lpkg/Class;.class LDC pkg/Class.nameI (1)",
            textifier.text.stream()
                    .map(text -> text.toString().trim())
                    .collect(Collectors.joining(" ")));
  }

  @Test
  public void testVisitLdcInsn_illegalArgument() {
    InstructionAdapter instructionAdapter = new InstructionAdapter(new MethodNode());

    Executable visitLdcInsn = () -> instructionAdapter.visitLdcInsn(new Object());

    assertThrows(IllegalArgumentException.class, visitLdcInsn);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedInvokeSpecial() {
    MethodNode methodNode = new MethodNode();
    InstructionAdapter instructionAdapter = new InstructionAdapter(methodNode);

    instructionAdapter.invokeSpecial("pkg/Class", "name", "()V");

    assertTrue(toText(methodNode).trim().startsWith("INVOKESPECIAL pkg/Class.name ()V"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedInvokeVirtual() {
    MethodNode methodNode = new MethodNode();
    InstructionAdapter instructionAdapter = new InstructionAdapter(methodNode);

    instructionAdapter.invokeVirtual("pkg/Class", "name", "()V");

    assertTrue(toText(methodNode).trim().startsWith("INVOKEVIRTUAL pkg/Class.name ()V"));
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testDeprecatedInvokeStatic() {
    MethodNode methodNode = new MethodNode();
    InstructionAdapter instructionAdapter = new InstructionAdapter(methodNode);

    instructionAdapter.invokeStatic("pkg/Class", "name", "()V");

    assertTrue(toText(methodNode).trim().startsWith("INVOKESTATIC pkg/Class.name ()V"));
  }

  /** Tests that classes transformed with an InstructionAdapter are unchanged. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAllMethods_precompiledClass(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor instructionClassAdapter =
            new InstructionClassAdapter(classWriter);

    Executable accept = () -> classReader.accept(instructionClassAdapter, attributes(), 0);

    assertDoesNotThrow(accept);
    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  private static Attribute[] attributes() {
    return new Attribute[] { new Comment(), new CodeComment() };
  }

  static class InstructionClassAdapter extends ClassVisitor {

    InstructionClassAdapter(final ClassVisitor classVisitor) {
      super(classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      return new InstructionAdapter(super.visitMethod(access, name, descriptor, signature, exceptions));
    }
  }
}
