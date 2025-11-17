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
