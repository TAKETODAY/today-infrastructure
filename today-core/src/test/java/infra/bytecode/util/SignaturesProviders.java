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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import infra.bytecode.AsmTest;
import infra.bytecode.AsmTest.PrecompiledClass;
import infra.bytecode.ClassReader;
import infra.bytecode.ClassVisitor;
import infra.bytecode.FieldVisitor;
import infra.bytecode.MethodVisitor;

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
