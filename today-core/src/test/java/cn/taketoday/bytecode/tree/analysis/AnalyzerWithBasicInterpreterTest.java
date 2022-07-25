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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.tree.ClassNode;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link BasicInterpreter}.
 *
 * @author Eric Bruneton
 */
public class AnalyzerWithBasicInterpreterTest extends AsmTest {

  private static final String CLASS_NAME = "C";

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new BasicInterpreter());
  }

  @Test
  public void testAnalyze_invalidNewArray() {
    MethodNode methodNode =
            new MethodNodeBuilder().iconst_0().intInsn(Opcodes.NEWARRAY, -1).vreturn().build();

    Executable analyze =
            () -> new Analyzer<BasicValue>(new BasicInterpreter()).analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Invalid array type"));
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a BasicInterpreter, and
   * that Analyzer can be subclassed to use custom frames.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAnalyze_basicInterpreter(
          final PrecompiledClass classParameter) throws AnalyzerException {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    Analyzer<BasicValue> analyzer =
            new Analyzer<BasicValue>(new BasicInterpreter()) {
              @Override
              protected Frame<BasicValue> newFrame(final int numLocals, final int numStack) {
                return new CustomFrame(numLocals, numStack);
              }

              @Override
              protected Frame<BasicValue> newFrame(final Frame<? extends BasicValue> src) {
                return new CustomFrame(src);
              }
            };

    ArrayList<Frame<? extends BasicValue>[]> methodFrames = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      methodFrames.add(analyzer.analyze(classNode.name, methodNode));
    }

    for (Frame<? extends BasicValue>[] frames : methodFrames) {
      for (Frame<? extends BasicValue> frame : frames) {
        assertTrue(frame == null || frame instanceof CustomFrame);
      }
    }
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a BasicInterpreter, even
   * if the method node's max locals and max stack size are not set.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAnalyzeAndComputeMaxs_basicInterpreter(
          final PrecompiledClass classParameter) throws AnalyzerException {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    ArrayList<MethodMaxs> methodMaxs = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      methodMaxs.add(new MethodMaxs(methodNode.maxStack, methodNode.maxLocals));
      methodNode.maxLocals = 0;
      methodNode.maxStack = 0;
    }
    Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(new BasicInterpreter());

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

  /**
   * Tests that the analyzer does not loop infinitely, even if the {@link Interpreter#merge} method
   * does not follow its required contract (namely that if the merge result is equal to the first
   * argument, the first argument should be returned - see #316326).
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @Test
  public void testAnalyze_badInterpreter() {
    ClassNode classNode = new ClassNode();
    new ClassReader(PrecompiledClass.JDK8_ALL_FRAMES.getBytes()).accept(classNode, 0);
    Analyzer<BasicValue> analyzer =
            new Analyzer<BasicValue>(
                    new BasicInterpreter() {
                      @Override
                      public BasicValue merge(final BasicValue value1, final BasicValue value2) {
                        return new BasicValue(super.merge(value1, value2).getType());
                      }
                    });

    ArrayList<Executable> analyses = new ArrayList<>();
    for (MethodNode methodNode : classNode.methods) {
      analyses.add(() -> analyzer.analyze(CLASS_NAME, methodNode));
    }

    for (Executable analysis : analyses) {
      assertTimeoutPreemptively(ofSeconds(1), analysis);
    }
  }

  /**
   * Tests that stack map frames are correctly merged when a JSR instruction can be reached from two
   * different control flow paths, with different local variable types (#316204).
   *
   * @throws IOException if the test class can't be loaded.
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @Test
  public void testAnalyze_mergeWithJsrReachableFromTwoDifferentPaths()
          throws IOException, AnalyzerException {
    ClassReader classReader =
            new ClassReader(Files.newInputStream(Paths.get("src/test/resources/Issue316204.class")));
    ClassNode classNode = new ClassNode();
    classReader.accept(classNode, 0);
    Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());

    analyzer.analyze(classNode.name, getMethod(classNode, "basicStopBundles"));

    assertEquals("RIR..... ", analyzer.getFrames()[104].toString());
  }

  private static MethodNode getMethod(final ClassNode classNode, final String name) {
    for (MethodNode methodNode : classNode.methods) {
      if (methodNode.name.equals(name)) {
        return methodNode;
      }
    }
    return null;
  }

  private static class CustomFrame extends Frame<BasicValue> {

    CustomFrame(final int numLocals, final int numStack) {
      super(numLocals, numStack);
    }

    CustomFrame(final Frame<? extends BasicValue> frame) {
      super(frame);
    }

    @Override
    public Frame<BasicValue> init(final Frame<? extends BasicValue> frame) {
      assertTrue(frame instanceof CustomFrame);
      return super.init(frame);
    }
  }

  private static class MethodMaxs {

    public final int maxStack;
    public final int maxLocals;

    public MethodMaxs(final int maxStack, final int maxLocals) {
      this.maxStack = maxStack;
      this.maxLocals = maxLocals;
    }
  }
}
