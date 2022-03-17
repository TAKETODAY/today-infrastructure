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
package cn.taketoday.core.bytecode.tree.analysis;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.Label;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.core.bytecode.tree.MethodNode;

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
                            Type.fromInternalName(classNode.name),
                            Type.fromInternalName(classNode.superName),
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
            new SimpleVerifier(Type.fromDescriptor("LC;"), Type.fromDescriptor("Ljava/lang/Number;"), false));
  }
}
