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

package cn.taketoday.core.bytecode.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassWriter;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.core.bytecode.tree.LabelNode;
import cn.taketoday.core.bytecode.tree.MethodNode;
import cn.taketoday.core.bytecode.tree.analysis.Analyzer;
import cn.taketoday.core.bytecode.tree.analysis.AnalyzerException;
import cn.taketoday.core.bytecode.tree.analysis.BasicValue;
import cn.taketoday.core.bytecode.tree.analysis.BasicVerifier;
import cn.taketoday.core.bytecode.tree.analysis.Frame;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/24 11:12
 */
class CheckFrameAnalyzerTests extends AsmTest {

  private static final String CLASS_NAME = "C";

  // Labels used to generate test cases.
  private final Label label0 = new Label();

  @Test
  void testAnalyze_invalidJsr() {
    MethodNode methodNode = new MethodNodeBuilder().jsr(label0).label(label0).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Error at instruction 0: JSR instructions are unsupported"));
  }

  @Test
  void testAnalyze_invalidRet() {
    MethodNode methodNode = new MethodNodeBuilder().ret(0).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Error at instruction 0: RET instructions are unsupported"));
  }

  @Test
  void testAnalyze_missingFrameAtJumpTarget() {
    MethodNode methodNode =
            new MethodNodeBuilder().iconst_0().ifne(label0).label(label0).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains("Error at instruction 1: Expected stack map frame at instruction 2"));
  }

  @Test
  void testAnalyze_missingFrameAfterGoto() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .nop()
                    .go(label0)
                    .nop()
                    .label(label0)
                    .frame(Opcodes.F_SAME, null, null)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains("Error at instruction 1: Expected stack map frame at instruction 2"));
  }

  @Test
  void testAnalyze_illegalFrameType() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .nop()
                    .go(label0)
                    .frame(123456, null, null)
                    .nop()
                    .label(label0)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Error at instruction 2: Illegal frame type 123456"));
  }

  @Test
  void testAnalyze_invalidAppendFrame() {
    MethodNode methodNode =
            new MethodNodeBuilder(/* maxStack = */ 0, /* maxLocals = */ 1)
                    .nop()
                    .frame(Opcodes.F_APPEND, new Object[] { Opcodes.INTEGER }, null)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains("Error at instruction 1: Cannot append more locals than maxLocals"));
  }

  @Test
  void testAnalyze_invalidChopFrame() {
    MethodNode methodNode =
            new MethodNodeBuilder(/* maxStack = */ 0, /* maxLocals = */ 1)
                    .nop()
                    .frame(Opcodes.F_CHOP, new Object[] { null, null }, null)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Error at instruction 1: Cannot chop more locals than defined"));
  }

  @Test
  void testAnalyze_illegalStackMapFrameValue() {
    MethodNode methodNode =
            new MethodNodeBuilder(/* maxStack = */ 0, /* maxLocals = */ 2)
                    .nop()
                    .frame(Opcodes.F_APPEND, new Object[] { new Object() }, null)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains("Error at instruction 1: Illegal stack map frame value java.lang.Object"));
  }

  @Test
  void testAnalyze_illegalLabelNodeStackMapFrameValue() {
    MethodNode methodNode =
            new MethodNodeBuilder(/* maxStack = */ 0, /* maxLocals = */ 2)
                    .nop()
                    .frame(Opcodes.F_APPEND, new Object[] { new LabelNode(label0) }, null)
                    .label(label0)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains("Error at instruction 1: LabelNode does not designate a NEW instruction"));
  }

  @Test
  void testAnalyze_frameAtJumpTargetHasIncompatibleStackHeight() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .iconst_0()
                    .ifne(label0)
                    .iconst_0()
                    .label(label0)
                    .frame(Opcodes.F_SAME1, null, new Object[] { Opcodes.INTEGER })
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains(
                    "Error at instruction 1: Stack map frame incompatible with frame at instruction 3 "
                            + "(incompatible stack heights)"));
  }

  @Test
  void testAnalyze_frameAtJumpTargetHasIncompatibleLocalValues() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .iconst_0()
                    .ifne(label0)
                    .iconst_0()
                    .label(label0)
                    .frame(Opcodes.F_NEW, new Object[] { Opcodes.INTEGER }, null)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains(
                    "Error at instruction 1: Stack map frame incompatible with frame at instruction 3 "
                            + "(incompatible types at local 0: R and I)"));
  }

  @Test
  void testAnalyze_frameAtJumpTargetHasIncompatibleStackValues() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .iconst_0()
                    .iconst_0()
                    .ifne(label0)
                    .iconst_0()
                    .iconst_0()
                    .label(label0)
                    .frame(Opcodes.F_NEW, new Object[] { "C" }, new Object[] { "C" })
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains(
                    "Error at instruction 2: Stack map frame incompatible with frame at instruction 5 "
                            + "(incompatible types at stack item 0: I and R)"));
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed from their existing stack map
   * frames with a BasicVerifier.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  void testAnalyze_basicVerifier(final PrecompiledClass classParameter, final Api apiParameter)
          throws AnalyzerException {
    assumeFalse(hasJsrOrRetInstructions(classParameter));
    ClassNode classNode = computeFrames(classParameter);
    Analyzer<BasicValue> analyzer = newAnalyzer();

    ArrayList<Frame<? extends BasicValue>[]> methodFrames = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      Frame<? extends BasicValue>[] result = analyzer.analyze(classNode.name, methodNode);
      methodFrames.add(result);
    }

    for (int i = 0; i < classNode.methods.size(); ++i) {
      Frame<? extends BasicValue>[] frames = methodFrames.get(i);
      for (int j = 0; j < lastJvmInsnIndex(classNode.methods.get(i)); ++j) {
        assertNotNull(frames[j]);
      }
    }
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed from their existing stack map
   * frames with a BasicVerifier, even if the method node's max locals and max stack size are not
   * set.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  void testAnalyzeAndComputeMaxs_basicVerifier(
          final PrecompiledClass classParameter, final Api apiParameter) throws AnalyzerException {
    assumeFalse(hasJsrOrRetInstructions(classParameter));
    ClassNode classNode = computeFrames(classParameter);
    ArrayList<MethodMaxs> methodMaxs = MethodMaxs.getAndClear(classNode);
    Analyzer<BasicValue> analyzer = newAnalyzer();

    ArrayList<MethodMaxs> analyzedMethodMaxs = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      analyzer.analyzeAndComputeMaxs(classNode.name, methodNode);
      analyzedMethodMaxs.add(new MethodMaxs(methodNode.maxStack, methodNode.maxLocals));
    }

    for (int i = 0; i < analyzedMethodMaxs.size(); ++i) {
      assertTrue(analyzedMethodMaxs.get(i).maxLocals >= methodMaxs.get(i).maxLocals);
      assertTrue(analyzedMethodMaxs.get(i).maxStack >= methodMaxs.get(i).maxStack);
    }
  }

  private static boolean hasJsrOrRetInstructions(final PrecompiledClass classParameter) {
    return classParameter == PrecompiledClass.JDK3_ALL_INSTRUCTIONS
            || classParameter == PrecompiledClass.JDK3_LARGE_METHOD;
  }

  private static ClassNode computeFrames(final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    classReader.accept(classWriter, 0);
    classFile = classWriter.toByteArray();
    ClassNode classNode = new ClassNode();
    new ClassReader(classFile).accept(classNode, 0);
    return classNode;
  }

  private static Analyzer<BasicValue> newAnalyzer() {
    return new CheckFrameAnalyzer<>(new BasicVerifier());
  }

  private static int lastJvmInsnIndex(final MethodNode method) {
    for (int i = method.instructions.size() - 1; i >= 0; --i) {
      if (method.instructions.get(i).getOpcode() >= 0) {
        return i;
      }
    }
    return -1;
  }

  private static class MethodMaxs {

    final int maxStack;
    final int maxLocals;

    MethodMaxs(final int maxStack, final int maxLocals) {
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }

    static ArrayList<MethodMaxs> getAndClear(final ClassNode classNode) {
      ArrayList<MethodMaxs> methodMaxs = new ArrayList<>();
      for (MethodNode methodNode : classNode.methods) {
        methodMaxs.add(new MethodMaxs(methodNode.maxStack, methodNode.maxLocals));
        methodNode.maxLocals = 0;
        methodNode.maxStack = 0;
      }
      return methodMaxs;
    }
  }
}
