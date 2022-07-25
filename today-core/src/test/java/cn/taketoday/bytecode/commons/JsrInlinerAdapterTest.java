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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.commons.MethodNodeBuilder;
import cn.taketoday.bytecode.tree.MethodNode;

import static cn.taketoday.bytecode.commons.MethodNodeBuilder.buildClassWithMethod;
import static cn.taketoday.bytecode.commons.MethodNodeBuilder.toText;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JSRInlinerAdapter}.
 *
 * @author Eric Bruneton
 */
public class JsrInlinerAdapterTest extends AsmTest {

  // Some local variable numbers used in tests.
  private static final int LOCAL1 = 1;
  private static final int LOCAL2 = 2;
  private static final int LOCAL3 = 3;
  private static final int LOCAL4 = 4;
  private static final int LOCAL5 = 5;

  // Labels used to generate test cases.
  private final Label label0 = new Label();
  private final Label label1 = new Label();
  private final Label label2 = new Label();
  private final Label label3 = new Label();
  private final Label label4 = new Label();
  private final Label label5 = new Label();
  private final Label label6 = new Label();
  private final Label label7 = new Label();
  private final Label label8 = new Label();
  private final Label label9 = new Label();
  private final Label label10 = new Label();
  private final Label label11 = new Label();
  private final Label label12 = new Label();

  // Labels used to generate expected results.
  private final Label expectedLabel0 = new Label();
  private final Label expectedLabel1 = new Label();
  private final Label expectedLabel2 = new Label();
  private final Label expectedLabel3 = new Label();
  private final Label expectedLabel4 = new Label();
  private final Label expectedLabel5 = new Label();
  private final Label expectedLabel6 = new Label();
  private final Label expectedLabel7 = new Label();
  private final Label expectedLabel8 = new Label();
  private final Label expectedLabel9 = new Label();
  private final Label expectedLabel10 = new Label();
  private final Label expectedLabel11 = new Label();
  private final Label expectedLabel12 = new Label();
  private final Label expectedLabel13 = new Label();
  private final Label expectedLabel14 = new Label();
  private final Label expectedLabel15 = new Label();
  private final Label expectedLabel16 = new Label();
  private final Label expectedLabel17 = new Label();
  private final Label expectedLabel18 = new Label();
  private final Label expectedLabel19 = new Label();
  private final Label expectedLabel20 = new Label();
  private final Label expectedLabel21 = new Label();
  private final Label expectedLabel22 = new Label();
  private final Label expectedLabel23 = new Label();
  private final Label expectedLabel24 = new Label();
  private final Label expectedLabel25 = new Label();
  private final Label expectedLabel26 = new Label();
  private final Label expectedLabel27 = new Label();
  private final Label expectedLabel28 = new Label();

  @Test
  public void testConstructor() {
    new JSRInlinerAdapter(null, Opcodes.ACC_PUBLIC, "name", "()V", null, null);
  }

