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
package cn.taketoday.asm.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import cn.taketoday.asm.AsmTest;
import cn.taketoday.asm.Attribute;
import cn.taketoday.asm.ClassFile;
import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.ClassWriter;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.tree.analysis.AnalyzerException;

import static cn.taketoday.asm.util.ASMifierTest.assertTextEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CheckClassAdapter}.
 *
 * @author Eric Bruneton
 */
public class CheckClassAdapterTest extends AsmTest implements Opcodes {

  private static final String EXPECTED_USAGE =
          "Verifies the given class.\n"
                  + "Usage: CheckClassAdapter <fully qualified class name or class file name>\n";

  @Test
  public void testConstructor() {
    assertDoesNotThrow(() -> new CheckClassAdapter(null));
  }

  @Test
  public void testVisit_illegalClassAccessFlag() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit =
            () -> checkClassAdapter.visit(V1_1, 1 << 20, "C", null, "java/lang/Object", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("Invalid access flags: 1048576", exception.getMessage());
  }

  @Test
  public void testVisit_illegalClassName() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit = () -> checkClassAdapter.visit(V1_1, 0, null, null, "java/lang/Object", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("Illegal class name (null)", exception.getMessage());
  }

  @Test
  public void testVisit_nonJavaIdentifierClassNamePre15() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit =
            () -> checkClassAdapter.visit(V1_4, 0, "class name", null, "java/lang/Object", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals(
            "Invalid class name (must be an internal class name): class name", exception.getMessage());
  }

  @Test
  public void testVisit_nonJavaIdentifierClassNamePost15() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit =
            () -> checkClassAdapter.visit(V1_5, 0, "class name", null, "java/lang/Object", null);

