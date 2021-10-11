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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import cn.taketoday.core.bytecode.Opcodes;
import cn.taketoday.core.bytecode.signature.SignatureReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link TraceSignatureVisitor}.
 *
 * @author Eugene Kuleshov
 */
public class TraceSignatureVisitorTest {

  private static final String[][] CLASS_SIGNATURES = {
          {
                  "false",
                  "<E extends java.lang.Enum<E>> implements java.lang.Comparable<E>, java.io.Serializable",
                  "<E:Ljava/lang/Enum<TE;>;>Ljava/lang/Object;Ljava/lang/Comparable<TE;>;Ljava/io/Serializable;"
          },
          {
                  "true",
                  "<D extends java.lang.reflect.GenericDeclaration> extends java.lang.reflect.Type",
                  "<D::Ljava/lang/reflect/GenericDeclaration;>Ljava/lang/Object;Ljava/lang/reflect/Type;"
          },
          {
                  "false",
                  "<K, V> extends java.util.AbstractMap<K, V> implements java.util.concurrent.ConcurrentMap<K, V>, java.io.Serializable",
                  "<K:Ljava/lang/Object;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/util/concurrent/ConcurrentMap<TK;TV;>;Ljava/io/Serializable;"
          },
          {
                  "false",
                  "<K extends java.lang.Enum<K>, V> extends java.util.AbstractMap<K, V> implements java.io.Serializable, java.lang.Cloneable",
                  "<K:Ljava/lang/Enum<TK;>;V:Ljava/lang/Object;>Ljava/util/AbstractMap<TK;TV;>;Ljava/io/Serializable;Ljava/lang/Cloneable;"
          },
          { "false", "<T, R extends T>", "<T:Ljava/lang/Object;R:TT;>Ljava/lang/Object;" }
  };

  private static final String[][] FIELD_SIGNATURES = {
          { "T[]", "[TT;" },
          { "AA<byte[][]>", "LAA<[[B>;" },
          { "java.lang.Class<?>", "Ljava/lang/Class<*>;" },
          { "java.lang.reflect.Constructor<T>", "Ljava/lang/reflect/Constructor<TT;>;" },
          { "java.util.Hashtable<?, ?>", "Ljava/util/Hashtable<**>;" },
          {
                  "java.util.concurrent.atomic.AtomicReferenceFieldUpdater<java.io.BufferedInputStream, byte[]>",
                  "Ljava/util/concurrent/atomic/AtomicReferenceFieldUpdater<Ljava/io/BufferedInputStream;[B>;"
          },
          {
                  "AA<java.util.Map<java.lang.String, java.lang.String>[][]>",
                  "LAA<[[Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;>;"
          },
          {
                  "java.util.Hashtable<java.lang.Object, java.lang.String>",
                  "Ljava/util/Hashtable<Ljava/lang/Object;Ljava/lang/String;>;"
          }
  };

  private static final String[][] METHOD_SIGNATURES = {
          { "void()E, F", "()V^TE;^TF;" },
          { "void(A<E>.B)", "(LA<TE;>.B;)V" },
          { "void(A<E>.B<F>)", "(LA<TE;>.B<TF;>;)V" },
          { "void(boolean, byte, char, short, int, float, long, double)", "(ZBCSIFJD)V" },
          {
                  "java.lang.Class<? extends E><E extends java.lang.Class>()",
                  "<E:Ljava/lang/Class;>()Ljava/lang/Class<+TE;>;"
          },
          {
                  "java.lang.Class<? super E><E extends java.lang.Class>()",
                  "<E:Ljava/lang/Class;>()Ljava/lang/Class<-TE;>;"
          },
          {
                  "void(java.lang.String, java.lang.Class<?>, java.lang.reflect.Method[], java.lang.reflect.Method, java.lang.reflect.Method)",
                  "(Ljava/lang/String;Ljava/lang/Class<*>;[Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)V"
          },
          {
                  "java.util.Map<java.lang.Object, java.lang.String>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>)",
                  "(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;"
          },
          {
                  "java.util.Map<java.lang.Object, java.lang.String><T>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)",
                  "<T:Ljava/lang/Object;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;"
          },
          {
                  "java.util.Map<java.lang.Object, java.lang.String><E, T extends java.lang.Comparable<E>>(java.lang.Object, java.util.Map<java.lang.Object, java.lang.String>, T)",
                  "<E:Ljava/lang/Object;T::Ljava/lang/Comparable<TE;>;>(Ljava/lang/Object;Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;TT;)Ljava/util/Map<Ljava/lang/Object;Ljava/lang/String;>;"
          }
  };

  public static Stream<Arguments> classSignatures() {
    return Arrays.stream(CLASS_SIGNATURES).map(values -> Arguments.of((Object[]) values));
  }

  public static Stream<Arguments> fieldSignatures() {
    return Arrays.stream(FIELD_SIGNATURES).map(values -> Arguments.of((Object[]) values));
  }

  public static Stream<Arguments> methodSignatures() {
    return Arrays.stream(METHOD_SIGNATURES).map(values -> Arguments.of((Object[]) values));
  }

  @Test
  public void testVisitBaseType_invalidSignature() {
    TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(0);

    Executable visitBaseType = () -> traceSignatureVisitor.visitBaseType('-');

    assertThrows(IllegalArgumentException.class, visitBaseType);
  }

  @ParameterizedTest
  @MethodSource("classSignatures")
  public void testVisitMethods_classSignature(
          final boolean isInterface, final String declaration, final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    TraceSignatureVisitor traceSignatureVisitor =
            new TraceSignatureVisitor(isInterface ? Opcodes.ACC_INTERFACE : 0);

    signatureReader.accept(traceSignatureVisitor);

    assertEquals(declaration, traceSignatureVisitor.getDeclaration());
  }

  @ParameterizedTest
  @MethodSource("fieldSignatures")
  public void testVisitMethods_fieldSignature(final String declaration, final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(0);

    signatureReader.acceptType(traceSignatureVisitor);

    assertEquals(declaration, traceSignatureVisitor.getDeclaration());
  }

  @ParameterizedTest
  @MethodSource("methodSignatures")
  public void testVisitMethods_methodSignature(final String declaration, final String signature) {
    SignatureReader signatureReader = new SignatureReader(signature);
    TraceSignatureVisitor traceSignatureVisitor = new TraceSignatureVisitor(0);

    signatureReader.accept(traceSignatureVisitor);

    String fullMethodDeclaration =
            traceSignatureVisitor.getReturnType()
                    + traceSignatureVisitor.getDeclaration()
                    + (traceSignatureVisitor.getExceptions() != null
                       ? traceSignatureVisitor.getExceptions()
                       : "");
    assertEquals(declaration, fullMethodDeclaration);
  }
}
