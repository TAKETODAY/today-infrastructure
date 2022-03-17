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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.tree.ClassNode;
import cn.taketoday.core.bytecode.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link BasicVerifier}.
 *
 * @author Eric Bruneton
 */
public class AnalyzerWithBasicVerifierTest extends AsmTest {

  private static final String CLASS_NAME = "C";

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new BasicVerifier());
  }

  @Test
  public void testAnalyze_invalidAload() {
    MethodNode methodNode = new MethodNodeBuilder().iconst_0().istore(1).aload(1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an object reference, but found I"));
  }

  @Test
  public void testAnalyze_invalidAstore() {
    MethodNode methodNode = new MethodNodeBuilder().iconst_0().astore(1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an object reference or a return address, but found I"));
  }

  @Test
  public void testAnalyze_invalidIstore() {
    MethodNode methodNode = new MethodNodeBuilder().aconst_null().istore(1).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains(" Expected I, but found R"));
  }

  @Test
  public void testAnalyze_invalidCheckcast() {
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
  public void testAnalyze_invalidArraylength() {
    MethodNode methodNode =
            new MethodNodeBuilder().iconst_0().insn(Opcodes.ARRAYLENGTH).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an array reference, but found I"));
  }

  @Test
  public void testAnalyze_invalidAthrow() {
    MethodNode methodNode =
            new MethodNodeBuilder().iconst_0().insn(Opcodes.ATHROW).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected an object reference, but found I"));
  }

  @Test
  public void testAnalyze_invalidIneg() {
    MethodNode methodNode =
            new MethodNodeBuilder().insn(Opcodes.FCONST_0).insn(Opcodes.INEG).vreturn().build();

    Executable analyze = () -> newAnalyzer().analyze(CLASS_NAME, methodNode);

    String message = assertThrows(AnalyzerException.class, analyze).getMessage();
    assertTrue(message.contains("Expected I, but found F"));
  }

  @Test
  public void testAnalyze_invalidIadd() {
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
  public void testAnalyze_invalidIastore() {
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
  public void testAnalyze_invalidFastore() {
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
  public void testAnalyze_invalidLastore() {
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
  public void testAnalyze_invalidMultianewarray() {
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

  /**
   * Tests that the precompiled classes can be successfully analyzed with a BasicVerifier.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAnalyze_basicVerifier(
          final PrecompiledClass classParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    Analyzer<BasicValue> analyzer = newAnalyzer();

    for (MethodNode methodNode : classNode.methods) {
      assertDoesNotThrow(() -> analyzer.analyze(classNode.name, methodNode));
    }
  }

  private static Analyzer<BasicValue> newAnalyzer() {
    return new Analyzer<>(new BasicVerifier());
  }
}
