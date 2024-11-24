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
package infra.bytecode.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import infra.bytecode.AsmTest;
import infra.bytecode.ClassFile;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.ClassWriter;
import infra.bytecode.MethodVisitor;
import infra.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link TryCatchBlockSorter}.
 *
 * @author Eric Bruneton
 */
public class TryCatchBlockSorterTest extends AsmTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(
            () -> new TryCatchBlockSorter(null, Opcodes.ACC_PUBLIC, "name", "()V", null, null));
  }

  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAllMethods_precompileClass(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor classVisitor =
            new ClassVisitor(classWriter) {
              @Override
              public MethodVisitor visitMethod(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final String[] exceptions) {
                return new TryCatchBlockSorter(
                        super.visitMethod(access, name, descriptor, signature, exceptions),
                        access,
                        name,
                        descriptor,
                        signature,
                        exceptions);
              }
            };

    classReader.accept(classVisitor, 0);

    if (classParameter.isNotCompatibleWithCurrentJdk()) {
      assertThrows(
              UnsupportedClassVersionError.class,
              () -> new ClassFile(classWriter.toByteArray()).newInstance());
    }
    else {
      assertDoesNotThrow(() -> new ClassFile(classWriter.toByteArray()).newInstance());
    }
  }
}
