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

// Modifications Copyright 2017 - 2026 the TODAY authors.
package infra.bytecode.tree.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import infra.bytecode.AsmTest;
import infra.bytecode.ClassReader;
import infra.bytecode.Label;
import infra.bytecode.Opcodes;
import infra.bytecode.tree.ClassNode;
import infra.bytecode.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link BasicVerifier}.
 *
 * @author Eric Bruneton
 */
class AnalyzerWithBasicVerifierTests extends AsmTest {

  private static final String CLASS_NAME = "C";

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> new BasicVerifier());
  }

  @Test
  void testAnalyze_invalidAload() {
    MethodNode methodNode = new MethodNodeBuilder().iconst_0().istore(1).aload(1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an object reference, but found I"));
  }

  @Test
  void testAnalyze_invalidAstore() {
    MethodNode methodNode = new MethodNodeBuilder().iconst_0().astore(1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an object reference or a return address, but found I"));
  }

  @Test
  void testAnalyze_invalidIstore() {
    MethodNode methodNode = new MethodNodeBuilder().aconst_null().istore(1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains(" Expected I, but found R"));
  }

  @Test
  void testAnalyze_invalidCheckcast() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .iconst_0()
                    .typeInsn(Opcodes.CHECKCAST, "java/lang/String")
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an object reference, but found I"));
  }

  @Test
  void testAnalyze_invalidArraylength() {
    MethodNode methodNode =
            new MethodNodeBuilder().iconst_0().insn(Opcodes.ARRAYLENGTH).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an array reference, but found I"));
  }

  @Test
  void testAnalyze_invalidAthrow() {
    MethodNode methodNode =
            new MethodNodeBuilder().iconst_0().insn(Opcodes.ATHROW).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an object reference, but found I"));
  }

  @Test
  void testAnalyze_invalidIneg() {
    MethodNode methodNode =
            new MethodNodeBuilder().insn(Opcodes.FCONST_0).insn(Opcodes.INEG).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected I, but found F"));
  }

  @Test
  void testAnalyze_invalidIadd() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.FCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.IADD)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("First argument: expected I, but found F"));
  }

  @Test
  void testAnalyze_invalidIastore() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.ICONST_1)
                    .intInsn(Opcodes.NEWARRAY, Opcodes.T_INT)
                    .insn(Opcodes.FCONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.IASTORE)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Second argument: expected I, but found F"));
  }

  @Test
  void testAnalyze_invalidFastore() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.ICONST_1)
                    .intInsn(Opcodes.NEWARRAY, Opcodes.T_FLOAT)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.FASTORE)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Third argument: expected F, but found I"));
  }

  @Test
  void testAnalyze_invalidLastore() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.ICONST_1)
                    .insn(Opcodes.ICONST_0)
                    .insn(Opcodes.LCONST_0)
                    .insn(Opcodes.LASTORE)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("First argument: expected a R array reference, but found I"));
  }

  @Test
  void testAnalyze_invalidMultianewarray() {
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .insn(Opcodes.FCONST_1)
                    .insn(Opcodes.ICONST_2)
                    .multiANewArrayInsn("[[I", 2)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertEquals("Error at instruction 2: Expected I, but found F", message);
  }

  @Test
  void testAnalyze_validMethodWithExceptionHandlers() {
    Label label0 = new Label();
    Label label1 = new Label();
    Label label2 = new Label();
    Label label3 = new Label();
    Label label4 = new Label();
    Label label5 = new Label();
    Label label6 = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder("(Ljava/lang/Object;)V", 3, 3)
                    .trycatch(label0, label1, label2, "java/lang/Exception")
                    .trycatch(label1, label3, label4, null)
                    .trycatch(label5, label6, label4, null)
                    .trycatch(label6, label2, label2, "java/lang/Exception")
                    .aload(0)
                    .ifnonnull(label0)
                    .aconst_null()
                    .athrow()
                    .label(label0)
                    .aload(0)
                    .astore(2)
                    .label(label1)
                    .nop()
                    .aload(2)
                    .pop()
                    .label(label3)
                    .vreturn()
                    .label(label4)
                    .astore(1)
                    .label(label5)
                    .aload(2)
                    .pop()
                    .label(label6)
                    .aload(1)
                    .athrow()
                    .label(label2)
                    .astore(1)
                    .go(label3)
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    assertDoesNotThrow(analyze);
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a BasicVerifier.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  void testAnalyze_basicVerifier(final PrecompiledClass classParameter, final Api apiParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    Analyzer<BasicValue> analyzer = newAnalyzer();

    for (MethodNode methodNode : classNode.methods) {
      assertDoesNotThrow(() -> analyzer.analyze(classNode.name, methodNode));
    }
  }

  @Test
  void testAnalyze_invalidIloadDueToLastInstructionOfExceptionHandler() {
    Label startTryLabel = new Label();
    Label endTryLabel = new Label();
    Label catchLabel = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder()
                    .trycatch(startTryLabel, endTryLabel, catchLabel)
                    .iconst_0()
                    .istore(1)
                    .label(startTryLabel)
                    .aconst_null()
                    .astore(1)
                    .label(endTryLabel)
                    .vreturn()
                    .label(catchLabel)
                    .iload(1)
                    .vreturn()
                    .build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Error at instruction 8: Expected I, but found ."));
  }

  private static Analyzer<BasicValue> newAnalyzer() {
    return new Analyzer<>(new BasicVerifier());
  }
}
