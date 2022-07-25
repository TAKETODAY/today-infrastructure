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

import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.Opcodes;
import cn.taketoday.bytecode.tree.MethodNode;
import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.tree.ClassNode;

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
