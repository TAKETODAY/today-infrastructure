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
package cn.taketoday.bytecode.commons;

import org.junit.jupiter.api.Test;

import cn.taketoday.bytecode.Type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link MethodSignature}.
 *
 * @author Eric Bruneton
 */
public class MethodSignatureTest {

  @Test
  public void testConstructor_fromDescriptor() {
    MethodSignature method = new MethodSignature("name", "(I)J");

    assertEquals("name", method.getName());
    assertEquals("(I)J", method.getDescriptor());
    assertEquals(Type.LONG_TYPE, method.getReturnType());
    assertArrayEquals(new Type[] { Type.INT_TYPE }, method.getArgumentTypes());
    assertEquals("name(I)J", method.toString());
  }

  @Test
  public void testConstructor_fromTypes() {
    MethodSignature method = new MethodSignature(Type.LONG_TYPE, "name", Type.INT_TYPE);

    assertEquals("name", method.getName());
    assertEquals("(I)J", method.getDescriptor());
    assertEquals(Type.LONG_TYPE, method.getReturnType());
    assertArrayEquals(new Type[] { Type.INT_TYPE }, method.getArgumentTypes());
    assertEquals("name(I)J", method.toString());
  }

  @Test
  public void testGetMethod_fromMethodObject() throws ReflectiveOperationException {
    MethodSignature method = MethodSignature.from(Object.class.getMethod("equals", Object.class));

    assertEquals("equals", method.getName());
    assertEquals("(Ljava/lang/Object;)Z", method.getDescriptor());
  }

  @Test
  public void testGetMethod_fromConstructorObject() throws ReflectiveOperationException {
    MethodSignature method = MethodSignature.from(Object.class.getConstructor());

    assertEquals("<init>", method.getName());
    assertEquals("()V", method.getDescriptor());
  }

  @Test
  public void testGetMethod_fromDescriptor() {
    MethodSignature method =
            MethodSignature.from(
                    "boolean name(byte, char, short, int, float, long, double, pkg.Class, pkg.Class[])");

    assertEquals("name", method.getName());
    assertEquals("(BCSIFJDLpkg/Class;[Lpkg/Class;)Z", method.getDescriptor());
  }

  @Test
  public void testGetMethod_fromInvalidDescriptor() {
    assertThrows(IllegalArgumentException.class, () -> MethodSignature.from("name()"));
    assertThrows(IllegalArgumentException.class, () -> MethodSignature.from("void name"));
    assertThrows(IllegalArgumentException.class, () -> MethodSignature.from("void name(]"));
  }

  @Test
  public void testGetMethod_withDefaultPackage() {
    MethodSignature withoutDefaultPackage =
            MethodSignature.from("void name(Object)", /* defaultPackage= */ false);
    MethodSignature withDefaultPackage = MethodSignature.from("void name(Object)", /* defaultPackage= */ true);

    assertEquals("(Ljava/lang/Object;)V", withoutDefaultPackage.getDescriptor());
    assertEquals("(LObject;)V", withDefaultPackage.getDescriptor());
  }

  @Test
  public void testEquals() {
    MethodSignature nullMethod = null;

    boolean equalsNull = new MethodSignature("name", "()V").equals(nullMethod);
    boolean equalsMethodWithDifferentName =
            new MethodSignature("name", "()V").equals(new MethodSignature("other", "()V"));
    boolean equalsMethodWithDifferentDescriptor =
            new MethodSignature("name", "()V").equals(new MethodSignature("name", "(I)J"));
    boolean equalsSame = new MethodSignature("name", "()V").equals(MethodSignature.from("void name()"));

    assertFalse(equalsNull);
    assertFalse(equalsMethodWithDifferentName);
    assertFalse(equalsMethodWithDifferentDescriptor);
    assertTrue(equalsSame);
  }

  @Test
  public void testHashCode() {
    assertNotEquals(0, new MethodSignature("name", "()V").hashCode());
  }

  @Test
  public void forConstructor() {
    MethodSignature cstruct_object = MethodSignature.forConstructor("Object");

    MethodSignature withoutDefaultPackage =
            MethodSignature.from("void <init>(Object)", /* defaultPackage= */ false);

    assertEquals("(Ljava/lang/Object;)V", withoutDefaultPackage.getDescriptor());

    assertThat(cstruct_object.getDescriptor())
            .isEqualTo(withoutDefaultPackage.getDescriptor());

    // <init>(Object,Object,Class)
    MethodSignature cstruct_objects = MethodSignature.forConstructor("Object, Object ,Class ");
    MethodSignature signature =
            MethodSignature.from("void <init>(Object,Object,Class)");

    assertThat(cstruct_objects).isEqualTo(signature);
  }

}
