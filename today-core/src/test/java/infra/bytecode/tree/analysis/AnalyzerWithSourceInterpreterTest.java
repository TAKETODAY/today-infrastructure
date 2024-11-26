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
package infra.bytecode.tree.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import infra.bytecode.AsmTest;
import infra.bytecode.ClassReader;
import infra.bytecode.tree.MethodNode;
import infra.bytecode.tree.ClassNode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link Analyzer}, when used with a {@link SourceInterpreter}.
 *
 * @author Eric Bruneton
 */
public class AnalyzerWithSourceInterpreterTest extends AsmTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new SourceInterpreter());
  }

  /**
   * Tests that the precompiled classes can be successfully analyzed with a SourceInterpreter.
   *
   * @throws AnalyzerException if the test class can't be analyzed.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAnalyze_sourceInterpreter(
          final PrecompiledClass classParameter) {
    ClassNode classNode = new ClassNode();
    new ClassReader(classParameter.getBytes()).accept(classNode, 0);
    Analyzer<SourceValue> analyzer = new Analyzer<>(new SourceInterpreter());

    for (MethodNode methodNode : classNode.methods) {
      assertDoesNotThrow(() -> analyzer.analyze(classNode.name, methodNode));
    }
  }
}
