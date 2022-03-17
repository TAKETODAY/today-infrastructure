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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import cn.taketoday.core.bytecode.AsmTest;
import cn.taketoday.core.bytecode.AsmTest.PrecompiledClass;
import cn.taketoday.core.bytecode.ClassReader;
import cn.taketoday.core.bytecode.ClassVisitor;
import cn.taketoday.core.bytecode.FieldVisitor;
import cn.taketoday.core.bytecode.MethodVisitor;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Provides class, field and method signatures for parameterized unit tests.
 *
 * @author Eric Bruneton
 */
public final class SignaturesProviders {

  private static final List<String> CLASS_SIGNATURES = new ArrayList<>();
  private static final List<String> FIELD_SIGNATURES = new ArrayList<>();
  private static final List<String> METHOD_SIGNATURES = new ArrayList<>();

  static {
    AsmTest.allClassesAndLatestApi()
            .map(argument -> (PrecompiledClass) argument.get()[0])
            .forEach(SignaturesProviders::collectSignatures);
    assertFalse(CLASS_SIGNATURES.isEmpty());
    assertFalse(FIELD_SIGNATURES.isEmpty());
    assertFalse(METHOD_SIGNATURES.isEmpty());
  }

  private SignaturesProviders() { }

  private static void collectSignatures(final PrecompiledClass classParameter) {
    ClassReader classReader = new ClassReader(classParameter.getBytes());
    classReader.accept(
            new ClassVisitor() {
              @Override
              public void visit(
                      final int version,
                      final int access,
                      final String name,
                      final String signature,
                      final String superName,
                      final String[] interfaces) {
                if (signature != null) {
                  CLASS_SIGNATURES.add(signature);
                }
              }

              @Override
              public FieldVisitor visitField(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final Object value) {
                if (signature != null) {
                  FIELD_SIGNATURES.add(signature);
                }
                return null;
              }

              @Override
              public MethodVisitor visitMethod(
                      final int access,
                      final String name,
                      final String descriptor,
                      final String signature,
                      final String[] exceptions) {
                if (signature != null) {
                  METHOD_SIGNATURES.add(signature);
                }
                return null;
              }
            },
            0);
  }

  static Stream<String> classSignatures() {
    return CLASS_SIGNATURES.stream();
  }

  static Stream<String> fieldSignatures() {
    return FIELD_SIGNATURES.stream();
  }

  static Stream<String> methodSignatures() {
    return METHOD_SIGNATURES.stream();
  }
}