  /**
   * Tests a method which has the most basic <code>try{}finally</code> form imaginable. More
   * specifically:
   *
   * <pre>
   * public void a() {
   *   int a = 0;
   *   try {
   *     a++;
   *   } finally {
   *     a--;
   *   }
   * }
   * </pre>
   */
  @Test
  public void testInlineJsr_basicTryFinally() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // Body of try block.
                    .iinc(1, 1)
                    .go(label3)
                    .label(label1) // Exception handler.
                    .astore(3)
                    .jsr(label2)
                    .aload(3)
                    .athrow()
                    .label(label2) // Subroutine.
                    .astore(2)
                    .iinc(1, -1)
                    .ret(2)
                    .label(label3) // Non-exceptional exit from try block.
                    .jsr(label2)
                    .label(label4)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label3, label4, label1)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // Try/catch block.
                    .iinc(1, 1)
                    .go(expectedLabel3)
                    .label(expectedLabel1) // Exception handler.
                    .astore(3)
                    .aconst_null()
                    .go(expectedLabel6)
                    .label(expectedLabel2)
                    .aload(3)
                    .athrow()
                    .label(expectedLabel3) // On non-exceptional exit, try block leads here.
                    .aconst_null()
                    .go(expectedLabel7)
                    .label(expectedLabel4)
                    .label(expectedLabel5)
                    .vreturn()
                    .label(expectedLabel6) // First instantiation of subroutine.
                    .astore(2)
                    .iinc(1, -1)
                    .go(expectedLabel2)
                    .label()
                    .label(expectedLabel7) // Second instantiation of subroutine.
                    .astore(2)
                    .iinc(1, -1)
                    .go(expectedLabel4)
                    .label()
                    .trycatch(expectedLabel0, expectedLabel1, expectedLabel1)
                    .trycatch(expectedLabel3, expectedLabel5, expectedLabel1)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * Tests a method which has an if/else in the finally clause. More specifically:
   *
   * <pre>
   * public void a() {
   *   int a = 0;
   *   try {
   *     a++;
   *   } finally {
   *     if (a == 0) {
   *       a += 2;
   *     } else {
   *       a += 3;
   *     }
   *   }
   * }
   * </pre>
   */
  @Test
  public void testInlineJsr_ifElseInFinally() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // Body of try block.
                    .iinc(1, 1)
                    .go(label5)
                    .label(label1) // Exception handler.
                    .astore(3)
                    .jsr(label2)
                    .aload(3)
                    .athrow()
                    .label(label2) // Subroutine.
                    .astore(2)
                    .iload(1)
                    .ifne(label3)
                    .iinc(1, 2)
                    .go(label4)
                    .label(label3) // Test "a != 0".
                    .iinc(1, 3)
                    .label(label4) // Common exit.
                    .ret(2)
                    .label(label5) // Non-exceptional exit from try block.
                    .jsr(label2)
                    .label(label6) // Used in the TRYCATCH below.
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label5, label6, label1)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // Try/catch block.
                    .iinc(1, 1)
                    .go(expectedLabel3)
                    .label(expectedLabel1) // Exception handler.
                    .astore(3)
                    .aconst_null()
                    .go(expectedLabel6)
                    .label(expectedLabel2)
                    .aload(3)
                    .athrow()
                    .label(expectedLabel3) // On non-exceptional exit, try block leads here.
                    .aconst_null()
                    .go(expectedLabel9)
                    .label(expectedLabel4)
                    .label(expectedLabel5)
                    .vreturn()
                    .label(expectedLabel6) // First instantiation of subroutine.
                    .astore(2)
                    .iload(1)
                    .ifne(expectedLabel7)
                    .iinc(1, 2)
                    .go(expectedLabel8)
                    .label(expectedLabel7) // Test "a != 0".
                    .iinc(1, 3)
                    .label(expectedLabel8) // Common exit.
                    .go(expectedLabel2)
                    .label()
                    .label(expectedLabel9) // Second instantiation of subroutine.
                    .astore(2)
                    .iload(1)
                    .ifne(expectedLabel10)
                    .iinc(1, 2)
                    .go(expectedLabel11)
                    .label(expectedLabel10) // Test "a != 0".
                    .iinc(1, 3)
                    .label(expectedLabel11) // Common exit.
                    .go(expectedLabel4)
                    .label()
                    .trycatch(expectedLabel0, expectedLabel1, expectedLabel1)
                    .trycatch(expectedLabel3, expectedLabel5, expectedLabel1)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * Tests a method which has a lookupswitch or tableswitch w/in the finally clause. More
   * specifically:
   *
   * <pre>
   * public void a() {
   *   int a = 0;
   *   try {
   *     a++;
   *   } finally {
   *     switch (a) {
   *       case 0:
   *         a += 2;
   *         break;
   *       default:
   *         a += 3;
   *     }
   *   }
   * }
   * </pre>
   */
  @ParameterizedTest
  @ValueSource(strings = { "true", "false" })
  public void testInlineJsr_lookupOrTableSwitchInFinally(final boolean useTableSwitch) {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // Body of try block.
                    .iinc(1, 1)
                    .go(label6)
                    .label(label1) // Exception handler.
                    .astore(3)
                    .jsr(label2)
                    .aload(3)
                    .athrow()
                    .label(label2) // Subroutine.
                    .astore(2)
                    .iload(1)
                    .switchto(label4, 0, label3, useTableSwitch)
                    .label(label3) // First switch case.
                    .iinc(1, 2)
                    .go(label5)
                    .label(label4) // Default switch case.
                    .iinc(1, 3)
                    .label(label5) // Common exit.
                    .ret(2)
                    .label(label6) // Non-exceptional exit from try block.
                    .jsr(label2)
                    .label(label7)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label6, label7, label1)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // Try/catch block.
                    .iinc(1, 1)
                    .go(expectedLabel3)
                    .label(expectedLabel1) // Exception handler.
                    .astore(3)
                    .aconst_null()
                    .go(expectedLabel6)
                    .label(expectedLabel2)
                    .aload(3)
                    .athrow()
                    .label(expectedLabel3) // On non-exceptional exit, try block leads here.
                    .aconst_null()
                    .go(expectedLabel10)
                    .label(expectedLabel4)
                    .label(expectedLabel5)
                    .vreturn()
                    .label(expectedLabel6) // First instantiation of subroutine.
                    .astore(2)
                    .iload(1)
                    .switchto(expectedLabel8, 0, expectedLabel7, useTableSwitch)
                    .label(expectedLabel7) // First switch case.
                    .iinc(1, 2)
                    .go(expectedLabel9)
                    .label(expectedLabel8) // Default switch case.
                    .iinc(1, 3)
                    .label(expectedLabel9) // Common exit.
                    .go(expectedLabel2)
                    .label()
                    .label(expectedLabel10) // Second instantiation of subroutine.
                    .astore(2)
                    .iload(1)
                    .switchto(expectedLabel12, 0, expectedLabel11, useTableSwitch)
                    .label(expectedLabel11) // First switch case.
                    .iinc(1, 2)
                    .go(expectedLabel13)
                    .label(expectedLabel12) // Default switch case.
                    .iinc(1, 3)
                    .label(expectedLabel13) // Common exit.
                    .go(expectedLabel4)
                    .label()
                    .trycatch(expectedLabel0, expectedLabel1, expectedLabel1)
                    .trycatch(expectedLabel3, expectedLabel5, expectedLabel1)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * Tests a simple nested finally. More specifically:
   *
   * <pre>
   * public void a1() {
   *   int a = 0;
   *   try {
   *     a += 1;
   *   } finally {
   *     try {
   *       a += 2;
   *     } finally {
   *       a += 3;
   *     }
   *   }
   * }
   * </pre>
   */
  @Test
  public void testInlineJsr_simpleNestedFinally() {
    MethodNode inputMethod =
            new MethodNodeBuilder(2, 6)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // Body of try block.
                    .iinc(1, 1)
                    .jsr(label2)
                    .go(label5)
                    .label(label1) // First exception handler.
                    .jsr(label2)
                    .athrow()
                    .label(label2) // First subroutine.
                    .astore(2)
                    .iinc(1, 2)
                    .jsr(label4)
                    .ret(2)
                    .label(label3) // Second exception handler.
                    .jsr(label4)
                    .athrow()
                    .label(label4) // Second subroutine.
                    .astore(3)
                    .iinc(1, 3)
                    .ret(3)
                    .label(label5) // On normal exit, try block jumps here.
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label2, label3, label3)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(2, 6)
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // Body of try block.
                    .iinc(1, 1)
                    .aconst_null()
                    .go(expectedLabel5)
                    .label(expectedLabel1)
                    .go(expectedLabel4)
                    .label(expectedLabel2) // First exception handler.
                    .aconst_null()
                    .go(expectedLabel9)
                    .label(expectedLabel3)
                    .athrow()
                    .label(expectedLabel4) // On normal exit, try block jumps here.
                    .vreturn()
                    .label(expectedLabel5) // First instantiation of first subroutine.
                    .astore(2)
                    .iinc(1, 2)
                    .aconst_null()
                    .go(expectedLabel13)
                    .label(expectedLabel6)
                    .go(expectedLabel1)
                    .label(expectedLabel7)
                    .aconst_null()
                    .go(expectedLabel14)
                    .label(expectedLabel8)
                    .athrow()
                    .label()
                    .label(expectedLabel9) // Second instantiation of first subroutine.
                    .astore(2)
                    .iinc(1, 2)
                    .aconst_null()
                    .go(expectedLabel15)
                    .label(expectedLabel10)
                    .go(expectedLabel3)
                    .label(expectedLabel11)
                    .aconst_null()
                    .go(expectedLabel16)
                    .label(expectedLabel12)
                    .athrow()
                    .label()
                    .label(expectedLabel13) // First instantiation of second subroutine.
                    .astore(3)
                    .iinc(1, 3)
                    .go(expectedLabel6)
                    .label()
                    .label(expectedLabel14) // Second instantiation of second subroutine.
                    .astore(3)
                    .iinc(1, 3)
                    .go(expectedLabel8)
                    .label()
                    .label(expectedLabel15) // Third instantiation of second subroutine.
                    .astore(3)
                    .iinc(1, 3)
                    .go(expectedLabel10)
                    .label()
                    .label(expectedLabel16) // Fourth instantiation of second subroutine.
                    .astore(3)
                    .iinc(1, 3)
                    .go(expectedLabel12)
                    .label()
                    .trycatch(expectedLabel0, expectedLabel2, expectedLabel2)
                    .trycatch(expectedLabel5, expectedLabel7, expectedLabel7)
                    .trycatch(expectedLabel9, expectedLabel11, expectedLabel11)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * This tests a subroutine which has no ret statement, but ends in a "return" instead.
   *
   * <p>We structure this as a try/finally with a break in the finally. Because the while loop is
   * infinite, it's clear from the byte code that the only path which reaches the RETURN instruction
   * is through the subroutine.
   *
   * <pre>
   * public void a1() {
   *   int a = 0;
   *   while (true) {
   *     try {
   *       a += 1;
   *     } finally {
   *       a += 2;
   *       break;
   *     }
   *   }
   * }
   * </pre>
   */
  @Test
  public void testInlineJsr_subroutineWithNoRet() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // While loop header/try block.
                    .iinc(1, 1)
                    .jsr(label2)
                    .go(label3)
                    .label(label1) // Implicit catch block.
                    .astore(2)
                    .jsr(label2)
                    .aload(2)
                    .athrow()
                    .label(label2) // Subroutine ...
                    .astore(3)
                    .iinc(1, 2)
                    .go(label4) // ... note that it does not return!
                    .label(label3) // End of the loop... goes back to the top!
                    .go(label0)
                    .label(label4)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // While loop header/try block.
                    .iinc(1, 1)
                    .aconst_null()
                    .go(expectedLabel5)
                    .label(expectedLabel1)
                    .go(expectedLabel4)
                    .label(expectedLabel2) // Implicit catch block.
                    .astore(2)
                    .aconst_null()
                    .go(expectedLabel7)
                    .label(expectedLabel3)
                    .aload(2)
                    .athrow()
                    .label(expectedLabel4) // End of the loop... goes back to the top!
                    .go(expectedLabel0)
                    .label()
                    .label(expectedLabel5) // First instantiation of subroutine ...
                    .astore(3)
                    .iinc(1, 2)
                    .go(expectedLabel6) // ... note that it does not return!
                    .label(expectedLabel6)
                    .vreturn()
                    .label(expectedLabel7) // Second instantiation of subroutine ...
                    .astore(3)
                    .iinc(1, 2)
                    .go(expectedLabel8) // ... note that it does not return!
                    .label(expectedLabel8)
                    .vreturn()
                    .trycatch(expectedLabel0, expectedLabel2, expectedLabel2)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * This tests a subroutine which has no ret statement, but ends in a "return" instead. The code
   * after the JSR appears to fall through the end of the method, but is in fact unreachable and
   * therefore valid.
   *
   * <pre>
   *   JSR L0
   *   GOTO L1
   * L0:
   *   ASTORE 0
   *   RETURN
   * L1:
   *   ACONST_NULL
   * </pre>
   */
  @Test
  public void testInlineJsr_subroutineWithNoRet2() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 1)
                    .jsr(label0)
                    .go(label1)
                    .label(label0)
                    .astore(0)
                    .vreturn()
                    .label(label1)
                    .aconst_null()
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 1)
                    .aconst_null()
                    .go(expectedLabel2)
                    .label(expectedLabel0)
                    .go(expectedLabel1)
                    .label(expectedLabel1)
                    .aconst_null()
                    .label(expectedLabel2) // First instantiation of subroutine.
                    .astore(0)
                    .vreturn()
                    .label()
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * This tests a subroutine which has no ret statement, but instead exits implicitly by branching
   * to code which is not part of the subroutine. (Sadly, this is legal)
   *
   * <p>We structure this as a try/finally in a loop with a break in the finally. The loop is not
   * trivially infinite, so the RETURN statement is reachable both from the JSR subroutine and from
   * the main entry point.
   *
   * <pre>
   * public void a1() {
   *   int a = 0;
   *   while (null == null) {
   *     try {
   *       a += 1;
   *     } finally {
   *       a += 2;
   *       break;
   *     }
   *   }
   * }
   * </pre>
   */
  @Test
  public void testInlineJsr_implicitExit() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // While loop header.
                    .aconst_null()
                    .ifnonnull(label5)
                    .label(label1) // Try block.
                    .iinc(1, 1)
                    .jsr(label3)
                    .go(label4)
                    .label(label2) // Implicit catch block.
                    .astore(2)
                    .jsr(label3)
                    .aload(2)
                    .athrow()
                    .label(label3) // Subroutine ...
                    .astore(3)
                    .iinc(1, 2)
                    .go(label5) // ... note that it does not return!
                    .label(label4) // End of the loop... goes back to the top!
                    .go(label1)
                    .label(label5)
                    .vreturn()
                    .trycatch(label1, label2, label2)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // While loop header.
                    .aconst_null()
                    .ifnonnull(expectedLabel6)
                    .label(expectedLabel1) // While loop header/try block.
                    .iinc(1, 1)
                    .aconst_null()
                    .go(expectedLabel7)
                    .label(expectedLabel2)
                    .go(expectedLabel5)
                    .label(expectedLabel3) // Implicit catch block.
                    .astore(2)
                    .aconst_null()
                    .go(expectedLabel8)
                    .label(expectedLabel4)
                    .aload(2)
                    .athrow()
                    .label(expectedLabel5) // End of the loop... goes back to the top!
                    .go(expectedLabel1)
                    .label(expectedLabel6) // Exit, not part of subroutine.
                    // Note that the two subroutine instantiations branch here.
                    .vreturn()
                    .label(expectedLabel7) // First instantiation of subroutine ...
                    .astore(3)
                    .iinc(1, 2)
                    .go(expectedLabel6) // ... note that it does not return!
                    .label()
                    .label(expectedLabel8) // Second instantiation of subroutine ...
                    .astore(3)
                    .iinc(1, 2)
                    .go(expectedLabel6) // ... note that it does not return!
                    .label()
                    .trycatch(expectedLabel1, expectedLabel3, expectedLabel3)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * Tests a nested try/finally with implicit exit from one subroutine to the other subroutine.
   * Equivalent to the following java code:
   *
   * <pre>
   * void m(boolean b) {
   *   try {
   *     return;
   *   } finally {
   *     while (b) {
   *       try {
   *         return;
   *       } finally {
   *         // NOTE --- this break avoids the second return above (weird)
   *         if (b) {
   *           break;
   *         }
   *       }
   *     }
   *   }
   * }
   * </pre>
   *
   * <p>This example is from the paper, "Subroutine Inlining and Bytecode Abstraction to Simplify
   * Static and Dynamic Analysis" by Cyrille Artho and Armin Biere.
   */
  @Test
  public void testInlineJsr_implicitExitToAnotherSubroutine() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 6)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // First try.
                    .jsr(label2)
                    .vreturn()
                    .label(label1) // Exception handler for first try.
                    .astore(LOCAL2)
                    .jsr(label2)
                    .aload(LOCAL2)
                    .athrow()
                    .label(label2) // First finally handler.
                    .astore(LOCAL4)
                    .go(label6)
                    .label(label3) // Body of while loop, also second try.
                    .jsr(label5)
                    .vreturn()
                    .label(label4) // Exception handler for second try.
                    .astore(LOCAL3)
                    .jsr(label5)
                    .aload(LOCAL3)
                    .athrow()
                    .label(label5) // Second finally handler.
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(label7)
                    .ret(LOCAL5)
                    .label(label6) // Test for the while loop.
                    .iload(LOCAL1)
                    .ifne(label3) // Falls through.
                    .label(label7) // Exit from finally block.
                    .ret(LOCAL4)
                    .trycatch(label0, label1, label1)
                    .trycatch(label3, label4, label4)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 6)
                    // --- Main Subroutine ---
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // First try.
                    .aconst_null()
                    .go(expectedLabel4)
                    .label(expectedLabel1)
                    .vreturn()
                    .label(expectedLabel2) // Exception handler for first try.
                    .astore(LOCAL2)
                    .aconst_null()
                    .go(expectedLabel11)
                    .label(expectedLabel3)
                    .aload(LOCAL2)
                    .athrow()
                    .label()
                    // --- First instantiation of first subroutine ---
                    .label(expectedLabel4) // First finally handler.
                    .astore(LOCAL4)
                    .go(expectedLabel9)
                    .label(expectedLabel5) // Body of while loop, also second try.
                    .aconst_null()
                    .go(expectedLabel18)
                    .label(expectedLabel6)
                    .vreturn()
                    .label(expectedLabel7) // Exception handler for second try.
                    .astore(LOCAL3)
                    .aconst_null()
                    .go(expectedLabel19)
                    .label(expectedLabel8)
                    .aload(LOCAL3)
                    .athrow()
                    .label(expectedLabel9) // Test for the while loop.
                    .iload(LOCAL1)
                    .ifne(expectedLabel5) // Falls through.
                    .label(expectedLabel10) // Exit from finally block.
                    .go(expectedLabel1)
                    // --- Second instantiation of first subroutine ---
                    .label(expectedLabel11) // First finally handler.
                    .astore(LOCAL4)
                    .go(expectedLabel16)
                    .label(expectedLabel12) // Body of while loop, also second try.
                    .aconst_null()
                    .go(expectedLabel20)
                    .label(expectedLabel13)
                    .vreturn()
                    .label(expectedLabel14) // Exception handler for second try.
                    .astore(LOCAL3)
                    .aconst_null()
                    .go(expectedLabel21)
                    .label(expectedLabel15)
                    .aload(LOCAL3)
                    .athrow()
                    .label(expectedLabel16) // Test for the while loop.
                    .iload(LOCAL1)
                    .ifne(expectedLabel12) // Falls through.
                    .label(expectedLabel17) // Exit from finally block.
                    .go(expectedLabel3)
                    // --- Second subroutine's 4 instantiations ---
                    .label(expectedLabel18) // First instantiation.
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel10)
                    .go(expectedLabel6)
                    .label()
                    .label(expectedLabel19) // Second instantiation.
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel10)
                    .go(expectedLabel8)
                    .label()
                    .label(expectedLabel20) // Third instantiation.
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel17)
                    .go(expectedLabel13)
                    .label()
                    .label(expectedLabel21) // Fourth instantiation.
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel17)
                    .go(expectedLabel15)
                    .label()
                    .trycatch(expectedLabel0, expectedLabel2, expectedLabel2)
                    .trycatch(expectedLabel5, expectedLabel7, expectedLabel7)
                    .trycatch(expectedLabel12, expectedLabel14, expectedLabel14)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * This tests two subroutines, neither of which exit. Instead, they both branch to a common set of
   * code which returns from the method. This code is not reachable except through these
   * subroutines, and since they do not invoke each other, it must be copied into both of them.
   *
   * <p>I don't believe this can be represented in Java.
   */
  @Test
  public void testInlineJsr_commonCodeWhichMustBeDuplicated() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 2)
                    .iconst_0()
                    .istore(1)
                    // Invoke the two subroutines, each twice.
                    .jsr(label0)
                    .jsr(label0)
                    .jsr(label1)
                    .jsr(label1)
                    .vreturn()
                    .label(label0) // Subroutine 1.
                    .iinc(1, 1)
                    .go(label2) // ... note that it does not return!
                    .label(label1) // Subroutine 2.
                    .iinc(1, 2)
                    .go(label2) // ... note that it does not return!
                    .label(label2) // Common code to both subroutines: exit method.
                    .vreturn()
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 2)
                    .iconst_0()
                    .istore(1)
                    // Invoke the two subroutines, each twice.
                    .aconst_null()
                    .go(expectedLabel4)
                    .label(expectedLabel0)
                    .aconst_null()
                    .go(expectedLabel6)
                    .label(expectedLabel1)
                    .aconst_null()
                    .go(expectedLabel8)
                    .label(expectedLabel2)
                    .aconst_null()
                    .go(expectedLabel10)
                    .label(expectedLabel3)
                    .vreturn()
                    .label()
                    .label(expectedLabel4) // Instantiation 1 of subroutine 1.
                    .iinc(1, 1)
                    .go(expectedLabel5) // ... note that it does not return!
                    .label(expectedLabel5)
                    .vreturn()
                    .label(expectedLabel6) // Instantiation 2 of subroutine 1.
                    .iinc(1, 1)
                    .go(expectedLabel7) // ... note that it does not return!
                    .label(expectedLabel7)
                    .vreturn()
                    .label(expectedLabel8) // Instantiation 1 of subroutine 2.
                    .iinc(1, 2)
                    .go(expectedLabel9) // ...note that it does not return!
                    .label(expectedLabel9)
                    .vreturn()
                    .label(expectedLabel10) // Instantiation 2 of subroutine 2.
                    .iinc(1, 2)
                    .go(expectedLabel11) // ... note that it does not return!
                    .label(expectedLabel11)
                    .vreturn()
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * This tests a simple subroutine where the control flow jumps back and forth between the
   * subroutine and the caller.
   *
   * <p>This would not normally be produced by a java compiler.
   */
  @Test
  public void testInlineJsr_interleavedCode() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 3)
                    .iconst_0()
                    .istore(1)
                    // Invoke the subroutine, each twice.
                    .jsr(label0)
                    .go(label1)
                    .label(label0) // Subroutine 1.
                    .astore(2)
                    .iinc(1, 1)
                    .go(label2)
                    .label(label1) // Second part of main subroutine.
                    .iinc(1, 2)
                    .go(label3)
                    .label(label2) // Second part of subroutine 1.
                    .iinc(1, 4)
                    .ret(2)
                    .label(label3) // Third part of main subroutine.
                    .jsr(label0)
                    .vreturn()
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 3)
                    // Main routine.
                    .iconst_0()
                    .istore(1)
                    .aconst_null()
                    .go(expectedLabel4)
                    .label(expectedLabel0)
                    .go(expectedLabel1)
                    .label(expectedLabel1)
                    .iinc(1, 2)
                    .go(expectedLabel2)
                    .label(expectedLabel2)
                    .aconst_null()
                    .go(expectedLabel6)
                    .label(expectedLabel3)
                    .vreturn()
                    .label(expectedLabel4) // Instantiation #1.
                    .astore(2)
                    .iinc(1, 1)
                    .go(expectedLabel5)
                    .label(expectedLabel5)
                    .iinc(1, 4)
                    .go(expectedLabel0)
                    .label()
                    .label(expectedLabel6) // Instantiation #2.
                    .astore(2)
                    .iinc(1, 1)
                    .go(expectedLabel7)
                    .label(expectedLabel7)
                    .iinc(1, 4)
                    .go(expectedLabel3)
                    .label()
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * Tests a nested try/finally with implicit exit from one subroutine to the other subroutine, and
   * with a surrounding try/catch thrown in the mix. Equivalent to the following java code:
   *
   * <pre>
   * void m(int b) {
   *   try {
   *     try {
   *       return;
   *     } finally {
   *       while (b) {
   *         try {
   *           return;
   *         } finally {
   *           // NOTE --- this break avoids the second return above (weird)
   *           if (b) {
   *             break;
   *           }
   *         }
   *       }
   *     }
   *   } catch (Exception e) {
   *     b += 3;
   *     return;
   *   }
   * }
   * </pre>
   */
  @Test
  public void testInlineJsr_implicitExitInTryCatch() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 6)
                    .iconst_0()
                    .istore(1)
                    .label(label0) // Outermost try.
                    .label(label1) // First try.
                    .jsr(label3)
                    .vreturn()
                    .label(label2) // Exception handler for first try.
                    .astore(LOCAL2)
                    .jsr(label3)
                    .aload(LOCAL2)
                    .athrow()
                    .label(label3) // First finally handler.
                    .astore(LOCAL4)
                    .go(label7)
                    .label(label4) // Body of while loop, also second try.
                    .jsr(label6)
                    .vreturn()
                    .label(label5) // Exception handler for second try.
                    .astore(LOCAL3)
                    .jsr(label6)
                    .aload(LOCAL3)
                    .athrow()
                    .label(label6) // Second finally handler.
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(label8)
                    .ret(LOCAL5)
                    .label(label7) // Test for the while loop.
                    .iload(LOCAL1)
                    .ifne(label4) // Falls through.
                    .label(label8) // Exit from finally block.
                    .ret(LOCAL4)
                    .label(label9) // Outermost catch.
                    .iinc(LOCAL1, 3)
                    .vreturn()
                    .trycatch(label1, label2, label2)
                    .trycatch(label4, label5, label5)
                    .trycatch(label0, label9, label9)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 6)
                    // --- Main Subroutine ---
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel0) // Outermost try / first try.
                    .aconst_null()
                    .go(expectedLabel5)
                    .label(expectedLabel1)
                    .vreturn()
                    .label(expectedLabel2) // Exception handler for first try.
                    .astore(LOCAL2)
                    .aconst_null()
                    .go(expectedLabel13)
                    .label(expectedLabel3)
                    .aload(LOCAL2)
                    .athrow()
                    .label(expectedLabel4) // Outermost catch.
                    .iinc(LOCAL1, 3)
                    .vreturn()
                    // --- First instantiation of first subroutine ---
                    .label(expectedLabel5) // First finally handler.
                    .astore(LOCAL4)
                    .go(expectedLabel10)
                    .label(expectedLabel6) // Body of while loop, also second try.
                    .aconst_null()
                    .go(expectedLabel21)
                    .label(expectedLabel7)
                    .vreturn()
                    .label(expectedLabel8) // Exception handler for second try.
                    .astore(LOCAL3)
                    .aconst_null()
                    .go(expectedLabel23)
                    .label(expectedLabel9)
                    .aload(LOCAL3)
                    .athrow()
                    .label(expectedLabel10) // Test for the while loop.
                    .iload(LOCAL1)
                    .ifne(expectedLabel6) // Falls through.
                    .label(expectedLabel11) // Exit from finally block.
                    .go(expectedLabel1)
                    .label(expectedLabel12)
                    // --- Second instantiation of first subroutine ---
                    .label(expectedLabel13) // First finally handler.
                    .astore(LOCAL4)
                    .go(expectedLabel18)
                    .label(expectedLabel14) // Body of while loop, also second try.
                    .aconst_null()
                    .go(expectedLabel25)
                    .label(expectedLabel15)
                    .vreturn()
                    .label(expectedLabel16) // Exception handler for second try.
                    .astore(LOCAL3)
                    .aconst_null()
                    .go(expectedLabel27)
                    .label(expectedLabel17)
                    .aload(LOCAL3)
                    .athrow()
                    .label(expectedLabel18) // Test for the while loop.
                    .iload(LOCAL1)
                    .ifne(expectedLabel14) // Falls through.
                    .label(expectedLabel19) // Exit from finally block.
                    .go(expectedLabel3)
                    .label(expectedLabel20)
                    // --- Second subroutine's 4 instantiations ---
                    .label(expectedLabel21)
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel11)
                    .go(expectedLabel7)
                    .label(expectedLabel22)
                    .label(expectedLabel23)
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel11)
                    .go(expectedLabel9)
                    .label(expectedLabel24)
                    .label(expectedLabel25)
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel19)
                    .go(expectedLabel15)
                    .label(expectedLabel26)
                    .label(expectedLabel27)
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(expectedLabel19)
                    .go(expectedLabel17)
                    .label(expectedLabel28)
                    // Main subroutine handlers.
                    .trycatch(expectedLabel0, expectedLabel2, expectedLabel2)
                    .trycatch(expectedLabel0, expectedLabel4, expectedLabel4)
                    // First instance of first subroutine try/catch handlers.
                    // Note: reuses handler code from main subroutine.
                    .trycatch(expectedLabel6, expectedLabel8, expectedLabel8)
                    .trycatch(expectedLabel5, expectedLabel12, expectedLabel4)
                    // Second instance of first sub try/catch handlers.
                    .trycatch(expectedLabel14, expectedLabel16, expectedLabel16)
                    .trycatch(expectedLabel13, expectedLabel20, expectedLabel4)
                    // All 4 instances of second subroutine.
                    .trycatch(expectedLabel21, expectedLabel22, expectedLabel4)
                    .trycatch(expectedLabel23, expectedLabel24, expectedLabel4)
                    .trycatch(expectedLabel25, expectedLabel26, expectedLabel4)
                    .trycatch(expectedLabel27, expectedLabel28, expectedLabel4)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * Tests an example coming from distilled down version of
   * com/sun/corba/ee/impl/protocol/CorbaClientDelegateImpl from GlassFish 2. See issue #317823.
   */
  @Test
  public void testInlineJsr_glassFish2CorbaClientDelegateImplExample() {
    MethodNode inputMethod =
            new MethodNodeBuilder(3, 3)
                    .label(label0)
                    .jsr(label4)
                    .label(label1)
                    .go(label5)
                    .label(label2)
                    .pop()
                    .jsr(label4)
                    .label(label3)
                    .aconst_null()
                    .athrow()
                    .label(label4)
                    .astore(1)
                    .ret(1)
                    .label(label5)
                    .aconst_null()
                    .aconst_null()
                    .aconst_null()
                    .pop()
                    .pop()
                    .pop()
                    .label(label6)
                    .go(label8)
                    .label(label7)
                    .pop()
                    .go(label8)
                    .aconst_null()
                    .athrow()
                    .label(label8)
                    .iconst_0()
                    .ifne(label0)
                    .jsr(label12)
                    .label(label9)
                    .vreturn()
                    .label(label10)
                    .pop()
                    .jsr(label12)
                    .label(label11)
                    .aconst_null()
                    .athrow()
                    .label(label12)
                    .astore(2)
                    .ret(2)
                    .trycatch(label0, label1, label2)
                    .trycatch(label2, label3, label2)
                    .trycatch(label0, label6, label7)
                    .trycatch(label0, label9, label10)
                    .trycatch(label10, label11, label10)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(3, 3)
                    // --- Main Subroutine ---
                    .label(expectedLabel0)
                    .aconst_null()
                    .go(expectedLabel15)
                    .label(expectedLabel1)
                    .label(expectedLabel2)
                    .go(expectedLabel6)
                    .label(expectedLabel3)
                    .pop()
                    .aconst_null()
                    .go(expectedLabel17)
                    .label(expectedLabel4)
                    .label(expectedLabel5)
                    .aconst_null()
                    .athrow()
                    .label(expectedLabel6)
                    .aconst_null()
                    .aconst_null()
                    .aconst_null()
                    .pop()
                    .pop()
                    .pop()
                    .label(expectedLabel7)
                    .go(expectedLabel9)
                    .label(expectedLabel8)
                    .pop()
                    .go(expectedLabel9)
                    // [some dead code skipped here]
                    .label(expectedLabel9)
                    .iconst_0()
                    .ifne(expectedLabel0)
                    .aconst_null()
                    .go(expectedLabel19)
                    .label(expectedLabel10)
                    .label(expectedLabel11)
                    .vreturn()
                    .label(expectedLabel12)
                    .pop()
                    .aconst_null()
                    .go(expectedLabel20)
                    .label(expectedLabel13)
                    .label(expectedLabel14)
                    .aconst_null()
                    .athrow()
                    // --- First instantiation of first subroutine ---
                    .label()
                    .label(expectedLabel15)
                    .astore(1)
                    .go(expectedLabel1)
                    .label(expectedLabel16)
                    // --- Second instantiation of first subroutine ---
                    .label(expectedLabel17)
                    .astore(1)
                    .go(expectedLabel4)
                    .label(expectedLabel18)
                    // --- First instantiation of second subroutine ---
                    .label(expectedLabel19)
                    .astore(2)
                    .go(expectedLabel10)
                    // --- Second instantiation of second subroutine ---
                    .label(expectedLabel20)
                    .astore(2)
                    .go(expectedLabel13)
                    .trycatch(expectedLabel0, expectedLabel2, expectedLabel3)
                    .trycatch(expectedLabel3, expectedLabel5, expectedLabel3)
                    .trycatch(expectedLabel0, expectedLabel7, expectedLabel8)
                    .trycatch(expectedLabel0, expectedLabel11, expectedLabel12)
                    .trycatch(expectedLabel12, expectedLabel14, expectedLabel12)
                    .trycatch(expectedLabel15, expectedLabel16, expectedLabel8)
                    .trycatch(expectedLabel15, expectedLabel16, expectedLabel12)
                    .trycatch(expectedLabel17, expectedLabel18, expectedLabel8)
                    .trycatch(expectedLabel17, expectedLabel18, expectedLabel12)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /**
   * Tests a method which has line numbers and local variable declarations. More specifically:
   *
   * <pre>
   *   public void a() {
   * 1   int a = 0;
   * 2   try {
   * 3     a++;
   * 4   } finally {
   * 5     a--;
   * 6   }
   *   }
   *   LV "a" from 1 to 6
   * </pre>
   */
  @Test
  public void testInlineJsr_basicLineNumberAndLocalVars() {
    MethodNode inputMethod =
            new MethodNodeBuilder(1, 4)
                    .label(label0)
                    .line(1, label0)
                    .iconst_0()
                    .istore(1)
                    .label(label1) // Body of try block.
                    .line(3, label1)
                    .iinc(1, 1)
                    .go(label4)
                    .label(label2) // Exception handler.
                    .astore(3)
                    .jsr(label3)
                    .aload(3)
                    .athrow()
                    .label(label3) // Subroutine.
                    .line(5, label3)
                    .astore(2)
                    .iinc(1, -1)
                    .ret(2)
                    .label(label4) // Non-exceptional exit from try block.
                    .jsr(label3)
                    .label(label5)
                    .vreturn()
                    .trycatch(label1, label2, label2)
                    .trycatch(label4, label5, label2)
                    .localvar("a", "I", 1, label0, label5)
                    .build();

    MethodNode inlinedMethod = new MethodNode(Opcodes.ACC_PUBLIC, "m", "()V", null, null);
    inputMethod.accept(
            new JSRInlinerAdapter(inlinedMethod, Opcodes.ACC_PUBLIC, "m", "()V", null, null));

    MethodNode expectedMethod =
            new MethodNodeBuilder(1, 4)
                    .label(expectedLabel0)
                    .line(1, expectedLabel0)
                    .iconst_0()
                    .istore(1)
                    .label(expectedLabel1) // Try/catch block.
                    .line(3, expectedLabel1)
                    .iinc(1, 1)
                    .go(expectedLabel4)
                    .label(expectedLabel2) // Exception handler.
                    .astore(3)
                    .aconst_null()
                    .go(expectedLabel7)
                    .label(expectedLabel3)
                    .aload(3)
                    .athrow()
                    .label(expectedLabel4) // On non-exceptional exit, try block leads here.
                    .aconst_null()
                    .go(expectedLabel9)
                    .label(expectedLabel5)
                    .label(expectedLabel6)
                    .vreturn()
                    .label(expectedLabel7) // First instantiation of subroutine.
                    .line(5, expectedLabel7)
                    .astore(2)
                    .iinc(1, -1)
                    .go(expectedLabel3)
                    .label(expectedLabel8)
                    .label(expectedLabel9) // Second instantiation of subroutine.
                    .line(5, expectedLabel9)
                    .astore(2)
                    .iinc(1, -1)
                    .go(expectedLabel5)
                    .label(expectedLabel10)
                    .trycatch(expectedLabel1, expectedLabel2, expectedLabel2)
                    .trycatch(expectedLabel4, expectedLabel6, expectedLabel2)
                    .localvar("a", "I", 1, expectedLabel0, expectedLabel6)
                    .localvar("a", "I", 1, expectedLabel7, expectedLabel8)
                    .localvar("a", "I", 1, expectedLabel9, expectedLabel10)
                    .build();
    assertEquals(toText(expectedMethod), toText(inlinedMethod));
    assertDoesNotThrow(() -> buildClassWithMethod(inlinedMethod).newInstance());
  }

  /** Tests that classes transformed with JSRInlinerAdapter can be loaded and instantiated. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testInlineJsr_precompiledClass(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(new JsrInlinerClassAdapter(classWriter), 0);

    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(
              UnsupportedClassVersionError.class,
              () -> new ClassFile(classWriter.toByteArray()).newInstance());
    }
    else {
      assertDoesNotThrow(() -> new ClassFile(classWriter.toByteArray()).newInstance());
    }
  }

  static class JsrInlinerClassAdapter extends ClassVisitor {

    JsrInlinerClassAdapter(final ClassVisitor classVisitor) {
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
      return new JSRInlinerAdapter(methodVisitor, access, name, descriptor, signature, exceptions) { };
    }
  }
}
