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
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.ConstantDynamic;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.commons.MethodNodeBuilder;
import cn.taketoday.bytecode.tree.FieldInsnNode;
import cn.taketoday.bytecode.tree.InsnList;
import cn.taketoday.bytecode.tree.LdcInsnNode;
import cn.taketoday.bytecode.tree.MethodInsnNode;
import cn.taketoday.bytecode.tree.MethodNode;

import static cn.taketoday.bytecode.commons.MethodNodeBuilder.buildClassWithMethod;
import static cn.taketoday.bytecode.commons.MethodNodeBuilder.toText;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link AdviceAdapter}.
 *
 * @author Eric Bruneton
 */
public class AdviceAdapterTest extends AsmTest {

  @Test
  public void testAllMethods_invalidConstructor() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 2, 2).insn(Opcodes.IRETURN).build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    Executable accept = () -> inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    Exception exception = assertThrows(IllegalArgumentException.class, accept);
    assertEquals("Invalid return in constructor", exception.getMessage());
  }

  @Test
  public void testAllMethods_simpleConstructor() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 2, 2)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #2.
                    // After instrumentation, expect an after advice here, before instruction #2.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(2).withAfterAdviceAt(2).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithTwoSuperInitInTwoBranches() {
    Label label0 = new Label();
    Label label1 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 3, 2)
                    .iload(1)
                    .insn(Opcodes.ICONST_1)
                    .insn(Opcodes.ICONST_2)
                    .insn(Opcodes.IADD)
                    .jumpInsn(Opcodes.IF_ICMPEQ, label0)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #7.
                    .go(label1)
                    .label(label0)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #11.
                    .label(label1)
                    .typeInsn(Opcodes.NEW, "java/lang/RuntimeException")
                    .insn(Opcodes.DUP)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false)
                    // After instrumentation, expect an after advice here, before instruction #15.
                    .athrow()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod)
                    .withBeforeAdviceAt(7, 11)
                    .withAfterAdviceAt(15)
                    .build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @ParameterizedTest
  @ValueSource(strings = { "true", "false" })
  public void testAllMethods_constructorWithTwoSuperInitInTwoSwitchBranches(
          final boolean useTableSwitch) {
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 4, 2)
                    .iload(1)
                    .insn(Opcodes.ICONST_1)
                    .insn(Opcodes.ICONST_2)
                    .insn(Opcodes.IADD)
                    .switchto(label0, label1, useTableSwitch)
                    .label(label0)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #8.
                    .go(label2)
                    .label(label1)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #12.
                    .go(label2)
                    .label(label2)
                    .typeInsn(Opcodes.NEW, "java/lang/RuntimeException")
                    .insn(Opcodes.DUP)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false)
                    // After instrumentation, expect an after advice here, before instruction #17.
                    .athrow()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod)
                    .withBeforeAdviceAt(8, 12)
                    .withAfterAdviceAt(17)
                    .build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithSuperInitsInNormalAndHandlerBranches() {
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    Label label3 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 2, 2)
                    .trycatch(label0, label1, label2)
                    .label(label0)
                    .insn(Opcodes.NOP)
                    .label(label1)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #5.
                    .go(label3)
                    .label(label2)
                    .insn(Opcodes.POP)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #10.
                    .label(label3)
                    // After instrumentation, expect an after advice here, before instruction #11.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod)
                    .withBeforeAdviceAt(5, 10)
                    .withAfterAdviceAt(11)
                    .build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithUninitThisInTwoBranches() {
    Label label0 = new Label();
    Label label1 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 6, 2)
                    .iload(1)
                    .insn(Opcodes.I2L)
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.LCONST_1)
                    .insn(Opcodes.LADD)
                    .insn(Opcodes.LCMP)
                    .ifne(label0)
                    .insn(Opcodes.NOP)
                    .typeInsn(Opcodes.NEW, "C")
                    .insn(Opcodes.DUP)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, DON'T expect a before advice here, because the above call does
                    // not initialize 'this'.
                    .astore(1)
                    .aload(0)
                    .go(label1)
                    .label(label0)
                    .iconst_0()
                    .aload(0)
                    .insn(Opcodes.SWAP)
                    .insn(Opcodes.POP)
                    .label(label1)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #21.
                    // After instrumentation, expect an after advice here, before instruction #21.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(21).withAfterAdviceAt(21).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithDupX1() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 3, 2)
                    .typeInsn(Opcodes.NEW, "C")
                    .astore(1)
                    .aload(1)
                    .aload(0)
                    .insn(Opcodes.DUP_X1)
                    .insn(Opcodes.POP)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, DON'T expect a before advice here, because the above call does
                    // not initialize 'this'.
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #8.
                    // After instrumentation, expect an after advice here, before instruction #8.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(8).withAfterAdviceAt(8).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithDupX2() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 4, 2)
                    .typeInsn(Opcodes.NEW, "C")
                    .astore(1)
                    .aload(1)
                    .aconst_null()
                    .aload(0)
                    .insn(Opcodes.DUP_X2)
                    .insn(Opcodes.POP2)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // No method enter here because the above call does not initialize 'this'.
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #9.
                    // After instrumentation, expect an after advice here, before instruction #9.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(9).withAfterAdviceAt(9).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithDup2() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 4, 2)
                    .typeInsn(Opcodes.NEW, "C")
                    .astore(1)
                    .aload(0)
                    .aload(1)
                    .insn(Opcodes.DUP2)
                    .insn(Opcodes.POP2)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, DON'T expect a before advice here, because the above call does
                    // not initialize 'this'.
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #8.
                    // After instrumentation, expect an after advice here, before instruction #8.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(8).withAfterAdviceAt(8).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithDup2X1() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 5, 2)
                    .typeInsn(Opcodes.NEW, "C")
                    .astore(1)
                    .aconst_null()
                    .aload(0)
                    .aload(1)
                    .insn(Opcodes.DUP2_X1)
                    .insn(Opcodes.POP2)
                    .insn(Opcodes.POP)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, DON'T expect a before advice here, because the above call does
                    // not initialize 'this'.
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #10.
                    // After instrumentation, expect an after advice here, before instruction #10.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(10).withAfterAdviceAt(10).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithDup2X2() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 6, 2)
                    .typeInsn(Opcodes.NEW, "C")
                    .astore(1)
                    .insn(Opcodes.LCONST_0)
                    .aload(0)
                    .aload(1)
                    .insn(Opcodes.DUP2_X2)
                    .insn(Opcodes.POP2)
                    .insn(Opcodes.POP2)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, DON'T expect a before advice here, because the above call does
                    // not initialize 'this'.
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #10.
                    // After instrumentation, expect an after advice here, before instruction #10.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(10).withAfterAdviceAt(10).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithJsrRet() {
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    Label label3 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 3, 3)
                    .trycatch(label0, label1, label1)
                    .label(label0)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #3.
                    .go(label3)
                    .label(label1)
                    .jsr(label2)
                    // After instrumentation, expect an after advice here, before instruction #6.
                    .athrow()
                    .label(label2)
                    .astore(2)
                    .ret(2)
                    .label(label3)
                    // After instrumentation, expect an after advice here, before instruction #11.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod)
                    .withBeforeAdviceAt(3)
                    .withAfterAdviceAt(6, 11)
                    .build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithLongsAndArrays() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 6, 4)
                    .aload(0)
                    .typeInsn(Opcodes.NEW, "C")
                    .fieldInsn(Opcodes.GETSTATIC, "java/lang/Long", "MAX_VALUE", "J")
                    .varInsn(Opcodes.LSTORE, 2)
                    .insn(Opcodes.ICONST_1)
                    .intInsn(Opcodes.NEWARRAY, Opcodes.T_LONG)
                    .iconst_0()
                    .ldcInsn(123L)
                    .insn(Opcodes.LASTORE)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, DON'T expect a before advice here, because the above call does
                    // not initialize 'this'.
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #11.
                    // After instrumentation, expect an after advice here, before instruction #11.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(11).withAfterAdviceAt(11).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithMultiAnewArray() {
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 3, 2)
                    .aload(0)
                    .insn(Opcodes.ICONST_1)
                    .insn(Opcodes.ICONST_2)
                    .multiANewArrayInsn("[[I", 2)
                    .fieldInsn(Opcodes.PUTSTATIC, "C", "f", "[[I")
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #6.
                    // After instrumentation, expect an after advice here, before instruction #6.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(6).withAfterAdviceAt(6).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithBranchesAfterSuperInit() {
    Label label1 = new Label();
    Label label2 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 2, 2)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #2.
                    .go(label1)
                    .label(label2)
                    .insn(Opcodes.POP)
                    // After instrumentation, expect an after advice here, before instruction #5.
                    .vreturn()
                    .label(label1)
                    .iconst_0()
                    .go(label2)
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(2).withAfterAdviceAt(5).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithForwardGotoAfterBlockWithoutSuccessor() {
    Label label1 = new Label();
    Label label2 = new Label();
    Label label3 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 3, 2)
                    .trycatch(label1, label2, label2)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #2.
                    .label(label1)
                    .iconst_0()
                    .go(label3)
                    .label(label2)
                    // After instrumentation, expect an after advice here, before instruction #6.
                    .athrow()
                    .label(label3)
                    .pop()
                    // After instrumentation, expect an after advice here, before instruction #9.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod)
                    .withBeforeAdviceAt(2)
                    .withAfterAdviceAt(6)
                    .withAfterAdviceAt(9)
                    .build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @ParameterizedTest
  @ValueSource(strings = { "lookupswitch", "tableswitch" })
  public void testAllMethods_constructorWithForwardSwitchAfterBlockWithoutSuccessor(
          final String parameter) {
    Label label1 = new Label();
    Label label2 = new Label();
    Label label3 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 3, 2)
                    .trycatch(label1, label2, label2)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #2.
                    .label(label1)
                    .iconst_0()
                    .iconst_0()
                    .switchto(label3, label3, /*useTableSwitch=*/ parameter.equals("tableswitch"))
                    .label(label2)
                    // After instrumentation, expect an after advice here, before instruction #7.
                    .athrow()
                    .label(label3)
                    .pop()
                    // After instrumentation, expect an after advice here, before instruction #10.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod)
                    .withBeforeAdviceAt(2)
                    .withAfterAdviceAt(7)
                    .withAfterAdviceAt(10)
                    .build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @Test
  public void testAllMethods_constructorWithHandlerFallthroughToPrivateMethodCall() {
    Label label1 = new Label();
    Label label2 = new Label();
    Label label3 = new Label();
    MethodNode inputMethod =
            new MethodNodeBuilder("<init>", "(I)V", 2, 2)
                    .trycatch(label1, label2, label2)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                    // After instrumentation, expect a before advice here, before instruction #2.
                    .label(label1)
                    .go(label3)
                    .label(label2)
                    .astore(1)
                    .label(label3)
                    .aload(0)
                    .methodInsn(Opcodes.INVOKESPECIAL, "C", "privateMethod", "()V", false)
                    // After instrumentation, expect an after advice here, before instruction #9.
                    .vreturn()
                    .build();

    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    inputMethod.accept(new BasicAdviceAdapter(outputMethod));

    MethodNode expectedMethod =
            new ExpectedMethodBuilder(inputMethod).withBeforeAdviceAt(2).withAfterAdviceAt(9).build();
    assertEquals(toText(expectedMethod), toText(outputMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(outputMethod).newInstance());
  }

  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAllMethods_precompiledClass(
          final PrecompiledClass classParameter) throws Exception {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor adviceClassAdapter =
            new EmptyAdviceClassAdapter(classWriter);

    Executable accept = () -> classReader.accept(adviceClassAdapter, ClassReader.EXPAND_FRAMES);

    assertDoesNotThrow(accept);
    ClassWriter expectedClassWriter = new ClassWriter(0);
    ClassVisitor expectedClassVisitor =
            new LocalVariablesSorterTest.LocalVariablesSorterClassAdapter(
                    expectedClassWriter);
    classReader.accept(expectedClassVisitor, ClassReader.EXPAND_FRAMES);
    assertEquals(
            new ClassFile(expectedClassWriter.toByteArray()), new ClassFile(classWriter.toByteArray()));
  }

  @Test
  public void testOnMethodEnter_mixedVisitors() {
    MethodNode outputMethod = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(I)V", null, null);
    AdviceAdapter adviceAdapter =
            new AdviceAdapter(
                    new MethodVisitor(outputMethod) { },
                    Opcodes.ACC_PUBLIC,
                    "<init>",
                    "()V") {
              @Override
              protected void onMethodEnter() {
                Label label = new Label();
                visitLabel(label);
                // Generate ICONST_1 with the delegate visitor. The advice adapter does not 'see' this
                // and therefore cannot update its stack state, if it were doing so.
                mv.visitInsn(ICONST_1);
                // Generate IFEQ with the advice adapter itself. If the stack was updated here, it would
                // pop from an empty stack because the previous ICONST_1 was not simulated.
                visitJumpInsn(IFEQ, label);
              }

              @Override
              protected void onMethodExit(final int opcode) { }
            };

    adviceAdapter.visitCode();
    adviceAdapter.visitVarInsn(Opcodes.ALOAD, 0);
    adviceAdapter.visitMethodInsn(
            Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    adviceAdapter.visitInsn(Opcodes.RETURN);
    adviceAdapter.visitMaxs(0, 0);
    adviceAdapter.visitEnd();

    assertEquals(
            "    ALOAD 0\n"
                    + "    INVOKESPECIAL java/lang/Object.<init> ()V\n"
                    + "   L0\n"
                    + "    ICONST_1\n"
                    + "    IFEQ L0\n"
                    + "    RETURN\n"
                    + "    MAXSTACK = 0\n"
                    + "    MAXLOCALS = 1\n",
            toText(outputMethod));
  }

  private static InsnList newBasicAdvice(final boolean isAfterAdvice) {
    InsnList insnList = new InsnList();
    insnList.add(
            new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;"));
    insnList.add(new LdcInsnNode(isAfterAdvice ? "exit" : "enter"));
    insnList.add(
            new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "java/io/PrintStream",
                    "println",
                    "(Ljava/lang/String;)V",
                    /* isInterface= */ false));
    return insnList;
  }

  private static class BasicAdviceAdapter extends AdviceAdapter {

    BasicAdviceAdapter(final MethodVisitor methodVisitor) {
      super(
              methodVisitor,
              Opcodes.ACC_PUBLIC,
              "<init>",
              "(I)V");
    }

    @Override
    public void visitLdcInsn(final Object value) {
      if (value instanceof Boolean
              || value instanceof Byte
              || value instanceof Short
              || value instanceof Character
              || value instanceof Integer
              || value instanceof Long
              || value instanceof Double
              || value instanceof Float
              || value instanceof String
              || value instanceof Type
              || value instanceof Handle
              || value instanceof ConstantDynamic) {
        super.visitLdcInsn(value);
      }
      else {
        // If this happens, add support for the new type in AdviceAdapter.visitLdcInsn(), if needed.
        throw new IllegalArgumentException("Unsupported type of value: " + value);
      }
    }

    @Override
    protected void onMethodEnter() {
      newBasicAdvice(/* isAfterAdvice= */ false).accept(this);
    }

    @Override
    protected void onMethodExit(final int opcode) {
      newBasicAdvice(/* isAfterAdvice= */ true).accept(this);
    }
  }

  private static class ExpectedMethodBuilder {

    private final MethodNode inputMethod;
    private final ArrayList<Advice> advices;

    ExpectedMethodBuilder(final MethodNode inputMethod) {
      this.inputMethod = inputMethod;
      this.advices = new ArrayList<>();
    }

    ExpectedMethodBuilder withBeforeAdviceAt(final int... insnIndices) {
      for (int insnIndex : insnIndices) {
        advices.add(new Advice(insnIndex, /* isAfterAdvice= */ false));
      }
      return this;
    }

    ExpectedMethodBuilder withAfterAdviceAt(final int... insnIndices) {
      for (int insnIndex : insnIndices) {
        advices.add(new Advice(insnIndex, /* isAfterAdvice= */ true));
      }
      return this;
    }

    MethodNode build() {
      MethodNode outputMethod =
              new MethodNode(inputMethod.access, inputMethod.name, inputMethod.desc, null, null);
      inputMethod.accept(outputMethod);

      Collections.sort(advices);
      for (Advice advice : advices) {
        outputMethod.instructions.insertBefore(
                outputMethod.instructions.get(advice.insnIndex), newBasicAdvice(advice.isAfterAdvice));
      }
      return outputMethod;
    }

    static class Advice implements Comparable<Advice> {

      final int insnIndex;
      final boolean isAfterAdvice;

      Advice(final int insnIndex, final boolean isAfterAdvice) {
        this.insnIndex = insnIndex;
        this.isAfterAdvice = isAfterAdvice;
      }

      @Override
      public int compareTo(final Advice other) {
        if (other.insnIndex == insnIndex) {
          return Boolean.compare(other.isAfterAdvice, isAfterAdvice);
        }
        else {
          return Integer.compare(other.insnIndex, insnIndex);
        }
      }
    }
  }

  private static class EmptyAdviceClassAdapter extends ClassVisitor {

    EmptyAdviceClassAdapter(final ClassVisitor classVisitor) {
      super(classVisitor);
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
      if (methodVisitor == null || (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) > 0) {
        return methodVisitor;
      }
      return new AdviceAdapter(methodVisitor, access, name, descriptor) { };
    }
  }
}
