/*
 * Copyright 2017 - 2025 the original author or authors.
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
package infra.bytecode.tree.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import infra.bytecode.AsmTest;
import infra.bytecode.ClassReader;
import infra.bytecode.Label;
import infra.bytecode.Opcodes;
import infra.bytecode.Type;
import infra.bytecode.tree.ClassNode;
import infra.bytecode.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link SimpleVerifier}.
 *
 * @author Eric Bruneton
 */
public class AnalyzerWithSimpleVerifierTest extends AsmTest {

  private static final String CLASS_NAME = "C";

  @Test
  void testAnalyze_differentDimensions() {
    Label otherwise = new Label();
    Label finish = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder("()[Ljava/io/Serializable;", 3, 1)
                    .insn(Opcodes.ICONST_0)
                    .ifne(otherwise)
                    .iconst_0()
                    .iconst_0()
                    .multiANewArrayInsn("[[I", 2)
                    .go(finish)
                    .label(otherwise)
                    .iconst_0()
                    .iconst_0()
                    .iconst_0()
                    .multiANewArrayInsn("[[[Ljava/lang/System;", 3)
                    .label(finish)
                    .areturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    assertDoesNotThrow(analyze);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  @Test
  void testAnalyze_arrayOfCurrentClass() {
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    Label label3 = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .iconst_0()
                    .typeInsn(Opcodes.ANEWARRAY, CLASS_NAME)
                    .astore(1)
                    .iconst_0()
                    .istore(2)
                    .label(label0)
                    .iload(2)
                    .ifne(label1)
                    .aload(1)
                    .iload(2)
                    .insn(Opcodes.AALOAD)
                    .pop()
                    .go(label0)
                    .label(label1)
                    .label(label2)
                    .iload(2)
                    .ifne(label3)
                    .aload(1)
                    .iload(2)
                    .insn(Opcodes.AALOAD)
                    .pop()
                    .go(label2)
                    .label(label3)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    assertDoesNotThrow(analyze);
    assertDoesNotThrow(() -> MethodNodeBuilder.buildClassWithMethod(methodNode).newInstance());
  }

  @Test
  void testAnalyze_primitiveArrayReturnType() {
    MethodNode methodNode =
            new MethodNodeBuilder("()[I", 1, 1)
                    .iconst_0()
                    .typeInsn(Opcodes.ANEWARRAY, CLASS_NAME)
                    .areturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Incompatible return type: expected [I, but found [LC;"));
  }

  @Test
  void testAnalyze_invalidInvokevirtual() {
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
  void testAnalyze_invalidInvokeinterface() {
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
  void testAnalyze_classNotFound() {
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
  void testAnalyze_mergeStackFrames() {
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

  @Test
  void testAnalyze_mergeStackFramesWithExceptionHandlers() {
    Label startTry0Label = new Label();
    Label endTry0Label = new Label();
    Label catch0Label = new Label();
    Label startTry1Label = new Label();
    Label endTry1Label = new Label();
    Label catch1Label = new Label();
    Label startTry2Label = new Label();
    Label endTry2Label = new Label();
    Label catch2Label = new Label();
    Label label0 = new Label();
    Label labelReturn = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder(2, 6)
                    .trycatch(startTry0Label, endTry0Label, catch0Label, "java/lang/Throwable")
                    .trycatch(startTry1Label, endTry1Label, catch1Label, "java/lang/Throwable")
                    .trycatch(startTry2Label, endTry2Label, catch2Label)
                    .iconst_0()
                    .istore(2)
                    .typeInsn(Opcodes.NEW, "java/lang/String")
                    .astore(3)
                    .typeInsn(Opcodes.NEW, "java/nio/file/Path")
                    .astore(1)
                    .label(startTry2Label)
                    .typeInsn(Opcodes.NEW, "java/io/PrintWriter")
                    .astore(2)
                    .label(startTry0Label)
                    .label(endTry0Label)
                    .aload(2)
                    .methodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintWriter", "close", "()V", false)
                    .go(endTry2Label)
                    .label(catch0Label)
                    .astore(3)
                    .label(startTry1Label)
                    .aload(2)
                    .methodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintWriter", "close", "()V", false)
                    .label(endTry1Label)
                    .go(label0)
                    .label(catch1Label)
                    .astore(4)
                    .aload(3)
                    .aload(4)
                    .methodInsn(
                            Opcodes.INVOKEVIRTUAL,
                            "java/lang/Throwable",
                            "addSuppressed",
                            "(Ljava/lang/Throwable;)V",
                            false)
                    .label(label0)
                    .aload(3)
                    .athrow()
                    .label(endTry2Label)
                    .go(labelReturn)
                    .label(catch2Label)
                    .astore(5)
                    .aload(5)
                    .athrow()
                    .label(labelReturn)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    assertDoesNotThrow(analyze);
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a SimpleVerifier.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  void testAnalyze_simpleVerifier(final PrecompiledClass classParameter, final Api apiParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    assumeFalse(classNode.methods.isEmpty());
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
  void testIsAssignableFrom_interface() throws AnalyzerException {
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
