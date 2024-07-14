/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package cn.taketoday.bytecode.tree.analysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.Type;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link SimpleVerifier}.
 *
 * @author Eric Bruneton
 */
public class AnalyzerWithSimpleVerifierTest extends AsmTest {

  private static final String CLASS_NAME = "C";

  @Test
  public void testAnalyze_invalidInvokevirtual() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.ACONST_NULL)
                    .typeInsn(Opcodes.CHECKCAST, "java/lang/Object")
                    .methodInsn(Opcodes.INVOKEVIRTUAL, "java/util/ArrayList", "size", "()I", false)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(
            message.contains(
                    "Method owner: expected Ljava/util/ArrayList;, but found Ljava/lang/Object;"));
  }

  @Test
  public void testAnalyze_invalidInvokeinterface() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.ACONST_NULL)
                    .typeInsn(Opcodes.CHECKCAST, "java/util/List")
                    .insn(Opcodes.FCONST_0)
                    .methodInsn(
                            Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertEquals("Error at instruction 3: Argument 1: expected I, but found F", message);
  }

  @Test
  public void testAnalyze_classNotFound() {
    Label loopLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .aload(0)
                    .astore(1)
                    .label(loopLabel)
                    .aconst_null()
                    .typeInsn(Opcodes.CHECKCAST, "D")
                    .astore(1)
                    .go(loopLabel)
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Type java.lang.ClassNotFoundException: D not present"));
  }

  @Test
  public void testAnalyze_mergeStackFrames() throws AnalyzerException {
    Label loopLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder(1, 4)
                    .aload(0)
                    .astore(1)
                    .aconst_null()
                    .typeInsn(Opcodes.CHECKCAST, "java/lang/Number")
                    .astore(2)
                    .aload(0)
                    .astore(3)
                    .label(loopLabel)
                    .aconst_null()
                    .typeInsn(Opcodes.CHECKCAST, "java/lang/Number")
                    .astore(1)
                    .aload(0)
                    .astore(2)
                    .aconst_null()
                    .typeInsn(Opcodes.CHECKCAST, "java/lang/Integer")
                    .astore(3)
                    .go(loopLabel)
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    assertDoesNotThrow(analyze);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a SimpleVerifier.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAnalyze_simpleVerifier(
          final PrecompiledClass classParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    Assumptions.assumeFalse(classNode.methods.isEmpty());
    Analyzer<BasicValue> analyzer =
            new Analyzer<BasicValue>(
                    new SimpleVerifier(
                            Type.forInternalName(classNode.name),
                            Type.forInternalName(classNode.superName),
                            (classNode.access & Opcodes.ACC_INTERFACE) != 0));

    for (MethodNode methodNode : classNode.methods) {
      assertDoesNotThrow(() -> analyzer.analyze(classNode.name, methodNode));
    }
  }

  /**
   * Checks that the merge of an ArrayList and an SQLException can be returned as an Iterable. The
   * merged type is recomputed by SimpleVerifier as Object (because of limitations of the merging
   * algorithm, due to multiple interface inheritance), but the subtyping check is relaxed if the
   * super type is an interface type.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @Test
  public void testIsAssignableFrom_interface() throws AnalyzerException {
    Label elseLabel = new Label();
    Label endIfLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder(
                    "(Ljava/util/ArrayList;Ljava/sql/SQLException;)Ljava/lang/Iterable;", 1, 3)
                    .aload(1)
                    .ifnonnull(elseLabel)
                    .aload(1)
                    .go(endIfLabel)
                    .label(elseLabel)
                    .aload(2)
                    .label(endIfLabel)
                    .areturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    assertDoesNotThrow(analyze);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  private static Analyzer<BasicValue> newAnalyzer() {
    return new Analyzer<>(
            new SimpleVerifier(Type.forDescriptor("LC;"), Type.forDescriptor("Ljava/lang/Number;"), false));
  }
}
