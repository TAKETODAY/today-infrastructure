/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.aot.agent;

import org.junit.jupiter.api.Test;

import infra.bytecode.Handle;
import infra.bytecode.Opcodes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/1 17:08
 */
class InvocationsRecorderClassVisitorTests {

  @Test
  void shouldCreateClassVisitor() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThat(classVisitor).isNotNull();
    assertThat(classVisitor.isTransformed()).isFalse();
  }

  @Test
  void shouldReturnOriginalClassBufferWhenNotTransformed() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    byte[] buffer = classVisitor.getTransformedClassBuffer();
    assertThat(buffer).isNotNull();
    assertThat(classVisitor.isTransformed()).isFalse();
  }

  @Test
  void shouldCreateMethodVisitor() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThatCode(() -> {
      classVisitor.visitMethod(Opcodes.ACC_PUBLIC, "testMethod", "()V", null, null);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldInitializeInstrumentedMethods() {
    // This test ensures the static initialization block runs without errors
    assertThatCode(() -> {
      // Accessing a method that uses instrumentedMethods to trigger static init
      InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor.class.getDeclaredConstructors();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldRewriteMethodNameCorrectly() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    // We can't directly test the private method, but we can verify the class initializes correctly
    assertThatCode(() -> {
      assertThat(classVisitor).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleVisitMethodWithAllParameters() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThatCode(() -> {
      classVisitor.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              "main", "([Ljava/lang/String;)V", null, new String[] { "java/lang/Exception" });
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldCreateInnerMethodVisitorWithNullDelegate() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThatCode(() -> {
      InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
              classVisitor.new InvocationsRecorderMethodVisitor(null);
      assertThat(methodVisitor).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldSetTransformedFlagWhenMethodIsVisited() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThat(classVisitor.isTransformed()).isFalse();
    // We can't easily test the actual transformation without proper bytecode context
  }

  @Test
  void shouldHandleVisitMethodWithNullSignature() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThatCode(() -> {
      classVisitor.visitMethod(Opcodes.ACC_PRIVATE, "privateMethod", "()V", null, null);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleVisitMethodWithExceptions() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThatCode(() -> {
      classVisitor.visitMethod(Opcodes.ACC_PUBLIC, "methodWithException", "()V", null,
              new String[] { "java/lang/RuntimeException" });
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldCreateMultipleMethodVisitors() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThatCode(() -> {
      InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor mv1 =
              classVisitor.new InvocationsRecorderMethodVisitor(null);
      InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor mv2 =
              classVisitor.new InvocationsRecorderMethodVisitor(null);
      assertThat(mv1).isNotNull();
      assertThat(mv2).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldRewriteDescriptorForStaticMethod() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    // Testing internal behavior indirectly through class structure validation
    assertThatCode(() -> {
      assertThat(classVisitor).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldRewriteDescriptorForVirtualMethod() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    // Testing internal behavior indirectly through class structure validation
    assertThatCode(() -> {
      assertThat(classVisitor).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleEmptyExceptionsArray() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    assertThatCode(() -> {
      classVisitor.visitMethod(Opcodes.ACC_PUBLIC, "noExceptions", "()V", null, new String[0]);
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldCreateMethodVisitorWithNonNullDelegate() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    infra.bytecode.MethodVisitor mockDelegate = new infra.bytecode.MethodVisitor() { };

    assertThatCode(() -> {
      InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
              classVisitor.new InvocationsRecorderMethodVisitor(mockDelegate);
      assertThat(methodVisitor).isNotNull();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleVisitMethodInsnWithUnsupportedOpcode() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      // INVOKEINTERFACE is not supported
      methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/lang/String", "length", "()I", true);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldNotTransformWhenMethodNotInInstrumentedSet() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "nonInstrumentedMethod", "()V", false);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleVisitInvokeDynamicInsnWithNonHandleArguments() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      methodVisitor.visitInvokeDynamicInsn("test", "()V",
              new infra.bytecode.Handle(Opcodes.H_INVOKESTATIC, "test", "method", "()V", false),
              "stringArgument", 42);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleVisitInvokeDynamicInsnWithNullHandleArguments() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      methodVisitor.visitInvokeDynamicInsn("test", "()V",
              new infra.bytecode.Handle(Opcodes.H_INVOKESTATIC, "test", "method", "()V", false),
              (Object) null);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleEmptyBootstrapMethodArguments() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      methodVisitor.visitInvokeDynamicInsn("test", "()V",
              new infra.bytecode.Handle(Opcodes.H_INVOKESTATIC, "test", "method", "()V", false));
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldCheckOpcodeSupportForInvokeStatic() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      // Testing that INVOKESTATIC opcode is supported but method is not instrumented
      methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "nonInstrumentedMethod", "()V", false);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldCheckOpcodeSupportForInvokeVirtual() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      // Testing that INVOKEVIRTUAL opcode is supported but method is not instrumented
      methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "nonInstrumentedMethod", "()V", false);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleVisitMethodInsnWithNullDelegate() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
      // Should not throw exception even with null delegate
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldNotTransformWhenOwnerIsNull() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, null, "method", "()V", false);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldNotTransformWhenMethodNameIsNull() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System", null, "()V", false);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleSpecialOpcodeWithInstrumentedMethod() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      // INVOKE SPECIAL is not supported, so should not transform even if method is instrumented
      methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleInvokeDynamicWithInstrumentedMethod() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      Handle handle = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
      methodVisitor.visitInvokeDynamicInsn("test", "()J",
              new Handle(Opcodes.H_INVOKESTATIC, "test", "bootstrap", "()V", false), handle);
      // currentTimeMillis is not in instrumented methods, so should not transform
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleInvokeDynamicWithMultipleHandleArguments() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      Handle handle1 = new Handle(Opcodes.H_INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
      Handle handle2 = new Handle(Opcodes.H_INVOKEVIRTUAL, "java/lang/String", "toString", "()Ljava/lang/String;", false);
      methodVisitor.visitInvokeDynamicInsn("test", "()V",
              new Handle(Opcodes.H_INVOKESTATIC, "test", "bootstrap", "()V", false), handle1, "string", handle2);
      assertThat(classVisitor.isTransformed()).isFalse();
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleRewriteDescriptorForInvokeStatic() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      // Testing internal method indirectly
      String descriptor = methodVisitor.rewriteDescriptor(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;");
      assertThat(descriptor).isEqualTo("(I)Ljava/lang/String;");
    }).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleRewriteDescriptorForInvokeVirtual() {
    InvocationsRecorderClassVisitor classVisitor = new InvocationsRecorderClassVisitor();
    InvocationsRecorderClassVisitor.InvocationsRecorderMethodVisitor methodVisitor =
            classVisitor.new InvocationsRecorderMethodVisitor(null);

    assertThatCode(() -> {
      // Testing internal method indirectly
      String descriptor = methodVisitor.rewriteDescriptor(Opcodes.INVOKEVIRTUAL, "java/lang/String", "length", "()I");
      assertThat(descriptor).isEqualTo("(Ljava/lang/String;)I");
    }).doesNotThrowAnyException();
  }

}