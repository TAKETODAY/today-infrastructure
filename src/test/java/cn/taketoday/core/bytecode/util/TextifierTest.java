/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.ClassReader;

import static cn.taketoday.core.bytecode.util.ASMifierTest.assertTextEquals;
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
public class TextifierTest extends AsmTest {

  private static final String EXPECTED_USAGE =
          "Prints a disassembled view of the given class.\n"
                  + "Usage: Textifier [-nodebug] <fully qualified class name or class file name>\n";

  @Test
  public void testConstructor() {
    assertDoesNotThrow(Textifier::new);
  }

  /**
   * Tests that the text produced with a Textifier is equal to the expected text.
   *
   * @throws IOException if the expected text can't be read from disk.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testTextify_precompiledClass(
          final PrecompiledClass classParameter) throws IOException {
    byte[] classFile = classParameter.getBytes();
    StringWriter output = new StringWriter();
    assumeTrue(classFile.length < 32768);

    new ClassReader(classFile)
            .accept(
                    new TraceClassVisitor(
                            null, new Textifier() { }, new PrintWriter(output)),
                    0);

    String expectedText =
            new String(
                    Files.readAllBytes(
                            Paths.get("src/test/resources/" + classParameter.getName() + ".txt")),
                    StandardCharsets.UTF_8)
                    .replace("\r", "");

    assertEquals(expectedText, output.toString());
  }

  @Test
  public void testMain_missingClassName() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = new String[0];

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertEquals("", output.toString());
    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  @Test
  public void testMain_missingClassName_withNodebug() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug" };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertEquals("", output.toString());
    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  @Test
  public void testMain_tooManyArguments() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug", getClass().getName(), "extraArgument" };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertEquals("", output.toString());
    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  @Test
  public void testMain_classFileNotFound() {
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
  public void testMain_classNotFound() {
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
  public void testMain_className() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { getClass().getName() };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("\npublic class cn/taketoday/core/bytecode/util/TextifierTest"));
    assertTrue(output.toString().contains("\n    LINENUMBER"));
    assertTrue(output.toString().contains("\n    LOCALVARIABLE"));
    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_className_withNoebug() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug", getClass().getName() };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("\npublic class cn/taketoday/core/bytecode/util/TextifierTest"));
    assertFalse(output.toString().contains("\n    LINENUMBER"));
    assertFalse(output.toString().contains("\n    LOCALVARIABLE"));
    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_classFile() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = {
            ClassLoader.getSystemResource(getClass().getName().replace('.', '/') + ".class").getPath()
    };

    Textifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("\npublic class cn/taketoday/core/bytecode/util/TextifierTest"));
    assertEquals("", logger.toString());
  }
}
