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
package cn.taketoday.asm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Type}.
 *
 * @author Eric Bruneton
 */
public class TypeTest implements Opcodes {

  @Test
  public void testGetSort_typeConstants() {
    assertEquals(Type.VOID, Type.VOID_TYPE.getSort());
    assertEquals(Type.BOOLEAN, Type.BOOLEAN_TYPE.getSort());
    assertEquals(Type.CHAR, Type.CHAR_TYPE.getSort());
    assertEquals(Type.BYTE, Type.BYTE_TYPE.getSort());
    assertEquals(Type.SHORT, Type.SHORT_TYPE.getSort());
    assertEquals(Type.INT, Type.INT_TYPE.getSort());
    assertEquals(Type.FLOAT, Type.FLOAT_TYPE.getSort());
    assertEquals(Type.LONG, Type.LONG_TYPE.getSort());
    assertEquals(Type.DOUBLE, Type.DOUBLE_TYPE.getSort());
  }

  @Test
  public void testGetSize_typeConstants() {
    assertEquals(0, Type.VOID_TYPE.getSize());
    assertEquals(1, Type.BOOLEAN_TYPE.getSize());
    assertEquals(1, Type.CHAR_TYPE.getSize());
    assertEquals(1, Type.BYTE_TYPE.getSize());
    assertEquals(1, Type.SHORT_TYPE.getSize());
    assertEquals(1, Type.INT_TYPE.getSize());
    assertEquals(1, Type.FLOAT_TYPE.getSize());
    assertEquals(2, Type.LONG_TYPE.getSize());
    assertEquals(2, Type.DOUBLE_TYPE.getSize());
  }

