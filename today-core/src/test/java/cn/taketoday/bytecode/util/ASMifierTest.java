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
package cn.taketoday.bytecode.util;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.ClassLoaderIClassLoader;
import org.codehaus.janino.IClassLoader;
import org.codehaus.janino.Parser;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.UnitCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;

import cn.taketoday.bytecode.AsmTest;
import cn.taketoday.bytecode.Attribute;
import cn.taketoday.bytecode.ClassFile;
import cn.taketoday.bytecode.ClassReader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for {@link ASMifier}.
 *
 * @author Eugene Kuleshov
 * @author Eric Bruneton
 */
// DontCheck(AbbreviationAsWordInName)
public class ASMifierTest extends AsmTest {

  private static final String EXPECTED_USAGE =
          "Prints the ASM code to generate the given class.\n"
                  + "Usage: ASMifier [-nodebug] <fully qualified class name or class file name>\n";

  private static final IClassLoader ICLASS_LOADER =
          new ClassLoaderIClassLoader(new URLClassLoader(new URL[0]));

  @Test
  public void testConstructor() {
    assertDoesNotThrow((ThrowingSupplier<ASMifier>) ASMifier::new);
  }

  /**
   * Tests that the code produced with an ASMifier compiles and generates the original class.
   *
   * @throws Exception if something goes wrong.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testAsmify_precompiledClass(
          final PrecompiledClass classParameter) throws Exception {
    byte[] classFile = classParameter.getBytes();
    assumeTrue(classFile.length < Short.MAX_VALUE);
    StringWriter output = new StringWriter();
    TraceClassVisitor asmifier =
            new TraceClassVisitor(
                    null,
                    new ASMifier("classWriter", 0) { },
                    new PrintWriter(output, true));

    new ClassReader(classFile)
            .accept(asmifier, new Attribute[] { new Comment(), new CodeComment() }, 0);

    // Janino can't compile JDK9 modules.
    assumeTrue(classParameter != PrecompiledClass.JDK9_MODULE);
    byte[] asmifiedClassFile = compile(classParameter.getName(), output.toString());
    Class<?> asmifiedClass = new ClassFile(asmifiedClassFile).newInstance().getClass();
    byte[] dumpClassFile = (byte[]) asmifiedClass.getMethod("dump").invoke(null);
    assertEquals(new ClassFile(classFile), new ClassFile(dumpClassFile));
  }

  private static byte[] compile(final String name, final String source) throws IOException {
    Parser parser = new Parser(new Scanner(name, new StringReader(source)));
    try {
      UnitCompiler unitCompiler = new UnitCompiler(parser.parseAbstractCompilationUnit(), ICLASS_LOADER);
      return unitCompiler.compileUnit(true, true, true)[0].toByteArray();
    }
    catch (CompileException e) {
      throw new AssertionError(source, e);
    }
  }

  @Test
  public void testMain_missingClassName() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = new String[0];

    ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTextEquals("", output.toString());
    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  @Test
  public void testMain_missingClassName_withNodebug() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug" };

    ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTextEquals("", output.toString());
    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  @Test
  public void testMain_tooManyArguments() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug", getClass().getName(), "extraArgument" };

    ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTextEquals("", output.toString());
    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  static void assertTextEquals(String one, String two) {
    two = two.replace("\r", "").replace("\n", "");
    one = one.replace("\r", "").replace("\n", "");
    assertEquals(one, two);
  }

  @Test
  public void testMain_classFileNotFound() {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "DoNotExist.class" };

    Executable main =
            () -> ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

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
            () -> ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertThrows(IOException.class, main);
    assertEquals("", output.toString());
    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_className() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { getClass().getName() };

    ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("public class ASMifierTestDump implements Opcodes"));
    assertTrue(output.toString().contains("\nmethodVisitor.visitLineNumber("));
    assertTrue(output.toString().contains("\nmethodVisitor.visitLocalVariable("));
    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_className_withNodebug() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = { "-nodebug", getClass().getName() };

    ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("public class ASMifierTestDump implements Opcodes"));
    assertFalse(output.toString().contains("\nmethodVisitor.visitLineNumber("));
    assertFalse(output.toString().contains("\nmethodVisitor.visitLocalVariable("));
    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_classFile() throws IOException {
    StringWriter output = new StringWriter();
    StringWriter logger = new StringWriter();
    String[] args = {
            ClassLoader.getSystemResource(getClass().getName().replace('.', '/') + ".class").getPath()
    };

    ASMifier.main(args, new PrintWriter(output, true), new PrintWriter(logger, true));

    assertTrue(output.toString().contains("public class ASMifierTestDump implements Opcodes"));
    assertEquals("", logger.toString());
  }
}
