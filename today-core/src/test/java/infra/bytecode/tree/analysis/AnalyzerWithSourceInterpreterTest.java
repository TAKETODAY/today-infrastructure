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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import infra.bytecode.AsmTest;
import infra.bytecode.ClassReader;
import infra.bytecode.Label;
import infra.bytecode.Opcodes;
import infra.bytecode.tree.AbstractInsnNode;
import infra.bytecode.tree.ClassNode;
import infra.bytecode.tree.MethodNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link SourceInterpreter}.
 *
 * @author Eric Bruneton
 */
class AnalyzerWithSourceInterpreterTest extends AsmTest {

  @Test
  void testConstructor() {
    assertDoesNotThrow(SourceInterpreter::new);
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a SourceInterpreter.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  void testAnalyze_sourceInterpreter(
          final PrecompiledClass classParameter, final Api apiParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());

    for (MethodNode methodNode : classNode.methods) {
      assertDoesNotThrow(() -> analyzer.analyze(classNode.name, methodNode));
    }
  }

  /** Checks if DUP_X2 producers are correct. */
  @Test
  void testAnalyze_dupx2Producers() throws AnalyzerException {
    Label label0 = new Label();
    Label label1 = new Label();
    MethodNode methodNode =
            new MethodNodeBuilder(4, 1)
                    .push()
                    .push()
                    .iconst_0()
                    .ifne(label0)
                    // First case
                    .insn(Opcodes.ICONST_M1)
                    .go(label1)
                    // Second case
                    .label(label0)
                    .iconst_0()
                    // DUP_X2 value
                    .label(label1)
                    .insn(Opcodes.DUP_X2)
                    .pop() // Point where the frame is checked
                    .pop()
                    .pop()
                    .pop()
                    .vreturn()
                    .build();

    Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());
    analyzer.analyze("C", methodNode);

    AbstractInsnNode firstPop =
            Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn.getOpcode() == Opcodes.POP)
                    .findFirst()
                    .get();
    AbstractInsnNode dupx2 =
            Arrays.stream(methodNode.instructions.toArray())
                    .filter(insn -> insn.getOpcode() == Opcodes.DUP_X2)
                    .findFirst()
                    .get();
    Frame<SourceValue> frame = analyzer.getFrames()[methodNode.instructions.indexOf(firstPop)];
    // Check if all source values have the DUP_X2 as a producer
    SourceValue sourceValue1 = frame.getStack(frame.getStackSize() - 4);
    SourceValue sourceValue2 = frame.getStack(frame.getStackSize() - 3);
    SourceValue sourceValue3 = frame.getStack(frame.getStackSize() - 2);
    SourceValue sourceValue4 = frame.getStack(frame.getStackSize() - 1);
    assertEquals(sourceValue1.insns.iterator().next(), dupx2);
    assertEquals(sourceValue2.insns.iterator().next(), dupx2);
    assertEquals(sourceValue3.insns.iterator().next(), dupx2);
    assertEquals(sourceValue4.insns.iterator().next(), dupx2);
  }
}