  @Test
  public void testGetDescriptor_typeConstants() {
    assertEquals("V", Type.VOID_TYPE.getDescriptor());
    assertEquals("Z", Type.BOOLEAN_TYPE.getDescriptor());
    assertEquals("C", Type.CHAR_TYPE.getDescriptor());
    assertEquals("B", Type.BYTE_TYPE.getDescriptor());
    assertEquals("S", Type.SHORT_TYPE.getDescriptor());
    assertEquals("I", Type.INT_TYPE.getDescriptor());
    assertEquals("F", Type.FLOAT_TYPE.getDescriptor());
    assertEquals("J", Type.LONG_TYPE.getDescriptor());
    assertEquals("J", Type.LONG_TYPE.getDescriptor());
    assertEquals("D", Type.DOUBLE_TYPE.getDescriptor());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "I",
        "V",
        "Z",
        "B",
        "C",
        "S",
        "D",
        "F",
        "J",
        "LI;",
        "LV;",
        "Ljava/lang/Object;",
        "[I",
        "[LI;",
        "[[Ljava/lang/Object;",
        "(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V",
        "()I",
        "()LI;",
        "()Ljava/lang/Object;",
        "()[I",
        "()[LI;",
        "()[[Ljava/lang/Object;"
      })
  public void testGetTypeFromDescriptor(final String descriptor) {
    Type type = Type.getType(descriptor);

    assertEquals(descriptor, type.getDescriptor());
    assertEquals(descriptor, type.toString());
  }

  @Test
  public void testGetTypeFromDescriptor_invalid() {
    Executable getType = () -> Type.getType("-");

    assertThrows(IllegalArgumentException.class, getType);
  }

  @ParameterizedTest
  @ValueSource(strings = {"I", "V", "java/lang/Object", "[I", "[LI;", "[[Ljava/lang/Object;"})
  public void testGetObjectType(final String internalName) {
    Type type = Type.getObjectType(internalName);

    assertEquals(internalName, type.getInternalName());
    assertEquals(internalName.charAt(0) == '[' ? Type.ARRAY : Type.OBJECT, type.getSort());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V",
        "()I",
        "()LI;",
        "()Ljava/lang/Object;",
        "()[I",
        "()[LI;",
        "()[[Ljava/lang/Object;"
      })
  public void testGetMethodTypeFromDescriptor(final String methodDescriptor) {
    Type type = Type.getMethodType(methodDescriptor);

    assertEquals(Type.METHOD, type.getSort());
    assertEquals(methodDescriptor, type.getDescriptor());
    assertEquals(methodDescriptor, type.toString());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V",
        "()I",
        "()LI;",
        "()Ljava/lang/Object;",
        "()[I",
        "()[LI;",
        "()[[Ljava/lang/Object;"
      })
  public void testGetArgumentTypesGetReturnTypeAndGetMethodType(final String methodDescriptor) {
    Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
    Type returnType = Type.getReturnType(methodDescriptor);
    Type methodType = Type.getMethodType(returnType, argumentTypes);

    assertEquals(Type.METHOD, methodType.getSort());
    assertEquals(methodDescriptor, methodType.getDescriptor());
    assertArrayEquals(argumentTypes, methodType.getArgumentTypes());
    assertEquals(returnType, methodType.getReturnType());
  }

  @Test
  public void testGetArgumentTypesInvalidMethodDescriptor() {
    Executable getArgumentTypes = () -> Type.getArgumentTypes("(Ljava/lang/String");

    assertTimeoutPreemptively(
        Duration.ofMillis(100), () -> assertThrows(RuntimeException.class, getArgumentTypes));
  }

  @Test
  public void testGetReturnTypeFromDescriptor() {
    assertEquals(Type.INT_TYPE, Type.getReturnType("()I"));
    assertEquals(Type.INT_TYPE, Type.getReturnType("(Lpkg/classMethod();)I"));
  }

  @Test
  public void testGetTypeFromClass() {
    assertEquals(Type.VOID_TYPE, Type.getType(void.class));
    assertEquals(Type.BOOLEAN_TYPE, Type.getType(boolean.class));
    assertEquals(Type.CHAR_TYPE, Type.getType(char.class));
    assertEquals(Type.BYTE_TYPE, Type.getType(byte.class));
    assertEquals(Type.SHORT_TYPE, Type.getType(short.class));
    assertEquals(Type.INT_TYPE, Type.getType(int.class));
    assertEquals(Type.FLOAT_TYPE, Type.getType(float.class));
    assertEquals(Type.LONG_TYPE, Type.getType(long.class));
    assertEquals(Type.DOUBLE_TYPE, Type.getType(double.class));
    assertEquals("Ljava/lang/Object;", Type.getType(Object.class).getDescriptor());
    assertEquals("[Ljava/lang/Object;", Type.getType(Object[].class).getDescriptor());
  }

  @Test
  public void testGetTypeFromConstructor() throws NoSuchMethodException, SecurityException {
    Type type = Type.getType(ClassReader.class.getConstructor(byte[].class, int.class, int.class));

    assertEquals("([BII)V", type.getDescriptor());
  }

  @Test
  public void testGetTypeFromMethod() throws NoSuchMethodException, SecurityException {
    Type type = Type.getType(Arrays.class.getMethod("binarySearch", byte[].class, byte.class));

    assertEquals("([BB)I", type.getDescriptor());
  }

  @Test
  public void testGetArgumentTypesFromMethod() throws NoSuchMethodException, SecurityException {
    Type[] argumentTypes =
        Type.getArgumentTypes(Arrays.class.getMethod("binarySearch", byte[].class, byte.class));

    assertArrayEquals(new Type[] {Type.getType(byte[].class), Type.BYTE_TYPE}, argumentTypes);
  }

  @Test
  public void testGetReturnTypeFromMethod() throws NoSuchMethodException, SecurityException {
    Type returnType =
        Type.getReturnType(Arrays.class.getMethod("binarySearch", byte[].class, byte.class));

    assertEquals(Type.INT_TYPE, returnType);
  }

  @Test
  public void testGetArgumentsAndReturnSizeFromDescriptor() {
    assertEquals(
        17 << 2,
        Type.getArgumentsAndReturnSizes(
            "(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V"));
    assertEquals(1 << 2 | 1, Type.getArgumentsAndReturnSizes("()I"));
    assertEquals(1 << 2 | 1, Type.getArgumentsAndReturnSizes("()F"));
    assertEquals(1 << 2 | 2, Type.getArgumentsAndReturnSizes("()J"));
    assertEquals(1 << 2 | 2, Type.getArgumentsAndReturnSizes("()D"));
    assertEquals(1 << 2 | 1, Type.getArgumentsAndReturnSizes("()LD;"));
  }

  @Test
  public void testGetSort() {
    assertEquals(Type.ARRAY, Type.getType("[LI;").getSort());
    assertEquals(Type.ARRAY, Type.getObjectType("[LI;").getSort());
    assertEquals(Type.OBJECT, Type.getType("LI;").getSort());
    assertEquals(Type.OBJECT, Type.getObjectType("I").getSort());
  }

  @Test
  public void testGetDimensions() {
    assertEquals(1, Type.getType("[I").getDimensions());
    assertEquals(3, Type.getType("[[[LI;").getDimensions());
  }

  @Test
  public void testGetElementType() {
    assertEquals(Type.INT_TYPE, Type.getType("[I").getElementType());
    assertEquals(Type.getObjectType("I"), Type.getType("[[[LI;").getElementType());
  }

  @Test
  public void testGetClassName() {
    assertEquals("void", Type.VOID_TYPE.getClassName());
    assertEquals("boolean", Type.BOOLEAN_TYPE.getClassName());
    assertEquals("char", Type.CHAR_TYPE.getClassName());
    assertEquals("byte", Type.BYTE_TYPE.getClassName());
    assertEquals("short", Type.SHORT_TYPE.getClassName());
    assertEquals("int", Type.INT_TYPE.getClassName());
    assertEquals("float", Type.FLOAT_TYPE.getClassName());
    assertEquals("long", Type.LONG_TYPE.getClassName());
    assertEquals("double", Type.DOUBLE_TYPE.getClassName());
    assertEquals("I[]", Type.getObjectType("[LI;").getClassName());
    assertEquals("java.lang.Object", Type.getObjectType("java/lang/Object").getClassName());
    assertEquals("java.lang.Object", Type.getType("Ljava/lang/Object;").getClassName());
  }

  @Test
  public void testGetInternalName() {
    assertEquals("[LI;", Type.getObjectType("[LI;").getInternalName());
    assertEquals("java/lang/Object", Type.getObjectType("java/lang/Object").getInternalName());
    assertEquals("java/lang/Object", Type.getType("Ljava/lang/Object;").getInternalName());
  }

  @Test
  public void testGetArgumentsAndReturnSize() {
    Type type = Type.getType("(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V");

    assertEquals(17 << 2, type.getArgumentsAndReturnSizes());
  }

  @Test
  public void testGetDescriptor() {
    assertEquals("[LI;", Type.getObjectType("[LI;").getDescriptor());
    assertEquals("Ljava/lang/Object;", Type.getObjectType("java/lang/Object").getDescriptor());
    assertEquals("Ljava/lang/Object;", Type.getType("Ljava/lang/Object;").getDescriptor());
  }

  @Test
  public void testGetMethodDescriptor() {
    assertEquals("(IJ)V", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.LONG_TYPE));
    assertEquals(
        "(Ljava/lang/Object;)V",
        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType("Ljava/lang/Object;")));
    assertEquals(
        "(Ljava/lang/Object;)V",
        Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType("java/lang/Object")));
  }

  @Test
  public void testGetInternalNameFromClass() {
    assertEquals("java/lang/Object", Type.getInternalName(Object.class));
    assertEquals("[Ljava/lang/Object;", Type.getInternalName(Object[].class));
  }

  @Test
  public void testGetDescriptorFromClass() {
    assertEquals("V", Type.getDescriptor(void.class));
    assertEquals("Z", Type.getDescriptor(boolean.class));
    assertEquals("C", Type.getDescriptor(char.class));
    assertEquals("B", Type.getDescriptor(byte.class));
    assertEquals("S", Type.getDescriptor(short.class));
    assertEquals("I", Type.getDescriptor(int.class));
    assertEquals("F", Type.getDescriptor(float.class));
    assertEquals("J", Type.getDescriptor(long.class));
    assertEquals("D", Type.getDescriptor(double.class));
    assertEquals("Ljava/lang/Object;", Type.getDescriptor(Object.class));
    assertEquals("[Ljava/lang/Object;", Type.getDescriptor(Object[].class));
    assertEquals("[[Ljava/lang/Object;", Type.getDescriptor(Object[][].class));
    assertEquals("[[I", Type.getDescriptor(int[][].class));
  }

  @Test
  public void testGetOpcode() {
    assertEquals(BALOAD, Type.BOOLEAN_TYPE.getOpcode(IALOAD));
    assertEquals(BALOAD, Type.BYTE_TYPE.getOpcode(IALOAD));
    assertEquals(CALOAD, Type.CHAR_TYPE.getOpcode(IALOAD));
    assertEquals(SALOAD, Type.SHORT_TYPE.getOpcode(IALOAD));
    assertEquals(IALOAD, Type.INT_TYPE.getOpcode(IALOAD));
    assertEquals(FALOAD, Type.FLOAT_TYPE.getOpcode(IALOAD));
    assertEquals(LALOAD, Type.LONG_TYPE.getOpcode(IALOAD));
    assertEquals(DALOAD, Type.DOUBLE_TYPE.getOpcode(IALOAD));
    assertEquals(AALOAD, Type.getType("Ljava/lang/Object;").getOpcode(IALOAD));
    assertEquals(AALOAD, Type.getObjectType("java/lang/Object").getOpcode(IALOAD));
    assertEquals(AASTORE, Type.getType("Ljava/lang/Object;").getOpcode(IASTORE));
    assertEquals(AASTORE, Type.getObjectType("java/lang/Object").getOpcode(IASTORE));
    assertEquals(AASTORE, Type.getType("[I").getOpcode(IASTORE));
    assertEquals(RETURN, Type.VOID_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.BOOLEAN_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.BYTE_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.CHAR_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.SHORT_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.INT_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(FRETURN, Type.FLOAT_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(LRETURN, Type.LONG_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(DRETURN, Type.DOUBLE_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.getType("Ljava/lang/Object;").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.getObjectType("java/lang/Object").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.getType("Ljava/lang/Object;").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.getObjectType("java/lang/Object").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.getType("[I").getOpcode(Opcodes.IRETURN));
    assertEquals(IADD, Type.BOOLEAN_TYPE.getOpcode(IADD));
    assertEquals(IADD, Type.BYTE_TYPE.getOpcode(IADD));
    assertEquals(IADD, Type.CHAR_TYPE.getOpcode(IADD));
    assertEquals(IADD, Type.SHORT_TYPE.getOpcode(IADD));
    assertEquals(IADD, Type.INT_TYPE.getOpcode(IADD));
    assertEquals(FADD, Type.FLOAT_TYPE.getOpcode(IADD));
    assertEquals(LADD, Type.LONG_TYPE.getOpcode(IADD));
    assertEquals(DADD, Type.DOUBLE_TYPE.getOpcode(IADD));
    Class<UnsupportedOperationException> expectedException = UnsupportedOperationException.class;
    assertThrows(expectedException, () -> Type.VOID_TYPE.getOpcode(IADD));
    assertThrows(expectedException, () -> Type.VOID_TYPE.getOpcode(ILOAD));
    assertThrows(expectedException, () -> Type.VOID_TYPE.getOpcode(IALOAD));
    assertThrows(expectedException, () -> Type.getType("LI;").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.getType("[I").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.getObjectType("I").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.getMethodType("()V").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.getMethodType("()V").getOpcode(IALOAD));
  }

  @Test
  public void testEquals() {
    Type nullType = null;

    final boolean equalsNull = Type.getObjectType("I").equals(nullType);
    final boolean equalsDifferentTypeSort = Type.getObjectType("I").equals(Type.INT_TYPE);
    final boolean equalsDifferentObjectType =
        Type.getObjectType("I").equals(Type.getObjectType("HI"));
    final boolean equalsOtherDifferentObjectType =
        Type.getObjectType("I").equals(Type.getObjectType("J"));
    final boolean equalsSameType = Type.getObjectType("I").equals(Type.getType("LI;"));
    final boolean equalsSameObjectType = Type.getType("LI;").equals(Type.getObjectType("I"));
    final boolean equalsSamePrimitiveType = Type.INT_TYPE.equals(Type.getType("I"));

    assertFalse(equalsNull);
    assertFalse(equalsDifferentTypeSort);
    assertFalse(equalsDifferentObjectType);
    assertFalse(equalsOtherDifferentObjectType);
    assertTrue(equalsSameType);
    assertTrue(equalsSameObjectType);
    assertTrue(equalsSamePrimitiveType);
  }

  @Test
  public void testHashcode() {
    assertNotEquals(0, Type.getType("Ljava/lang/Object;").hashCode());
    assertEquals(
        Type.getType("Ljava/lang/Object;").hashCode(),
        Type.getObjectType("java/lang/Object").hashCode());
    assertNotEquals(Type.INT_TYPE.hashCode(), Type.getObjectType("I").hashCode());
  }
}
