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
package cn.taketoday.bytecode.tree.analysis;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.List;

import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.AbstractInsnNode;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.AsmTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Analyzer}.
 *
 * @author Eric Bruneton
 */
public class AnalyzerTest extends AsmTest {

  private static final String CLASS_NAME = "C";

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

  @Test
  public void testAnalyze_invalidOpcode() {
    MethodNode methodNode = new MethodNodeBuilder().insn(-1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal opcode -1"));
  }

  @Test
  public void testAnalyze_invalidPop() {
    MethodNode methodNode =
            new MethodNodeBuilder().insn(Opcodes.LCONST_0).insn(Opcodes.POP).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of POP"));
  }

  @Test
  public void testAnalyze_invalidPop2() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.POP2)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of POP2"));
  }

  @Test
  public void testAnalyze_invalidDup() {
    MethodNode methodNode =
            new MethodNodeBuilder().insn(Opcodes.LCONST_0).insn(Opcodes.DUP).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of DUP"));
  }

  @Test
  public void testAnalyze_invalidDupx1() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.DUP_X1)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of DUP_X1"));
  }

  @Test
  public void testAnalyze_invalidDupx2() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.DUP_X2)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of DUP_X2"));
  }

  @Test
  public void testAnalyze_invalidDup2() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.DUP2)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of DUP2"));
  }

  @Test
  public void testAnalyze_invalidDup2x1() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.DUP2_X1)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of DUP2_X1"));
  }

  @Test
  public void testAnalyze_invalidDup2x2() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.DUP2_X2)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of DUP2_X2"));
  }

  @Test
  public void testAnalyze_invalidSwap() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.SWAP)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Illegal use of SWAP"));
  }

  @Test
  public void testAnalyze_invalidGetLocal() {
    MethodNode methodNode = new MethodNodeBuilder().aload(10).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Trying to get an inexistant local variable"));
  }

  @Test
  public void testAnalyze_invalidSetLocal() {
    MethodNode methodNode = new MethodNodeBuilder().aconst_null().astore(10).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Trying to set an inexistant local variable"));
  }

  @Test
  public void testAnalyze_invalidPopFromEmptyStack() {
    MethodNode methodNode = new MethodNodeBuilder().insn(Opcodes.POP).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Cannot pop operand off an empty stack."));
  }

  @Test
  public void testAnalyze_invalidPushOnFullStack() {
    MethodNode methodNode =
            new MethodNodeBuilder(3, 3).iconst_0().iconst_0().iconst_0().iconst_0().vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Insufficient maximum stack size."));
  }

  @Test
  public void testAnalyze_inconsistentStackHeights() {
    Label ifLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .iconst_0()
                    .ifne(ifLabel)
                    .iconst_0()
                    .label(ifLabel)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Incompatible stack heights"));
  }

  @Test
  public void testAnalyze_invalidRet() {
    MethodNode methodNode = new MethodNodeBuilder().ret(1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("RET instruction outside of a subroutine"));
  }

  @Test
  public void testAnalyze_invalidFalloffEndOfMethod() {
    MethodNode methodNode = new MethodNodeBuilder().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Execution can fall off the end of the code"));
  }

  @Test
  public void testAnalyze_invalidFalloffSubroutine() {
    Label gotoLabel = new Label();
    Label jsrLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .go(gotoLabel)
                    .label(jsrLabel)
                    .astore(1)
                    .ret(1)
                    .label(gotoLabel)
                    .jsr(jsrLabel)
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Execution can fall off the end of the code"));
  }

  @Disabled("TODO currently Analyzer can not detect this situation")
  @Test
  public void testAnalyze_invalidOverlappingSubroutines() {
    // The problem is that other overlapping subroutine situations are valid, such as
    // when a nested subroutine implicitly returns to its parent subroutine, without a RET.
    Label subroutine1Label = new Label();
    Label subroutine2Label = new Label();
    Label endSubroutineLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .jsr(subroutine1Label)
                    .jsr(subroutine2Label)
                    .vreturn()
                    .label(subroutine1Label)
                    .astore(1)
                    .go(endSubroutineLabel)
                    .label(subroutine2Label)
                    .astore(1)
                    .label(endSubroutineLabel)
                    .ret(1)
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    assertThrows(AnalyzerException.class, analyze);
  }

  /**
   * Tests a method which has the most basic <code>try{}finally{}</code> form imaginable. That is:
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
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_basicTryFinally() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(4, 4)
                    .iconst_0()
                    .istore(1)
                    // Body of try block.
                    .label(label0)
                    .iinc(1, 1)
                    .go(label3)
                    // Exception handler.
                    .label(label1)
                    .astore(3)
                    .jsr(label2)
                    .aload(3)
                    .athrow()
                    // Subroutine.
                    .label(label2)
                    .astore(2)
                    .iinc(1, -1)
                    .push()
                    .push()
                    .ret(2)
                    // Non-exceptional exit from try block.
                    .label(label3)
                    .jsr(label2)
                    .push()
                    .push()
                    .label(label4)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label3, label4, label1)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  /**
   * Tests a method which has an if/else-if w/in the finally clause. More specifically:
   *
   * <pre>
   * public void a() {
   *   int a = 0;
   *   try {
   *       a++;
   *   } finally {
   *     if (a == 0) {
   *       a += 2;
   *     } else {
   *       a += 3;
   *     }
   *   }
   * }
   * </pre>
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_ifElseInFinally() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(5, 4)
                    .iconst_0()
                    .istore(1)
                    // Body of try block.
                    .label(label0)
                    .iinc(1, 1)
                    .go(label5)
                    // Exception handler.
                    .label(label1)
                    .astore(3)
                    .jsr(label2)
                    .push()
                    .push()
                    .aload(3)
                    .athrow()
                    // Subroutine.
                    .label(label2)
                    .astore(2)
                    .push()
                    .push()
                    .iload(1)
                    .ifne(label3)
                    .iinc(1, 2)
                    .go(label4)
                    .label(label3)
                    .iinc(1, 3)
                    .label(label4)
                    .ret(2)
                    // Non-exceptional exit from try block.
                    .label(label5)
                    .jsr(label2)
                    .label(label6)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label5, label6, label1)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
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
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_simpleNestedFinally() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(5, 6)
                    .iconst_0()
                    .istore(1)
                    // Body of try block.
                    .label(label0)
                    .iinc(1, 1)
                    .jsr(label2)
                    .go(label5)
                    // First exception handler.
                    .label(label1)
                    .astore(4)
                    .jsr(label2)
                    .aload(4)
                    .athrow()
                    // First subroutine.
                    .label(label2)
                    .astore(2)
                    .iinc(1, 2)
                    .jsr(label4)
                    .push()
                    .push()
                    .ret(2)
                    // Second exception handler.
                    .label(label3)
                    .astore(5)
                    .jsr(label4)
                    .aload(5)
                    .athrow()
                    // Second subroutine.
                    .label(label4)
                    .astore(3)
                    .push()
                    .push()
                    .iinc(1, 3)
                    .ret(3)
                    // On normal exit, try block jumps here.
                    .label(label5)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label2, label3, label3)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  @Test
  public void testAnalyze_nestedSubroutines() throws AnalyzerException {
    Label subroutine1Label = new Label();
    Label subroutine2Label = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder(1, 3)
                    .jsr(subroutine1Label)
                    .vreturn()
                    .label(subroutine1Label)
                    .astore(1)
                    .jsr(subroutine2Label)
                    .jsr(subroutine2Label)
                    .ret(1)
                    .label(subroutine2Label)
                    .astore(2)
                    .ret(2)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
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
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_subroutineWithNoRet() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    // While loop header/try block.
                    .label(label0)
                    .iinc(1, 1)
                    .jsr(label2)
                    .go(label3)
                    // Implicit catch block.
                    .label(label1)
                    .astore(2)
                    .jsr(label2)
                    .push()
                    .push()
                    .aload(2)
                    .athrow()
                    // Subroutine which does not return.
                    .label(label2)
                    .astore(3)
                    .iinc(1, 2)
                    .go(label4)
                    // End of the loop, goes back to the top.
                    .label(label3)
                    .go(label0)
                    .label(label4)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  /**
   * This tests a subroutine which has no ret statement, but ends in a "return" instead.
   *
   * <pre>
   *   aconst_null
   *   jsr l0
   * l0:
   *   astore 0
   *   astore 0
   *   return
   * </pre>
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_subroutineWithNoRet2() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(2, 2)
                    .aconst_null()
                    .jsr(label0)
                    .nop()
                    .label(label0)
                    .astore(0)
                    .astore(0)
                    .vreturn()
                    .label(label1)
                    .localVariable("i", "I", null, label0, label1, 1)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  @Test
  public void testAnalyze_subroutineLocalsAccess() throws AnalyzerException {
    Label startLabel = new Label();
    Label exceptionHandler1Label = new Label();
    Label exceptionHandler2Label = new Label();
    Label subroutineLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder(1, 5)
                    .label(startLabel)
                    .jsr(subroutineLabel)
                    .vreturn()
                    .label(exceptionHandler1Label)
                    .astore(1)
                    .jsr(subroutineLabel)
                    .aload(1)
                    .athrow()
                    .label(subroutineLabel)
                    .astore(2)
                    .aconst_null()
                    .astore(3)
                    .ret(2)
                    .label(exceptionHandler2Label)
                    .astore(4)
                    .aload(4)
                    .athrow()
                    .trycatch(startLabel, exceptionHandler1Label, exceptionHandler1Label)
                    .trycatch(
                            startLabel,
                            exceptionHandler2Label,
                            exceptionHandler2Label,
                            "java/lang/RuntimeException")
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
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
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_implicitExit() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    // While loop header.
                    .label(label0)
                    .aconst_null()
                    .ifnonnull(label5)
                    // Try block.
                    .label(label1)
                    .iinc(1, 1)
                    .jsr(label3)
                    .go(label4)
                    // Implicit catch block.
                    .label(label2)
                    .astore(2)
                    .jsr(label3)
                    .aload(2)
                    .push()
                    .push()
                    .athrow()
                    // Subroutine which does not return.
                    .label(label3)
                    .astore(3)
                    .iinc(1, 2)
                    .go(label5)
                    // End of the loop, goes back to the top.
                    .label(label4)
                    .go(label1)
                    .label(label5)
                    .vreturn()
                    .trycatch(label1, label2, label2)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
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
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_implicitExitToAnotherSubroutine() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(5, 6)
                    .iconst_0()
                    .istore(1)
                    // First try.
                    .label(label0)
                    .jsr(label2)
                    .vreturn()
                    // Exception handler for first try.
                    .label(label1)
                    .astore(LOCAL2)
                    .jsr(label2)
                    .push()
                    .push()
                    .aload(LOCAL2)
                    .athrow()
                    // First finally handler.
                    .label(label2)
                    .astore(LOCAL4)
                    .push()
                    .push()
                    .go(label6)
                    // Body of while loop, also second try.
                    .label(label3)
                    .jsr(label5)
                    .vreturn()
                    // Exception handler for second try.
                    .label(label4)
                    .astore(LOCAL3)
                    .push()
                    .push()
                    .jsr(label5)
                    .aload(LOCAL3)
                    .athrow()
                    // Second finally handler.
                    .label(label5)
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(label7)
                    .ret(LOCAL5)
                    // Test for the while loop.
                    .label(label6)
                    .iload(LOCAL1)
                    .ifne(label3)
                    // Exit from finally block.
                    .label(label7)
                    .ret(LOCAL4)
                    .trycatch(label0, label1, label1)
                    .trycatch(label3, label4, label4)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  @Test
  public void testanalyze_implicitExitToAnotherSubroutine2() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(1, 4)
                    .iconst_0()
                    .istore(1)
                    .jsr(label0)
                    .vreturn()
                    .label(label0)
                    .astore(2)
                    .jsr(label1)
                    .go(label2)
                    .label(label1)
                    .astore(3)
                    .iload(1)
                    .ifne(label2)
                    .ret(3)
                    .label(label2)
                    .ret(2)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  /**
   * This tests a simple subroutine where the control flow jumps back and forth between the
   * subroutine and the caller.
   *
   * <p>This would not normally be produced by a java compiler.
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_interleavedCode() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(4, 3)
                    .iconst_0()
                    .istore(1)
                    .jsr(label0)
                    .go(label1)
                    // Subroutine 1.
                    .label(label0)
                    .astore(2)
                    .iinc(1, 1)
                    .go(label2)
                    // Second part of main subroutine.
                    .label(label1)
                    .iinc(1, 2)
                    .go(label3)
                    // Second part of subroutine 1.
                    .label(label2)
                    .iinc(1, 4)
                    .push()
                    .push()
                    .ret(2)
                    // Third part of main subroutine.
                    .label(label3)
                    .push()
                    .push()
                    .vreturn()
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
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
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_implicitExitInTryCatch() throws AnalyzerException {
    MethodNode methodNode =
            new MethodNodeBuilder(4, 6)
                    .iconst_0()
                    .istore(1)
                    // First try.
                    .label(label0)
                    .jsr(label2)
                    .vreturn()
                    // Exception handler for first try.
                    .label(label1)
                    .astore(LOCAL2)
                    .jsr(label2)
                    .aload(LOCAL2)
                    .athrow()
                    // First finally handler.
                    .label(label2)
                    .astore(LOCAL4)
                    .go(label6)
                    // Body of while loop, also second try.
                    .label(label3)
                    .jsr(label5)
                    .push()
                    .push()
                    .vreturn()
                    // Exception handler for second try.
                    .label(label4)
                    .astore(LOCAL3)
                    .jsr(label5)
                    .aload(LOCAL3)
                    .athrow()
                    // Second finally handler.
                    .label(label5)
                    .astore(LOCAL5)
                    .iload(LOCAL1)
                    .ifne(label7)
                    .push()
                    .push()
                    .ret(LOCAL5)
                    // Test for the while loop.
                    .label(label6)
                    .iload(LOCAL1)
                    .ifne(label3)
                    // Exit from finally{} block.
                    .label(label7)
                    .ret(LOCAL4)
                    // Outermost catch.
                    .label(label8)
                    .iinc(LOCAL1, 3)
                    .vreturn()
                    .trycatch(label0, label1, label1)
                    .trycatch(label3, label4, label4)
                    .trycatch(label0, label8, label8)
                    .build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  /**
   * Tests that Analyzer works correctly on classes with many labels.
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_manyLabels() throws AnalyzerException {
    Label target = new Label();
    MethodNodeBuilder methodNodeBuilder = new MethodNodeBuilder(1, 1).jsr(target).label(target);
    for (int i = 0; i < 8192; i++) {
      Label label = new Label();
      methodNodeBuilder.go(label).label(label);
    }
    MethodNode methodNode = methodNodeBuilder.vreturn().build();

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  /**
   * Tests an example coming from distilled down version of
   * com/sun/corba/ee/impl/protocol/CorbaClientDelegateImpl from GlassFish 2. See issue #317823.
   *
   * @throws AnalyzerException if the test fails
   */
  @Test
  public void testAnalyze_glassFish2CorbaClientDelegateImplExample() throws AnalyzerException {
    MethodNode methodNode =
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

    Frame<?>[] frames = newAnalyzer().analyze(CLASS_NAME, methodNode);

    MethodMaxs methodMaxs = computeMaxStackAndLocalsFromFrames(frames);
    assertEquals(methodNode.maxStack, methodMaxs.maxStack);
    assertEquals(methodNode.maxLocals, methodMaxs.maxLocals);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  private static Analyzer<MockValue> newAnalyzer() {
    return new Analyzer<>(new MockInterpreter());
  }

  private static MethodMaxs computeMaxStackAndLocalsFromFrames(final Frame<?>[] frames) {
    int maxStack = 0;
    int maxLocals = 0;
    for (Frame<?> frame : frames) {
      if (frame != null) {
        int stackSize = 0;
        for (int i = 0; i < frame.getStackSize(); ++i) {
          stackSize += frame.getStack(i).getSize();
        }
        maxStack = Math.max(maxStack, stackSize);
        maxLocals = Math.max(maxLocals, frame.getLocals());
      }
    }
    return new MethodMaxs(maxStack, maxLocals);
  }

  private static class MethodMaxs {

    public final int maxStack;
    public final int maxLocals;

    public MethodMaxs(final int maxStack, final int maxLocals) {
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }
  }

  private static enum MockValue implements Value {
    INT,
    LONG,
    REFERENCE,
    RETURN_ADDRESS,
    TOP;

    @Override
    public int getSize() {
      return equals(LONG) ? 2 : 1;
    }
  }

  private static class MockInterpreter extends Interpreter<MockValue> {

    MockInterpreter() {
      super();
    }

    @Override
    public MockValue newValue(final Type type) {
      if (type == null) {
        return MockValue.TOP;
      }
      switch (type.getSort()) {
        case Type.VOID:
          return null;
        case Type.INT:
          return MockValue.INT;
        case Type.LONG:
          return MockValue.INT;
        case Type.OBJECT:
          return MockValue.REFERENCE;
        default:
          throw new UnsupportedOperationException();
      }
    }

    @Override
    public MockValue newOperation(final AbstractInsnNode insn) {
      switch (insn.getOpcode()) {
        case Opcodes.ACONST_NULL:
          return MockValue.REFERENCE;
        case Opcodes.ICONST_0:
          return MockValue.INT;
        case Opcodes.JSR:
          return MockValue.RETURN_ADDRESS;
        case Opcodes.LCONST_0:
          return MockValue.LONG;
        default:
          throw new UnsupportedOperationException();
      }
    }

    @Override
    public MockValue copyOperation(final AbstractInsnNode insn, final MockValue value) {
      return value;
    }

    @Override
    public MockValue unaryOperation(final AbstractInsnNode insn, final MockValue value) {
      switch (insn.getOpcode()) {
        case Opcodes.IFNE:
        case Opcodes.IFNONNULL:
        case Opcodes.ATHROW:
          return null;
        case Opcodes.IINC:
          return MockValue.INT;
        case Opcodes.NEWARRAY:
          return MockValue.REFERENCE;
        default:
          throw new UnsupportedOperationException();
      }
    }

    @Override
    public MockValue binaryOperation(
            final AbstractInsnNode insn, final MockValue value1, final MockValue value2) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MockValue ternaryOperation(
            final AbstractInsnNode insn,
            final MockValue value1,
            final MockValue value2,
            final MockValue value3) {
      throw new UnsupportedOperationException();
    }

    @Override
    public MockValue naryOperation(
            final AbstractInsnNode insn, final List<? extends MockValue> values) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void returnOperation(
            final AbstractInsnNode insn, final MockValue value, final MockValue expected) {
      // Nothing to do.
    }

    @Override
    public MockValue merge(final MockValue value1, final MockValue value2) {
      if (!value1.equals(value2)) {
        return MockValue.TOP;
      }
      return value1;
    }
  }
}
