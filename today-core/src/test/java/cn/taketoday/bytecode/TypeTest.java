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
package cn.taketoday.bytecode;

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
    Type type = Type.fromDescriptor(descriptor);

    assertEquals(descriptor, type.getDescriptor());
    assertEquals(descriptor, type.toString());
  }

  @Test
  public void testGetTypeFromDescriptor_invalid() {
    Executable getType = () -> Type.fromDescriptor("-");

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, getType);
    assertEquals("Invalid descriptor: -", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = { "I", "V", "java/lang/Object", "[I", "[LI;", "[[Ljava/lang/Object;" })
  public void testGetObjectType(final String internalName) {
    Type type = Type.fromInternalName(internalName);

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
    Type type = Type.fromMethod(methodDescriptor);

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
    Type returnType = Type.forReturnType(methodDescriptor);
    Type methodType = Type.fromMethod(returnType, argumentTypes);

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
    assertEquals(Type.INT_TYPE, Type.forReturnType("()I"));
    assertEquals(Type.INT_TYPE, Type.forReturnType("(Lpkg/classMethod();)I"));
  }

  @Test
  public void testGetTypeFromClass() {
    assertEquals(Type.VOID_TYPE, Type.fromClass(void.class));
    assertEquals(Type.BOOLEAN_TYPE, Type.fromClass(boolean.class));
    assertEquals(Type.CHAR_TYPE, Type.fromClass(char.class));
    assertEquals(Type.BYTE_TYPE, Type.fromClass(byte.class));
    assertEquals(Type.SHORT_TYPE, Type.fromClass(short.class));
    assertEquals(Type.INT_TYPE, Type.fromClass(int.class));
    assertEquals(Type.FLOAT_TYPE, Type.fromClass(float.class));
    assertEquals(Type.LONG_TYPE, Type.fromClass(long.class));
    assertEquals(Type.DOUBLE_TYPE, Type.fromClass(double.class));
    assertEquals("Ljava/lang/Object;", Type.fromClass(Object.class).getDescriptor());
    assertEquals("[Ljava/lang/Object;", Type.fromClass(Object[].class).getDescriptor());
  }

  @Test
  public void testGetTypeFromConstructor() throws NoSuchMethodException, SecurityException {
    Type type = Type.fromConstructor(ClassReader.class.getConstructor(byte[].class, int.class));

    assertEquals("([BII)V", type.getDescriptor());
  }

  @Test
  public void testGetTypeFromMethod() throws NoSuchMethodException, SecurityException {
    Type type = Type.fromMethod(Arrays.class.getMethod("binarySearch", byte[].class, byte.class));

    assertEquals("([BB)I", type.getDescriptor());
  }

  @Test
  public void testGetArgumentTypesFromMethod() throws NoSuchMethodException, SecurityException {
    Type[] argumentTypes =
            Type.getArgumentTypes(Arrays.class.getMethod("binarySearch", byte[].class, byte.class));

    assertArrayEquals(new Type[] { Type.fromClass(byte[].class), Type.BYTE_TYPE }, argumentTypes);
  }

  @Test
  public void testGetReturnTypeFromMethod() throws NoSuchMethodException, SecurityException {
    Type returnType =
            Type.forReturnType(Arrays.class.getMethod("binarySearch", byte[].class, byte.class));

    assertEquals(Type.INT_TYPE, returnType);
  }

  @Test
  void testGetArgumentCountFromType() {
    assertEquals(
            14,
            Type.fromMethod("(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V")
                    .getArgumentCount());
  }

  @Test
  void testGetArgumentCountFromDescriptor() {
    assertEquals(
            14, Type.getArgumentCount("(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V"));
    assertEquals(0, Type.getArgumentCount("()I"));
  }

  @Test
  void testGetArgumentsAndReturnSizeFromType() {
    assertEquals(
            17 << 2,
            Type.fromMethod("(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V")
                    .getArgumentsAndReturnSizes());
    assertEquals(1 << 2 | 1, Type.fromMethod("()I").getArgumentsAndReturnSizes());
    assertEquals(1 << 2 | 1, Type.fromMethod("()F").getArgumentsAndReturnSizes());
    assertEquals(1 << 2 | 2, Type.fromMethod("()J").getArgumentsAndReturnSizes());
    assertEquals(1 << 2 | 2, Type.fromMethod("()D").getArgumentsAndReturnSizes());
    assertEquals(1 << 2 | 1, Type.fromMethod("()LD;").getArgumentsAndReturnSizes());
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
    assertEquals(Type.ARRAY, Type.fromDescriptor("[LI;").getSort());
    assertEquals(Type.ARRAY, Type.fromInternalName("[LI;").getSort());
    assertEquals(Type.OBJECT, Type.fromDescriptor("LI;").getSort());
    assertEquals(Type.OBJECT, Type.fromInternalName("I").getSort());
  }

  @Test
  public void testGetDimensions() {
    assertEquals(1, Type.fromDescriptor("[I").getDimensions());
    assertEquals(3, Type.fromDescriptor("[[[LI;").getDimensions());
  }

  @Test
  public void testGetElementType() {
    assertEquals(Type.INT_TYPE, Type.fromDescriptor("[I").getElementType());
    assertEquals(Type.fromInternalName("I"), Type.fromDescriptor("[[[LI;").getElementType());
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
    assertEquals("I[]", Type.fromInternalName("[LI;").getClassName());
    assertEquals("java.lang.Object", Type.fromInternalName("java/lang/Object").getClassName());
    assertEquals("java.lang.Object", Type.fromDescriptor("Ljava/lang/Object;").getClassName());
  }

  @Test
  public void testGetInternalName() {
    assertEquals("[LI;", Type.fromInternalName("[LI;").getInternalName());
    assertEquals("java/lang/Object", Type.fromInternalName("java/lang/Object").getInternalName());
    assertEquals("java/lang/Object", Type.fromDescriptor("Ljava/lang/Object;").getInternalName());
  }

  @Test
  public void testGetArgumentsAndReturnSize() {
    Type type = Type.fromDescriptor("(IZBCSDFJLI;LV;Ljava/lang/Object;[I[LI;[[Ljava/lang/Object;)V");

    assertEquals(17 << 2, type.getArgumentsAndReturnSizes());
  }

  @Test
  public void testGetDescriptor() {
    assertEquals("[LI;", Type.fromInternalName("[LI;").getDescriptor());
    assertEquals("Ljava/lang/Object;", Type.fromInternalName("java/lang/Object").getDescriptor());
    assertEquals("Ljava/lang/Object;", Type.fromDescriptor("Ljava/lang/Object;").getDescriptor());
  }

  @Test
  public void testGetMethodDescriptor() {
    assertEquals("(IJ)V", Type.getMethodDescriptor(Type.VOID_TYPE, Type.INT_TYPE, Type.LONG_TYPE));
    assertEquals(
            "(Ljava/lang/Object;)V",
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.fromDescriptor("Ljava/lang/Object;")));
    assertEquals(
            "(Ljava/lang/Object;)V",
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.fromInternalName("java/lang/Object")));
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
    assertEquals(AALOAD, Type.fromDescriptor("Ljava/lang/Object;").getOpcode(IALOAD));
    assertEquals(AALOAD, Type.fromInternalName("java/lang/Object").getOpcode(IALOAD));
    assertEquals(AASTORE, Type.fromDescriptor("Ljava/lang/Object;").getOpcode(IASTORE));
    assertEquals(AASTORE, Type.fromInternalName("java/lang/Object").getOpcode(IASTORE));
    assertEquals(AASTORE, Type.fromDescriptor("[I").getOpcode(IASTORE));
    assertEquals(RETURN, Type.VOID_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.BOOLEAN_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.BYTE_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.CHAR_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.SHORT_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(IRETURN, Type.INT_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(FRETURN, Type.FLOAT_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(LRETURN, Type.LONG_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(DRETURN, Type.DOUBLE_TYPE.getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.fromDescriptor("Ljava/lang/Object;").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.fromInternalName("java/lang/Object").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.fromDescriptor("Ljava/lang/Object;").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.fromInternalName("java/lang/Object").getOpcode(Opcodes.IRETURN));
    assertEquals(ARETURN, Type.fromDescriptor("[I").getOpcode(Opcodes.IRETURN));
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
    assertThrows(expectedException, () -> Type.fromDescriptor("LI;").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.fromDescriptor("[I").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.fromInternalName("I").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.fromMethod("()V").getOpcode(IADD));
    assertThrows(expectedException, () -> Type.fromMethod("()V").getOpcode(IALOAD));
  }

  @Test
  public void testEquals() {
    Type nullType = null;

    final boolean equalsNull = Type.fromInternalName("I").equals(nullType);
    final boolean equalsDifferentTypeSort = Type.fromInternalName("I").equals(Type.INT_TYPE);
    final boolean equalsDifferentObjectType =
            Type.fromInternalName("I").equals(Type.fromInternalName("HI"));
    final boolean equalsOtherDifferentObjectType =
            Type.fromInternalName("I").equals(Type.fromInternalName("J"));
    final boolean equalsSameType = Type.fromInternalName("I").equals(Type.fromDescriptor("LI;"));
    final boolean equalsSameObjectType = Type.fromDescriptor("LI;").equals(Type.fromInternalName("I"));
    final boolean equalsSamePrimitiveType = Type.INT_TYPE.equals(Type.fromDescriptor("I"));

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
    assertNotEquals(0, Type.fromDescriptor("Ljava/lang/Object;").hashCode());
    assertEquals(
            Type.fromDescriptor("Ljava/lang/Object;").hashCode(),
            Type.fromInternalName("java/lang/Object").hashCode());
    assertNotEquals(Type.INT_TYPE.hashCode(), Type.fromInternalName("I").hashCode());
  }
}
