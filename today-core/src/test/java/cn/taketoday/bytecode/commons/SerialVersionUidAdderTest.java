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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.bytecode.ClassWriter;
import cn.taketoday.bytecode.Opcodes;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SerialVersionUIDAdder}.
 *
 * @author Eric Bruneton
 */
public class SerialVersionUidAdderTest extends AsmTest {

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new SerialVersionUIDAdder(null));
  }

  @ParameterizedTest
  @CsvSource({
          "SerialVersionClass,4654798559887898126",
          "SerialVersionAnonymousInnerClass$1,2591057588230880800",
          "SerialVersionInterface,682190902657822970",
          "SerialVersionEmptyInterface,-2126445979242430981"
  })
  void testAllMethods(final String className, final long expectedSvuid) throws IOException {
    ClassReader classReader = new ClassReader(className);
    SerialVersionUIDAdder svuidAdder = new SerialVersionUIDAdder(null);

    classReader.accept(svuidAdder, 0);
    long svuid = svuidAdder.computeSVUID();

    assertEquals(expectedSvuid, svuid);
  }

  @Test
  public void testAllMethods_enum() throws IOException {
    ClassReader classReader = new ClassReader("SerialVersionEnum");
    ClassWriter classWriter = new ClassWriter(0);
    SerialVersionUIDAdder svuidAdder = new SerialVersionUIDAdder(classWriter);

    classReader.accept(svuidAdder, 0);

    assertFalse(new ClassFile(classWriter.toByteArray()).toString().contains("serialVersionUID"));
  }

  /**
   * Tests that SerialVersionUIDAdder succeeds on all precompiled classes, and that it actually adds
   * a serialVersionUID field.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAllMethods_precompiledClass(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    ClassWriter classWriter = new ClassWriter(0);

    classReader.accept(
            new SerialVersionUIDAdder(classWriter) { }, 0);

    if ((classReader.getAccess() & Opcodes.ACC_ENUM) == 0) {
      assertTrue(new ClassFile(classWriter.toByteArray()).toString().contains("serialVersionUID"));
    }
  }
}
