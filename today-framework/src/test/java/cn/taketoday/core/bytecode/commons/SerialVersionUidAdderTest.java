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
package cn.taketoday.core.bytecode.commons;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.ClassFile;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassWriter;
import cn.taketoday.core.bytecode.Opcodes;

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
