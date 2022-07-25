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
package cn.taketoday.bytecode.commons;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassVisitor;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.ConstantDynamic;
import cn.taketoday.bytecode.Handle;
import cn.taketoday.bytecode.Label;
import cn.taketoday.bytecode.MethodVisitor;
import cn.taketoday.bytecode.Type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CodeSizeEvaluator}.
 *
 * @author Eric Bruneton
 */
public class CodeSizeEvaluatorTest extends AsmTest {

  /**
   * Tests that the size estimations of CodeSizeEvaluator are correct, and that classes are
   * unchanged with a ClassReader->CodeSizeEvaluator->ClassWriter transform.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testAllMethods_precompiledClass(final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ArrayList<CodeSizeEvaluation> evaluations = new ArrayList<>();
    ClassVisitor codeSizesEvaluator =
            new CodeSizesEvaluator(classWriter, evaluations);

    Executable accept = () -> classReader.accept(codeSizesEvaluator, attributes(), 0);

    assertDoesNotThrow(accept);
    for (CodeSizeEvaluation evaluation : evaluations) {
      assertTrue(evaluation.actualSize >= evaluation.minSize);
      assertTrue(evaluation.actualSize <= evaluation.maxSize);
    }
    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  private static Attribute[] attributes() {
    return new Attribute[] { new Comment(), new CodeComment() };
  }

  static class CodeSizeEvaluation {

    final int minSize;
    final int maxSize;
    final int actualSize;

    CodeSizeEvaluation(final int minSize, final int maxSize, final int actualSize) {
      this.minSize = minSize;
      this.maxSize = maxSize;
      this.actualSize = actualSize;
    }
  }

  static class CodeSizesEvaluator extends ClassVisitor {

    private final ArrayList<CodeSizeEvaluation> evaluations;

    CodeSizesEvaluator(

            final ClassWriter classWriter,
            final ArrayList<CodeSizeEvaluation> evaluations) {
      super(classWriter);
      this.evaluations = evaluations;
    }

    @Override
    public MethodVisitor visitMethod(
            final int access,
            final String name,
            final String descriptor,
            final String signature,
            final String[] exceptions) {
      MethodVisitor methodVisitor =
              super.visitMethod(access, name, descriptor, signature, exceptions);
      return new CodeSizeEvaluator(methodVisitor) {

        @Override
        public void visitLdcInsn(final Object value) {
          if (value instanceof Boolean
                  || value instanceof Byte
                  || value instanceof Short
                  || value instanceof Character
                  || value instanceof Integer
                  || value instanceof Long
                  || value instanceof Double
                  || value instanceof Float
                  || value instanceof String
                  || value instanceof Type
                  || value instanceof Handle
                  || value instanceof ConstantDynamic) {
            super.visitLdcInsn(value);
          }
          else {
            // If this happens, add support for the new type in
            // CodeSizeEvaluator.visitLdcInsn(), if needed.
            throw new IllegalArgumentException("Unsupported type of value: " + value);
          }
        }

        @Override
        public void visitMaxs(final int maxStack, final int maxLocals) {
          Label end = new Label();
          visitLabel(end);
          super.visitMaxs(maxStack, maxLocals);

          evaluations.add(new CodeSizeEvaluation(getMinSize(), getMaxSize(), end.getOffset()));
        }
      };
    }
  }
}
