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

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.signature.SignatureReader;
import cn.taketoday.core.bytecode.signature.SignatureVisitor;
import cn.taketoday.core.bytecode.signature.SignatureWriter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link CheckSignatureAdapter}.
 *
 * @author Eric Bruneton
 */
public class CheckSignatureAdapterTest extends AsmTest {

  @Test
  public void testVisitFormalTypeParameter_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);
    checkSignatureAdapter.visitSuperclass();

    Executable visitFormalTypeParameter = () -> checkSignatureAdapter.visitFormalTypeParameter("T");

    assertThrows(IllegalStateException.class, visitFormalTypeParameter);
  }

  @Test
  public void testVisitFormalTypeParameter_typeSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitFormalTypeParameter = () -> checkSignatureAdapter.visitFormalTypeParameter("T");

    assertThrows(IllegalStateException.class, visitFormalTypeParameter);
  }

  @Test
  public void testVisitClassBound_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitClassBound = () -> checkSignatureAdapter.visitClassBound();

    assertThrows(IllegalStateException.class, visitClassBound);
  }

  @Test
  public void testVisitClassBound_typeSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitClassBound = () -> checkSignatureAdapter.visitClassBound();

    assertThrows(IllegalStateException.class, visitClassBound);
  }

  @Test
  public void testVisitInterfaceBound_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitInterfaceBound = () -> checkSignatureAdapter.visitInterfaceBound();

    assertThrows(IllegalStateException.class, visitInterfaceBound);
  }

  @Test
  public void testVisitInterfaceBound_typeSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitInterfaceBound = () -> checkSignatureAdapter.visitInterfaceBound();

    assertThrows(IllegalStateException.class, visitInterfaceBound);
  }

  @Test
  public void testVisitSuperClass_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);
    checkSignatureAdapter.visitSuperclass();

    Executable visitSuperClass = () -> checkSignatureAdapter.visitSuperclass();

    assertThrows(IllegalStateException.class, visitSuperClass);
  }

  @Test
  public void testVisitSuperClass_methodSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE, null);

    Executable visitSuperClass = () -> checkSignatureAdapter.visitSuperclass();

    assertThrows(IllegalStateException.class, visitSuperClass);
  }

  @Test
  public void testVisitInterface_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitInterface = () -> checkSignatureAdapter.visitInterface();

    assertThrows(IllegalStateException.class, visitInterface);
  }

  @Test
  public void testVisitInterface_methodSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE, null);

    Executable visitInterface = () -> checkSignatureAdapter.visitInterface();

    assertThrows(IllegalStateException.class, visitInterface);
  }

  @Test
  public void testVisitParameterType_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitParameterType = () -> checkSignatureAdapter.visitParameterType();

    assertThrows(IllegalStateException.class, visitParameterType);
  }

  @Test
  public void testVisitParameterType_methodSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE, null);
    checkSignatureAdapter.visitReturnType();

    Executable visitParameterType = () -> checkSignatureAdapter.visitParameterType();

    assertThrows(IllegalStateException.class, visitParameterType);
  }

  @Test
  public void testVisitReturnType_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitReturnType = () -> checkSignatureAdapter.visitReturnType();

    assertThrows(IllegalStateException.class, visitReturnType);
  }

  @Test
  public void testVisitReturnType_methodSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE, null);
    checkSignatureAdapter.visitReturnType();

    Executable visitReturnType = () -> checkSignatureAdapter.visitReturnType();

    assertThrows(IllegalStateException.class, visitReturnType);
  }

  @Test
  public void testVisitExceptionType_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitExceptionType = () -> checkSignatureAdapter.visitExceptionType();

    assertThrows(IllegalStateException.class, visitExceptionType);
  }

  @Test
  public void testVisitExceptionType_methodSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE, null);

    Executable visitExceptionType = () -> checkSignatureAdapter.visitExceptionType();

    assertThrows(IllegalStateException.class, visitExceptionType);
  }

  @Test
  public void testVisitBaseType_typeSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);
    checkSignatureAdapter.visitBaseType('I');

    Executable visitBaseType = () -> checkSignatureAdapter.visitBaseType('I');

    assertThrows(IllegalStateException.class, visitBaseType);
  }

  @Test
  public void testVisitBaseType_typeSignature_illegalVoidArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitBaseType = () -> checkSignatureAdapter.visitBaseType('V');

    Exception exception = assertThrows(IllegalArgumentException.class, visitBaseType);
    assertEquals("Base type descriptor can't be V", exception.getMessage());
  }

  @Test
  public void testVisitBaseType_typeSignature_illegalArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitBaseType = () -> checkSignatureAdapter.visitBaseType('A');

    Exception exception = assertThrows(IllegalArgumentException.class, visitBaseType);
    assertEquals("Base type descriptor must be one of ZCBSIFJD", exception.getMessage());
  }

  @Test
  public void testVisitBaseType_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitBaseType = () -> checkSignatureAdapter.visitBaseType('I');

    assertThrows(IllegalStateException.class, visitBaseType);
  }

  @Test
  public void testVisitTypeVariable_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitTypeVariable = () -> checkSignatureAdapter.visitTypeVariable("T");

    assertThrows(IllegalStateException.class, visitTypeVariable);
  }

  @Test
  public void testVisitTypeVariable_typeSignature_nullArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitTypeVariable = () -> checkSignatureAdapter.visitTypeVariable(null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeVariable);
    assertEquals("Invalid type variable (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitTypeVariable_typeSignature_emptyArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitTypeVariable = () -> checkSignatureAdapter.visitTypeVariable("");

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeVariable);
    assertEquals("Invalid type variable (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitTypeVariable_typeSignature_illegalArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitTypeVariable = () -> checkSignatureAdapter.visitTypeVariable("LT;");

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeVariable);
    assertEquals(
            "Invalid type variable (must not contain . ; [ / < > or :): LT;", exception.getMessage());
  }

  @Test
  public void testVisitTypeVariable_typeSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);
    checkSignatureAdapter.visitTypeVariable("T");

    Executable visitTypeVariable = () -> checkSignatureAdapter.visitTypeVariable("T");

    assertThrows(IllegalStateException.class, visitTypeVariable);
  }

  @Test
  public void testVisitArrayType_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitArrayType = () -> checkSignatureAdapter.visitArrayType();

    assertThrows(IllegalStateException.class, visitArrayType);
  }

  @Test
  public void testVisitArrayType_typeSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);
    checkSignatureAdapter.visitArrayType();

    Executable visitArrayType = () -> checkSignatureAdapter.visitArrayType();

    assertThrows(IllegalStateException.class, visitArrayType);
  }

  @Test
  public void testVisitClassType_classSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);

    Executable visitClassType = () -> checkSignatureAdapter.visitClassType("A");

    assertThrows(IllegalStateException.class, visitClassType);
  }

  @Test
  public void testVisitClassType_typeSignature_nullArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitClassType = () -> checkSignatureAdapter.visitClassType(null);

    Exception exception = assertThrows(IllegalArgumentException.class, visitClassType);
    assertEquals("Invalid class name (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitClassType_typeSignature_emptyArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitClassType = () -> checkSignatureAdapter.visitClassType("");

    Exception exception = assertThrows(IllegalArgumentException.class, visitClassType);
    assertEquals("Invalid class name (must not be null or empty)", exception.getMessage());
  }

  @Test
  public void testVisitClassType_typeSignature_illegalArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitClassType = () -> checkSignatureAdapter.visitClassType("<A>");

    Exception exception = assertThrows(IllegalArgumentException.class, visitClassType);
    assertEquals(
            "Invalid class name (must not contain . ; [ < > or :): <A>", exception.getMessage());
  }

  @Test
  public void testVisitClassType_typeSignature_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);
    checkSignatureAdapter.visitClassType("A");

    Executable visitClassType = () -> checkSignatureAdapter.visitClassType("A");

    assertThrows(IllegalStateException.class, visitClassType);
  }

  @Test
  public void testVisitClassType_nonJavaIdentifier() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null);
    SignatureVisitor signatureVisitor = checkSignatureAdapter.visitSuperclass();

    Executable visitClassType = () -> signatureVisitor.visitClassType("Foo Bar");

    assertDoesNotThrow(visitClassType);
  }

  @Test
  public void testVisitInnerClassType_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitInnerClassType = () -> checkSignatureAdapter.visitInnerClassType("A");

    assertThrows(IllegalStateException.class, visitInnerClassType);
  }

  @Test
  public void testVisitTypeArgument_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitTypeArgument = () -> checkSignatureAdapter.visitTypeArgument();

    assertThrows(IllegalStateException.class, visitTypeArgument);
  }

  @Test
  public void testVisitTypeArgument_wildcard_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitTypeArgument = () -> checkSignatureAdapter.visitTypeArgument('+');

    assertThrows(IllegalStateException.class, visitTypeArgument);
  }

  @Test
  public void testVisitTypeArgument_wildcard_illegalArgument() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);
    checkSignatureAdapter.visitClassType("A");

    Executable visitTypeArgument = () -> checkSignatureAdapter.visitTypeArgument('*');

    Exception exception = assertThrows(IllegalArgumentException.class, visitTypeArgument);
    assertEquals("Wildcard must be one of +-=", exception.getMessage());
  }

  @Test
  public void testVisitEnd_illegalState() {
    CheckSignatureAdapter checkSignatureAdapter =
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null);

    Executable visitEnd = () -> checkSignatureAdapter.visitEnd();

    assertThrows(IllegalStateException.class, visitEnd);
  }

  @ParameterizedTest
  @MethodSource({ "cn.taketoday.core.bytecode.util.SignaturesProviders#classSignatures" })
  public void testVisitMethods_classSignature(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    SignatureWriter signatureWriter = new SignatureWriter();

    signatureReader.accept(
            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, signatureWriter));

    assertEquals(signature, signatureWriter.toString());
  }

  @ParameterizedTest
  @MethodSource({ "cn.taketoday.core.bytecode.util.SignaturesProviders#classSignatures" })
  public void testVisitMethods_classSignature_noDelegate(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);

    Executable visitMethods =
            () ->
                    signatureReader.accept(
                            new CheckSignatureAdapter(CheckSignatureAdapter.CLASS_SIGNATURE, null));

    assertDoesNotThrow(visitMethods);
  }

  @ParameterizedTest
  @MethodSource({ "cn.taketoday.core.bytecode.util.SignaturesProviders#methodSignatures" })
  public void testVisitMethods_methodSignature(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    SignatureWriter signatureWriter = new SignatureWriter();

    signatureReader.accept(
            new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE, signatureWriter));

    assertEquals(signature, signatureWriter.toString());
  }

  @ParameterizedTest
  @MethodSource({ "cn.taketoday.core.bytecode.util.SignaturesProviders#methodSignatures" })
  public void testVisitMethods_methodSignature_noDelegate(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);

    Executable visitMethods =
            () ->
                    signatureReader.accept(
                            new CheckSignatureAdapter(CheckSignatureAdapter.METHOD_SIGNATURE, null));

    assertDoesNotThrow(visitMethods);
  }

  @ParameterizedTest
  @MethodSource({ "cn.taketoday.core.bytecode.util.SignaturesProviders#fieldSignatures" })
  public void testVisitMethods_typeSignature(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    SignatureWriter signatureWriter = new SignatureWriter();

    signatureReader.acceptType(
            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, signatureWriter));

    assertEquals(signature, signatureWriter.toString());
  }

  @ParameterizedTest
  @MethodSource({ "cn.taketoday.core.bytecode.util.SignaturesProviders#fieldSignatures" })
  public void testVisitMethods_typeSignature_noDelegate(final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);

    Executable visitMethods =
            () ->
                    signatureReader.acceptType(
                            new CheckSignatureAdapter(CheckSignatureAdapter.TYPE_SIGNATURE, null));

    assertDoesNotThrow(visitMethods);
  }
}
