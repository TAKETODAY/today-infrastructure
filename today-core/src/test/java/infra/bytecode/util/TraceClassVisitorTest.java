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
package infra.bytecode.util;

import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.PrintWriter;
import java.io.StringWriter;

import infra.bytecode.AnnotationVisitor;
import infra.bytecode.AsmTest;
import infra.bytecode.Attribute;
import infra.bytecode.ClassFile;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.ClassWriter;
import infra.bytecode.FieldVisitor;
import infra.bytecode.MethodVisitor;
import infra.bytecode.ModuleVisitor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link TraceClassVisitor}.
 *
 * @author Eric Bruneton
 */
public class TraceClassVisitorTest extends AsmTest {

  /**
   * Tests that classes are unchanged with a ClassReader->TraceClassVisitor->ClassWriter transform.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testVisitMethods(final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor traceClassVisitor =
            new TraceClassVisitor(classWriter, new PrintWriter(new StringWriter()));

    classReader.accept(traceClassVisitor, new Attribute[] { new Comment(), new CodeComment() }, 0);

    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  /** Tests that ClassReader can accept a TraceClassVisitor without delegate. */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testVisitMethods_noDelegate(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    StringWriter output = new StringWriter();

    classReader.accept(new TraceClassVisitor(new PrintWriter(output, true)), 0);

    assertTrue(output.toString().contains(classParameter.getInternalName()));
  }

  /**
   * Tests that ClassReader can accept a TraceAnnotationVisitor, TraceFieldVisitor,
   * TraceMethodVisitor or TraceModuleVisitor without delegate.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testVisitMethods_noNestedDelegate(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);

    Executable accept =
            () ->
                    classReader.accept(
                            new ClassVisitor() {

                              @Override
                              public ModuleVisitor visitModule(
                                      final String name, final int access, final String version) {
                                return new TraceModuleVisitor(new Textifier());
                              }

                              @Override
                              public AnnotationVisitor visitAnnotation(
                                      final String descriptor, final boolean visible) {
                                return new TraceAnnotationVisitor(new Textifier());
                              }

                              @Override
                              public FieldVisitor visitField(
                                      final int access,
                                      final String name,
                                      final String descriptor,
                                      final String signature,
                                      final Object value) {
                                return new TraceFieldVisitor(new Textifier());
                              }

                              @Override
                              public MethodVisitor visitMethod(
                                      final int access,
                                      final String name,
                                      final String descriptor,
                                      final String signature,
                                      final String[] exceptions) {
                                return new TraceMethodVisitor(new Textifier());
                              }
                            },
                            0);

    assertDoesNotThrow(accept);
  }
}