    assertDoesNotThrow(visit);
  }

  @Test
  public void testVisit_illegalSuperClass() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit =
            () ->
                    checkClassAdapter.visit(
                            V1_1, ACC_PUBLIC, "java/lang/Object", null, "java/lang/Object", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("The super class name of the Object class must be 'null'", exception.getMessage());
  }

  @Test
  public void testVisit_moduleInfoSuperClass() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit =
            () ->
                    checkClassAdapter.visit(
                            V1_1, ACC_PUBLIC, "module-info", null, "java/lang/Object", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals(
            "The super class name of a module-info class must be 'null'", exception.getMessage());
  }

  @Test
  public void testVisit_illegalInterfaceSuperClass() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit = () -> checkClassAdapter.visit(V1_1, ACC_INTERFACE, "I", null, "C", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals(
            "The super class name of interfaces must be 'java/lang/Object'", exception.getMessage());
  }

  @Test
  public void testVisit_illegalSignature() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit =
            () -> checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", "LC;I", "java/lang/Object", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("LC;I: error at index 3", exception.getMessage());
  }

  @Test
  public void testVisit_illegalAccessFlagSet() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visit =
            () ->
                    checkClassAdapter.visit(
                            V1_1, ACC_FINAL + ACC_ABSTRACT, "C", null, "java/lang/Object", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visit);
    assertEquals("final and abstract are mutually exclusive: 1040", exception.getMessage());
  }

  @Test
  public void testVisit_illegalMultipleCalls() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visit =
            () -> checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Exception exception = assertThrows(IllegalStateException.class, visit);
    assertEquals("visit must be called only once", exception.getMessage());
  }

  @Test
  public void testVisitModule_illegalModuleName() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitModule = () -> checkClassAdapter.visitModule("pkg.invalid=name", 0, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitModule);
    assertEquals(
            "Invalid module name (must be a fully qualified name): pkg.invalid=name",
            exception.getMessage());
  }

  @Test
  public void testVisitModule_illegalMultipleCalls() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    checkClassAdapter.visitModule("module1", Opcodes.ACC_OPEN, null);

    Executable visitModule = () -> checkClassAdapter.visitModule("module2", 0, null);

    Exception exception = assertThrows(IllegalStateException.class, visitModule);
    assertEquals("visitModule can be called only once.", exception.getMessage());
  }

  @Test
  public void testVisitSource_beforeStart() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);

    Executable visitSource = () -> checkClassAdapter.visitSource(null, null);

    Exception exception = assertThrows(IllegalStateException.class, visitSource);
    assertEquals("Cannot visit member before visit has been called.", exception.getMessage());
  }

  @Test
  public void testVisitSource_afterEnd() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    checkClassAdapter.visitEnd();

    Executable visitSource = () -> checkClassAdapter.visitSource(null, null);

    Exception exception = assertThrows(IllegalStateException.class, visitSource);
    assertEquals("Cannot visit member after visitEnd has been called.", exception.getMessage());
  }

  @Test
  public void testVisitSource_illegalMultipleCalls() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    checkClassAdapter.visitSource(null, null);

    Executable visitSource = () -> checkClassAdapter.visitSource(null, null);

    Exception exception = assertThrows(IllegalStateException.class, visitSource);
    assertEquals("visitSource can be called only once.", exception.getMessage());
  }

  @Test
  public void testVisitOuterClass_illegalOuterClassName() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitOuterClass = () -> checkClassAdapter.visitOuterClass(null, null, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitOuterClass);
    assertEquals("Illegal outer class owner", exception.getMessage());
  }

  @Test
  public void testVisitOuterClass_illegalMultipleCalls() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    checkClassAdapter.visitOuterClass("name", null, null);

    Executable visitOuterClass = () -> checkClassAdapter.visitOuterClass(null, null, null);

    Exception exception = assertThrows(IllegalStateException.class, visitOuterClass);
    assertEquals("visitOuterClass can be called only once.", exception.getMessage());
  }

  @Test
  public void testInnerClass_illegalInnerClassName() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    checkClassAdapter.visitInnerClass("name", "outerName", "0validInnerName", 0);

    Executable visitInnerClass =
            () -> checkClassAdapter.visitInnerClass("name", "outerName", "0illegalInnerName;", 0);

    Exception exception = assertThrows(IllegalArgumentException.class, visitInnerClass);
    assertEquals(
            "Invalid inner class name (must be a valid Java identifier): 0illegalInnerName;",
            exception.getMessage());
  }

  @Test
  public void testVisitRecordComponent_illegalRecordComponentSignature1() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V14, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitRecordComponent = () -> checkClassAdapter.visitRecordComponent("i", "I", "L;");

    Exception exception = assertThrows(IllegalArgumentException.class, visitRecordComponent);
    assertEquals("L;: identifier expected at index 1", exception.getMessage());
  }

  @Test
  public void testVisitRecordComponent_illegalRecordComponentSignature2() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V14, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitRecordComponent = () -> checkClassAdapter.visitRecordComponent("i", "I", "LC+");

    Exception exception = assertThrows(IllegalArgumentException.class, visitRecordComponent);
    assertEquals("LC+: ';' expected at index 3", exception.getMessage());
  }

  @Test
  public void testVisitRecordComponent_illegalRecordComponentSignature3() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V14, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitRecordComponent =
            () -> checkClassAdapter.visitRecordComponent("i", "I", "LC;I");

    Exception exception = assertThrows(IllegalArgumentException.class, visitRecordComponent);
    assertEquals("LC;I: error at index 3", exception.getMessage());
  }

  @Test
  public void testVisitField_illegalAccessFlagSet() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitField =
            () -> checkClassAdapter.visitField(ACC_PUBLIC + ACC_PRIVATE, "i", "I", null, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitField);
    assertEquals("public, protected and private are mutually exclusive: 3", exception.getMessage());
  }

  @Test
  public void testVisitField_illegalFieldSignature1() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitField = () -> checkClassAdapter.visitField(ACC_PUBLIC, "i", "I", "L;", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitField);
    assertEquals("L;: identifier expected at index 1", exception.getMessage());
  }

  @Test
  public void testVisitField_illegalFieldSignature2() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitField = () -> checkClassAdapter.visitField(ACC_PUBLIC, "i", "I", "LC+", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitField);
    assertEquals("LC+: ';' expected at index 3", exception.getMessage());
  }

  @Test
  public void testVisitField_illegalFieldSignature3() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitField = () -> checkClassAdapter.visitField(ACC_PUBLIC, "i", "I", "LC;I", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitField);
    assertEquals("LC;I: error at index 3", exception.getMessage());
  }

  @Test
  public void testVisitMethod_illegalAccessFlagSet() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitMethod =
            () -> checkClassAdapter.visitMethod(ACC_ABSTRACT | ACC_STRICT, "m", "()V", null, null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethod);
    assertEquals("strictfp and abstract are mutually exclusive: 3072", exception.getMessage());
  }

  @Test
  public void testVisitMethod_legalAccessFlagSet_V17() {
    // Java 17 allows to mix ACC_ABSTRACT and ACC_STRICT because ACC_STRICT is ignored
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V17, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitMethod =
            () -> checkClassAdapter.visitMethod(ACC_ABSTRACT | ACC_STRICT, "m", "()V", null, null);

    assertDoesNotThrow(visitMethod);
  }

  @Test
  public void testVisitMethod_illegalSignature() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitMethod =
            () ->
                    checkClassAdapter.visitMethod(
                            ACC_PUBLIC, "m", "()V", "<T::LI.J<*+LA;>;>()V^LA;X", null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitMethod);
    assertEquals("<T::LI.J<*+LA;>;>()V^LA;X: error at index 24", exception.getMessage());
  }

  @Test
  public void testVisitTypeAnnotation_illegalAnnotation1() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitTypeAnnotation =
            () -> checkClassAdapter.visitTypeAnnotation(0xFFFFFFFF, null, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeAnnotation);
    assertEquals("Invalid type reference sort 0xff", exception.getMessage());
  }

  @Test
  public void testVisitTypeAnnotation_illegalAnnotation2() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitTypeAnnotation =
            () -> checkClassAdapter.visitTypeAnnotation(0x00FFFFFF, null, "LA;", true);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeAnnotation);
    assertEquals("Invalid type reference 0xffffff", exception.getMessage());
  }

  @Test
  public void testVisitAttribute_illegalAttribute() {
    CheckClassAdapter checkClassAdapter = new CheckClassAdapter(null);
    checkClassAdapter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);

    Executable visitAttribute = () -> checkClassAdapter.visitAttribute(null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitAttribute);
    assertEquals("Invalid attribute (must not be null)", exception.getMessage());
  }

  /**
   * Tests that classes are unchanged with a ClassReader->CheckClassAdapter->ClassWriter transform.
   */
  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_ALL_APIS)
  public void testVisitMethods_precompiledClass(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassWriter classWriter = new ClassWriter(0);
    ClassVisitor classVisitor = new CheckClassAdapter(classWriter, true);

    Executable accept = () -> classReader.accept(classVisitor, attributes(), 0);

    classReader.accept(classVisitor, attributes(), 0);
    assertEquals(new ClassFile(classFile), new ClassFile(classWriter.toByteArray()));
  }

  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testVisitMethods_noDelegate_precompiledClass(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassVisitor classVisitor = new CheckClassAdapter(null, true) { };

    Executable accept = () -> classReader.accept(classVisitor, attributes(), 0);

    assertDoesNotThrow(accept);
  }

  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testVisitMethods_noMemberDelegate_precompiledClass(
          final PrecompiledClass classParameter) {
    byte[] classFile = classParameter.getBytes();
    ClassReader classReader = new ClassReader(classFile);
    ClassVisitor classVisitor =
            new CheckClassAdapter(new ClassVisitor(null) { }, true) { };

    Executable accept = () -> classReader.accept(classVisitor, attributes(), 0);

    assertDoesNotThrow(accept);
  }

  @ParameterizedTest
  @MethodSource(ALL_CLASSES_AND_LATEST_API)
  public void testVerify_precompiledClass(
          final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    StringWriter logger = new StringWriter();

    CheckClassAdapter.verify(
            classReader, /* printResults = */ false, new PrintWriter(logger, true));

    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_missingClassName() throws IOException {
    StringWriter logger = new StringWriter();
    String[] args = new String[0];

    CheckClassAdapter.main(args, new PrintWriter(logger, true));

    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  @Test
  public void testMain_tooManyArguments() throws IOException {
    StringWriter logger = new StringWriter();
    String[] args = { getClass().getName(), "extraArgument" };

    CheckClassAdapter.main(args, new PrintWriter(logger, true));

    assertTextEquals(EXPECTED_USAGE, logger.toString());
  }

  @Test
  public void testMain_classFileNotFound() {
    StringWriter logger = new StringWriter();
    String[] args = { "DoNotExist.class" };

    Executable main = () -> CheckClassAdapter.main(args, new PrintWriter(logger, true));

    assertThrows(IOException.class, main);
    assertTextEquals("", logger.toString());
  }

  @Test
  public void testMain_classNotFound() {
    StringWriter logger = new StringWriter();
    String[] args = { "do\\not\\exist" };

    Executable main = () -> CheckClassAdapter.main(args, new PrintWriter(logger, true));

    assertThrows(IOException.class, main);
    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_className() throws IOException {
    StringWriter logger = new StringWriter();
    String[] args = { getClass().getName() };

    CheckClassAdapter.main(args, new PrintWriter(logger, true));

    assertEquals("", logger.toString());
  }

  @Test
  public void testMain_classFile() throws IOException {
    StringWriter logger = new StringWriter();
    String[] args = {
            ClassLoader.getSystemResource(getClass().getName().replace('.', '/') + ".class").getPath()
    };

    CheckClassAdapter.main(args, new PrintWriter(logger, true));

    assertEquals("", logger.toString());
  }

  @Test
  public void testVerify_validClass() throws Exception {
    ClassReader classReader = new ClassReader(getClass().getName());
    StringWriter logger = new StringWriter();

    CheckClassAdapter.verify(classReader, true, new PrintWriter(logger, true));

    String log = logger.toString();
    assertFalse(log.startsWith(AnalyzerException.class.getName() + ": Error at instruction"));
    assertTrue(log.contains("00000 CheckClassAdapterTest  :  :     ALOAD 0"));
    assertTrue(log.contains("00001 CheckClassAdapterTest [Object  : [Object  :     ARETURN"));
  }

  @Test
  public void testVerify_invalidClass() {
    ClassWriter classWriter = new ClassWriter(0);
    classWriter.visit(V1_1, ACC_PUBLIC, "C", null, "java/lang/Object", null);
    MethodVisitor methodVisitor = classWriter.visitMethod(ACC_PUBLIC, "m", "()V", null, null);
    methodVisitor.visitCode();
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitVarInsn(ISTORE, 30);
    methodVisitor.visitInsn(RETURN);
    methodVisitor.visitMaxs(1, 31);
    methodVisitor.visitEnd();
    classWriter.visitEnd();
    ClassReader classReader = new ClassReader(classWriter.toByteArray());
    StringWriter logger = new StringWriter();

    CheckClassAdapter.verify(classReader, true, new PrintWriter(logger, true));

    String log = logger.toString();
    assertTrue(
            log.startsWith(
                    AnalyzerException.class.getName()
                            + ": Error at instruction 1: Expected I, but found LC;"));
  }

  private static Attribute[] attributes() {
    return new Attribute[] { new Comment(), new CodeComment() };
  }

  Object methodWithObjectArrayArgument(final Object[] arg) {
    return arg;
  }
}
