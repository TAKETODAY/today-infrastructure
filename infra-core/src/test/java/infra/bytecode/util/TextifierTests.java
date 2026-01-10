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
package infra.bytecode.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import infra.bytecode.AsmTest;
import infra.bytecode.ClassReader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for {@link Textifier}.
 *
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
class TextifierTests extends AsmTest {

  private static final String EXPECTED_USAGE =
          "Prints a disassembled view of the given class.\n"
                  + "Usage: Textifier [-nodebug] <fully qualified class name or class file name>";

  @Test
  void testConstructor() {
    assertDoesNotThrow(() -> new Textifier());
  }

  /**
   * Tests that the text produced with a Textifier is equal to the expected text.
   *
   * @throws IOException if the expected text can't be read from disk.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  void testTextify_precompiledClass(final PrecompiledClass classParameter, final Api apiParameter)
          throws IOException {
    byte[] classFile = classParameter.getBytes();
    StringWriter output = new StringWriter();
    assumeTrue(classFile.length < 32768);

    new ClassReader(classFile)
            .accept(
                    new TraceClassVisitor(
                            null, new Textifier() { }, new PrintWriter(output)),
                    0);

    String expectedText =
            Files.readString(Paths.get("src/test/resources/" + classParameter.getName() + ".txt"))
                    .replace("\r", "");

    assertEquals(expectedText, output.toString());
  }

  @Test
  void testMain_missingClassName() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = new String[0];

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertEquals("", output.toString());
    assertEquals(EXPECTED_USAGE, logger.toString().trim());
  }

  @Test
  void testMain_missingClassName_withNodebug() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug" };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertEquals("", output.toString());
    assertEquals(EXPECTED_USAGE, logger.toString().trim());
  }

  @Test
  void testMain_tooManyArguments() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug", getClass().getName(), "extraArgument" };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertEquals("", output.toString());
    assertEquals(EXPECTED_USAGE, logger.toString().trim());
  }

  @Test
  void testMain_classFileNotFound() {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "DoNotExist.class" };

    Executable main =
            () -> Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertThrows(IOException.class, main);
    assertEquals("", output.toString());
    assertEquals("", logger.toString());
  }

  @Test
  void testMain_classNotFound() {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "do\\not\\exist" };

    Executable main =
            () -> Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertThrows(IOException.class, main);
    assertEquals("", output.toString());
    assertEquals("", logger.toString());
  }

  @Test
  void testMain_className() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { getClass().getName() };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("\nclass infra/bytecode/util/TextifierTest"));
    assertTrue(output.toString().contains("\n    LINENUMBER"));
    assertTrue(output.toString().contains("\n    LOCALVARIABLE"));
    assertEquals("", logger.toString());
  }

  @Test
  void testMain_className_withNoebug() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug", getClass().getName() };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("\nclass infra/bytecode/util/TextifierTests"));
    assertFalse(output.toString().contains("\n    LINENUMBER"));
    assertFalse(output.toString().contains("\n    LOCALVARIABLE"));
    assertEquals("", logger.toString());
  }

  @Test
  void testMain_classFile() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = {
            ClassLoader.getSystemResource(getClass().getName().replace('.', '/') + ".class").getPath()
    };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("\nclass infra/bytecode/util/TextifierTests"));
    assertEquals("", logger.toString());
  }
}
